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

    public void StoreInternally(MiniGitObject obj) {
        objects.put(obj.miniGitSha1(), obj);
    }

    MiniGitObject LoadFromInternal(String hash) {
        return objects.get(hash);
    }

    public void save(Path saveAt) throws IOException {
        Path objectsPath = saveAt.resolve("objects");
        if (!Files.exists(objectsPath)) {
            Files.createDirectory(objectsPath);
        }
        for (Map.Entry<String, MiniGitObject> obj : objects.entrySet()) {
            Path objPath = objectsPath.resolve(Path.of(obj.getKey().substring(0, 2), obj.getKey().substring(2)));
            obj.getValue().write(objPath);
        }
    }

    public static Repository load(Path loadFrom) throws IOException {
        if (!Files.exists(loadFrom)) {
            throw new IOException("Repository does not exist.");
        }
        return new Repository();
    }
}
