package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;

public sealed interface MinigitObjectLoader permits Repository {
    MiniGitObject loadFromInternal(String hash) throws IOException;
}
