package cz.cuni.mff.jordanpa.minigit.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FileHelper {
    /**
     * If the path is dir -> all files in dir
     * If the path is file -> only that file
     * excludes anything in toExclude - either starts with or ends with.
     */
    public static List<Path> getAllFiles(Path path, List<String> toExclude) {
        path = path.normalize();
        ArrayList<Path> paths = new ArrayList<>();
        Path CWD = Path.of("./");
        if (!Files.isDirectory(path)) {
            if (isExcluded(path, toExclude)) {
                return List.of();
            }
            return List.of(getRelativePathToDirectory(path, CWD));
        }
        try(var dirStream = Files.newDirectoryStream(path)) {
            for (Path entry : dirStream) {
                if (isExcluded(entry, toExclude)) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    paths.addAll(getAllFiles(entry, toExclude));
                }
                else {
                    paths.add(getRelativePathToDirectory(entry, CWD));
                }
            }
        }
        catch(IOException e) {
            IO.println(e);
        }
        return paths;
    }

    public static Path getRelativePathToDirectory(Path entry, Path directory) {
        return directory.toAbsolutePath().relativize(entry.toAbsolutePath()).normalize();
    }

    public static boolean isExcluded(Path path, List<String> toExclude) {
        return toExclude.stream().anyMatch(ex -> path.toString().startsWith(ex) || path.toString().endsWith(ex));
    }
}
