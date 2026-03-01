package cz.cuni.mff.jordanpa.minigit.misc;

import java.nio.file.Path;
import java.util.List;

public final class FileHelper {
    public static List<Path> getFilesFromWildcard(String wildcard) {
        IO.println("Wildcards not implemented yet");
        return List.of(Path.of(wildcard));
    }
    public static List<Path> getFilesExceptWildcards(List<String> wildcards) {
        throw new RuntimeException("Not implemented yet");
    }
}
