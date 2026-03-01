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
    private final Path headPath;
    private Head head;

    public Head getHead() throws IOException {
        ensureHeadLoaded();
        return head;
    }

    /**
     * Map path -> hash.
     */
    private HashMap<Path, String> stagedIndex;

    public Repository(Path path) {
        this.repoPath = path;
        this.objectsPath = path.resolve("objects");
        this.indexPath = path.resolve("index");
        this.headPath = path.resolve("HEAD");
    }

    public Map<String, String> getReferences() {
        return Map.copyOf(references);
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
        saveHead();
    }

    public void addToIndex(Path... files) throws IOException {
        ensureIndexLoaded();
        for (Path file : files) {
            try {
                if (!Files.exists(file)) {
                    stagedIndex.remove(file);
                    continue;
                }
                Blob blob = new Blob(file);
                stagedIndex.put(file, blob.miniGitSha1());
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
        if (stagedIndex != null) {
            return;
        }
        stagedIndex = new HashMap<>();
        try(Scanner scanner = new Scanner(indexPath)) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                int delimiterIndex = nextLine.indexOf(' ');
                String hash = nextLine.substring(0, delimiterIndex);
                Path path = Path.of(nextLine.substring(delimiterIndex + 1)).normalize();
                stagedIndex.put(path, hash);
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
        if (stagedIndex == null) {
            return;
        }
        try (var out = Files.newBufferedWriter(indexPath)) {
            for (Map.Entry<Path, String> entry : stagedIndex.entrySet()) {
                out.write(entry.getValue() + " " + entry.getKey().normalize() + "\n");
            }
        }
    }

    private void saveHead() throws IOException {
        if (!Files.exists(headPath)) {
            Files.createDirectories(headPath.getParent());
            Files.createFile(headPath);
            new Head(Head.Type.UNSET, null).write(headPath);
        }
        if (head != null) {
            head.write(headPath);
        }
    }

    private void ensureHeadLoaded() throws IOException {
        if (head == null) {
            head = Head.loadHead(headPath);
        }
    }

    public Map<Path, String> getTrackedFiles() throws IOException {
        ensureIndexLoaded();
        return Map.copyOf(stagedIndex);
    }

    public void checkoutTree(Tree treeToRestore) throws IOException {
        List<FileStatus> statusesStaged = getWorkingToIndexStatus();
        List<FileStatus> statusesCommited = getStagedToLastCommitStatus();
        if (statusesStaged.stream().anyMatch(status -> status.status() != FileStatusType.TRACKED && status.status() != FileStatusType.UNTRACKED)) {
            throw new IOException("Cannot restore tree. There are unstaged uncommitted changes.");
        }
        if (statusesCommited.stream().anyMatch(status -> status.status() != FileStatusType.TRACKED && status.status() != FileStatusType.UNTRACKED)) {
            throw new IOException("Cannot restore tree. There are staged uncommitted changes.");
        }
        Tree CurrentRoot = Tree.buildTree(stagedIndex).getLast();
        if (loadFromInternal(CurrentRoot.miniGitSha1()) == null) {
            throw new IOException("Cannot restore tree. Current root tree is not in repository. Use tree command to build it.");
        }
        for (FileStatus status : statusesStaged.stream().filter(s -> s.status() == FileStatusType.TRACKED).toList()) {
            if (status.status() == FileStatusType.TRACKED) {
                IO.println("Deleting tracked file " + status.path());
                Files.delete(status.path());
            }
        }
        stagedIndex = getIndexOfTree(treeToRestore);
        IO.println("Restoring tree...");
        internalForcedCheckoutTree();
    }

    private void internalForcedCheckoutTree() {
        for (HashMap.Entry<Path, String> entry : stagedIndex.entrySet()) {
            MiniGitObject possibleBlobToRestore = loadFromInternal(entry.getValue());
            if (possibleBlobToRestore instanceof Blob blobToRestore) {
                if (Files.exists(entry.getKey())) {
                    IO.println("File " + entry.getKey() + " already exists. Cannot overwrite!");
                    return;
                }
                IO.println("Restoring file " + entry.getKey() + "...");
                try {
                    blobToRestore.writeContentsTo(entry.getKey());
                } catch (IOException e) {
                    IO.println(e);
                }
            }
            else {
                IO.println("Object with specified hash is not a blob, even though it should. Repository is corrupted.");
            }
        }
    }

    public List<FileStatus> getStagedToLastCommitStatus() throws IOException {
        ensureHeadLoaded();
        ensureIndexLoaded();
        HashMap<Path, String> commitIndex = getCommitIndexFromHead();
        return getFileStatusesFromComparison(stagedIndex, commitIndex);
    }

    private HashMap<Path, String> getCommitIndexFromHead() throws IOException {
        if (head.type() == Head.Type.COMMIT) {
            MiniGitObject maybeCommit = loadFromInternal(head.hash());
            if (maybeCommit instanceof Commit commit) {
                MiniGitObject maybeTree = loadFromInternal(commit.getTreeHash());
                if (maybeTree instanceof Tree tree) {
                    return getIndexOfTree(tree);
                }
                else {
                    throw new IOException("Cannot restore tree. Tree is not in repository.");
                }
            }
            else {
                throw new IOException("Cannot restore head.");
            }
        }
        else if (head.type() == Head.Type.BRANCH) {
            throw new IOException("Cannot restore branch. Not implemented yet");
        }
        else {
            IO.println("No commit yet");
            return new HashMap<>();
        }
    }

    public List<FileStatus> getWorkingToIndexStatus() throws IOException {
        ensureIndexLoaded();
        HashMap<Path, String> workingDirIndex = getIndexOfWorkingDirectory();
        return getFileStatusesFromComparison(workingDirIndex, stagedIndex);
    }

    private List<FileStatus> getFileStatusesFromComparison(HashMap<Path, String> currentIndex, HashMap<Path, String> indexToCompare) {
        List<FileStatus> statuses = new ArrayList<>();
        for (Path path : currentIndex.keySet()) {
            String workingDirectoryHash = currentIndex.get(path);
            FileStatusType againstCommit;
            if (indexToCompare.containsKey(path)) {
                if (indexToCompare.get(path).equals(workingDirectoryHash)) {
                    againstCommit = FileStatusType.TRACKED;
                }
                else {
                    againstCommit = FileStatusType.MODIFIED;
                }
            }
            else {
                againstCommit = FileStatusType.UNTRACKED;
            }
            statuses.add(new FileStatus(path, againstCommit));
        }
        var deletedAgainstStaged = indexToCompare.keySet().stream().filter(path -> !currentIndex.containsKey(path));
        deletedAgainstStaged.forEach(path -> statuses.add(new FileStatus(path, FileStatusType.DELETED)));
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

    public void addCommitAsNewHeadAndStoreInternally(Commit commit) {
        head = new Head(Head.Type.COMMIT, commit.miniGitSha1());
        storeInternally(commit);
    }

    HashMap<Path, String> getIndexOfTree(Tree tree) {
        return getIndexOfTreeInternal(tree, Path.of("./"));
    }

    private HashMap<Path, String> getIndexOfTreeInternal(Tree tree, Path pathSoFar) {
        HashMap<Path, String> treeIndex = new HashMap<>();
        tree.getContents().forEach((name, entry) -> {
            if (entry.type() == Tree.TreeEntryType.TREE) {
                MiniGitObject possiblySubtreeToRestore = loadFromInternal(entry.hash());
                if (possiblySubtreeToRestore instanceof Tree subtreeToRestore) {
                    treeIndex.putAll(getIndexOfTreeInternal(subtreeToRestore, pathSoFar.resolve(name)));
                }
                else {
                    IO.println("Object with specified hash is not a tree, even though it should. Repository is corrupted.");
                }
            }
            else if (entry.type() == Tree.TreeEntryType.BLOB) {
                treeIndex.put(pathSoFar.resolve(name).normalize(), entry.hash());

            }
        });
        return treeIndex;
    }

    public HashMap<Path, String> getIndexOfWorkingDirectory() throws IOException {
        List<Path> paths = getPaths(Path.of("./"));
        HashMap<Path, String> workingDirIndex = new HashMap<>();
        for(Path path : paths) {
            String hash = new Blob(path).miniGitSha1();
            workingDirIndex.put(path, hash);
        }
        return workingDirIndex;
    }
}
