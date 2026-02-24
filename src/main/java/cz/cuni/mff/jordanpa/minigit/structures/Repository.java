package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Repository {

    /**
     * Map hash -> actual object.
     */
    private final HashMap<String, MiniGitObject> objects = new HashMap<>();

    /**
     * Map human readable name -> hash.
     */
    private final HashMap<String, String> references = new HashMap<>();

    private Commit head;
    private final Path repoPath;
    private final Path objectsPath;

    public Repository(Path path) {
        this.repoPath = path;
        this.objectsPath = path.resolve("objects");
    }

    public static Repository load(Path loadFrom) throws IOException {
        if (!Files.exists(loadFrom)) {
            throw new IOException("Repository does not exist.");
        }
        return new Repository(loadFrom);
    }


    public void storeInternally(MiniGitObject obj) {
        objects.put(obj.miniGitSha1(), obj);
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
    }
}
