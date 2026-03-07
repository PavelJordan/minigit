package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Path;

public interface MinigitObjectLoader {
    MiniGitObject loadFromInternal(String hash) throws IOException;
    Path getRootPath();
    String getHashFromRef(String ref);
}
