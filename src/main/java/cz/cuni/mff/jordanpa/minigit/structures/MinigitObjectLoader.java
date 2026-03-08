package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Path;

public interface MinigitObjectLoader {
    /**
     * Loads the object from the internal database with the specified hash. The implementation depends on a concrete class.
     * @return Object with the correct concrete type, or null if it is not found.
     * @throws IOException If the object is corrupted.
     */
    MiniGitObject loadFromInternal(String hash) throws IOException;

    /**
     * Returns the root path of the repository.
     */
    Path getRootPath();

    /**
     * Converts a reference (branch name, tag name) to a hash.
     * @param ref the reference to convert (branch name, tag name)
     * @return the hash, or null if the reference does not exist.
     */
    String getHashFromRef(String ref);
}
