package cz.cuni.mff.jordanpa.minigit.structures;

import java.util.HashMap;

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
        objects.put(obj.sha1(), obj);
    }

    public MiniGitObject LoadFromInternal(String hash) {
        return objects.get(hash);
    }
}
