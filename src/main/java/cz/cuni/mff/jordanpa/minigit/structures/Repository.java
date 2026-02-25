package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Repository {

    public enum FileStatusType {
        TRACKED,
        MODIFIED,
        DELETED,
        UNTRACKED
    }

    public record FileStatus(Path path, FileStatusType status) {}

    /**
     * Map hash -> actual object. Only the objects that are currently in runtime memory.
     */
    private final HashMap<String, MiniGitObject> objects = new HashMap<>();

    /**
     * Map human-readable name -> hash. Only the references that are currently in runtime memory.
     */
    private final HashMap<String, String> references = new HashMap<>();
    private final Path repoPath;
    private final Path objectsPath;
    private final Path indexPath;

    /**
     * Map path -> hash.
     */
    private HashMap<Path, String> index;

    public Repository(Path path) {
        this.repoPath = path;
        this.objectsPath = path.resolve("objects");
        this.indexPath = path.resolve("index");
    }

    public static Repository load(Path loadFrom) throws IOException {
        if (!Files.exists(loadFrom)) {
            throw new IOException("Repository does not exist.");
        }
        return new Repository(loadFrom);
    }


    public void storeInternally(MiniGitObject obj) {
        if (!objects.containsKey(obj.miniGitSha1())) {
            objects.put(obj.miniGitSha1(), obj);
        }
    }

    public MiniGitObject loadFromInternal(String hash) {
        if (!objects.containsKey(hash)) {
            try {
                MiniGitObject obj = MiniGitObject.getObjectBasedOnHash(objectsPath, hash);
                objects.put(hash, obj);
            }
            catch(IOException e) {
                IO.println(e);
                IO.println("Error loading object from repository. Check your permissions and hash and try again.");
                return null;
            }
        }
        return objects.get(hash);
    }

    public void save() throws IOException {
        if (!Files.exists(objectsPath)) {
            Files.createDirectory(objectsPath);
        }
        for (Map.Entry<String, MiniGitObject> obj : objects.entrySet()) {
            MiniGitObject.saveObjectBasedOnHash(objectsPath, obj.getKey(), obj.getValue());
        }
        saveIndex();
    }

    public void addToIndex(Path... files) throws IOException {
        ensureIndexLoaded();
        for (Path file : files) {
            try {
                if (!Files.exists(file)) {
                    index.remove(file);
                    continue;
                }
                Blob blob = new Blob(file);
                index.put(file, blob.miniGitSha1());
                storeInternally(blob);
            }
            catch(IOException e) {
                IO.println(e);
                IO.println("Error adding file " + file.toString() + " to index. Continuing...");
            }
        }
    }

    /**
     * Loads the index from the disk (path -> blob hash).
     */
    private void ensureIndexLoaded() throws IOException {
        if (index != null) {
            return;
        }
        index = new HashMap<>();
        try(Scanner scanner = new Scanner(indexPath)) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                int delimiterIndex = nextLine.indexOf(' ');
                String hash = nextLine.substring(0, delimiterIndex);
                Path path = Path.of(nextLine.substring(delimiterIndex + 1)).normalize();
                index.put(path, hash);
            }
        }
    }

    /**
     * Saves the index to the disk (path -> blob hash).
     */
    private void saveIndex() throws IOException {
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath.getParent());
            Files.createFile(indexPath);
        }
        if (index == null) {
            return;
        }
        try (var out = Files.newBufferedWriter(indexPath)) {
            for (Map.Entry<Path, String> entry : index.entrySet()) {
                out.write(entry.getValue() + " " + entry.getKey().normalize() + "\n");
            }
        }
    }

    public Map<Path, String> getTrackedFiles() throws IOException {
        ensureIndexLoaded();
        return Map.copyOf(index);
    }

    public void restoreTree(Tree treeToRestore) throws IOException {
        List<FileStatus> statuses = getStatus();
        if (statuses.stream().anyMatch(status -> status.status() == FileStatusType.DELETED || status.status() == FileStatusType.MODIFIED)) {
            throw new IOException("Cannot restore tree. There are uncommitted changes.");
        }
        Tree CurrentRoot = Tree.buildTree(index).getLast();
        if (loadFromInternal(CurrentRoot.miniGitSha1()) == null) {
            throw new IOException("Cannot restore tree. Current root tree is not in repository. Use tree command to build it.");
        }
        for (FileStatus status : statuses) {
            if (status.status() == FileStatusType.TRACKED) {
                IO.println("Deleting tracked file " + status.path());
                Files.delete(status.path());
            }
        }
        index = new HashMap<>();
        IO.println("Restoring tree...");
        restoreTreeInternal(treeToRestore, Path.of("./"));
    }

    private void restoreTreeInternal(Tree treeToRestore, Path path) {
        treeToRestore.getContents().forEach((name, entry) -> {
           if (entry.type() == Tree.TreeEntryType.TREE) {
               MiniGitObject possiblySubtreeToRestore = loadFromInternal(entry.hash());
               if (possiblySubtreeToRestore instanceof Tree subtreeToRestore) {
                   IO.println("Restoring subtree " + path.resolve(name));
                   restoreTreeInternal(subtreeToRestore, path.resolve(name));
               }
               else {
                   IO.println("Object with specified hash is not a tree, even though it should. Repository is corrupted.");
               }
           }
           else if (entry.type() == Tree.TreeEntryType.BLOB) {
               MiniGitObject possibleBlobToRestore = loadFromInternal(entry.hash());
               if (possibleBlobToRestore instanceof Blob blobToRestore) {
                   index.put(path.resolve(name), blobToRestore.miniGitSha1());
                   if (Files.exists(path.resolve(name))) {
                       IO.println("File " + path.resolve(name) + " already exists. Cannot overwrite!");
                       return;
                   }
                   IO.println("Restoring file " + path.resolve(name));
                   try {
                       blobToRestore.writeContentsTo(path.resolve(name));
                   } catch (IOException e) {
                       IO.println(e);
                   }
               }
               else {
                   IO.println("Object with specified hash is not a blob, even though it should. Repository is corrupted.");
               }
           }
        });
    }

    public List<FileStatus> getStatus() throws IOException {
        ensureIndexLoaded();
        List<Path> existingPaths = getPaths(Path.of("./"));
        List<FileStatus> statuses = new ArrayList<>();
        for (Path path : existingPaths) {
            if (!index.containsKey(path)) {
                statuses.add(new FileStatus(path, FileStatusType.UNTRACKED));
            }
            else {
                Blob newBlob = new Blob(path);
                if (!index.get(path).equals(newBlob.miniGitSha1())) {
                    statuses.add(new FileStatus(path, FileStatusType.MODIFIED));
                }
                else {
                    statuses.add(new FileStatus(path, FileStatusType.TRACKED));
                }
            }
        }
        var deletedPaths = index.keySet().stream().filter(path -> !existingPaths.contains(path));
        deletedPaths.forEach(path -> statuses.add(new FileStatus(path, FileStatusType.DELETED)));
        return statuses;
    }

    private List<Path> getPaths(Path path) {
        ArrayList<Path> paths = new ArrayList<>();
        try(var dirStream = Files.newDirectoryStream(path)) {
            for (Path entry : dirStream) {
                if (entry.getFileName().toString().equals(".minigit")) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    paths.addAll(getPaths(entry));
                }
                else {
                    paths.add(entry.normalize());
                }
            }
        }
        catch(IOException e) {
            IO.println(e);
        }
        return paths;
    }
}
