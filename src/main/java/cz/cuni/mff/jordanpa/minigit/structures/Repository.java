package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Repository {

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

    public void removeFromIndex(Path... files) throws IOException {
        ensureIndexLoaded();
        throw new UnsupportedOperationException("Not implemented yet.");
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

    public Set<Path> getTrackedFiles() throws IOException {
        ensureIndexLoaded();
        return Set.copyOf(index.keySet());
    }

    public String trackedFileHash(Path path) throws IOException {
        ensureIndexLoaded();
        return index.get(path);
    }
}
