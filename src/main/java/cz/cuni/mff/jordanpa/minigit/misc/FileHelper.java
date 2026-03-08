package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FileHelper {

    /**
     *
     * Get all paths here
     *
     * <p>
     *     If the path is dir -> all files in dir and subdirs.
     *     If the path is file -> only that file
     *     excludes anything in toExclude - either starts with or ends with.
     * </p>
     * @param path Path to get all files from (file or directory)
     * @param toExclude patterns to exclude (if paths start or end with it, including sub-paths)
     * @return List of paths to all files in the directory and subdirectories, or single file if the path is a file.
     * @throws IOException If the path is invalid, or some other I/O error occurs.
     */
    public static List<Path> getAllFiles(Path path, List<String> toExclude) throws IOException {
        path = path.normalize();
        ArrayList<Path> paths = new ArrayList<>();
        Path CWD = Path.of("./");

        // Path is file -> return only that file
        if (!Files.isDirectory(path)) {
            if (isExcluded(path, toExclude)) {
                return List.of();
            }
            return List.of(getRelativePathToDirectory(path, CWD));
        }

        // Path is directory -> return all files in that directory (recursively)
        try(var dirStream = Files.newDirectoryStream(path)) {
            for (Path entry : dirStream) {

                // Test if it is excluded
                if (isExcluded(entry, toExclude)) {
                    continue;
                }

                // Test if it is a directory (go recursively)
                if (Files.isDirectory(entry)) {
                    paths.addAll(getAllFiles(entry, toExclude));
                }

                // File -> add to list
                else {
                    paths.add(getRelativePathToDirectory(entry, CWD));
                }
            }
        }
        return paths;
    }

    public static Path getRelativePathToDirectory(Path entry, Path directory) {
        return directory.toAbsolutePath().relativize(entry.toAbsolutePath()).normalize();
    }

    /**
     * Test if the path should be excluded based on the patterns (If it starts or ends with any of the patterns)
     * @return True if it should be excluded based on the patterns, false otherwise.
     */
    public static boolean isExcluded(Path path, List<String> toExclude) {
        return toExclude.stream().anyMatch(ex -> path.toString().startsWith(ex) || path.toString().endsWith(ex));
    }

    public static List<Repository.FileStatus> getFileStatusesFromComparison(HashMap<Path, String> currentIndex, HashMap<Path, String> indexToCompare) {
        List<Repository.FileStatus> statuses = new ArrayList<>();
        for (Path path : currentIndex.keySet()) {
            String workingDirectoryHash = currentIndex.get(path);
            Repository.FileStatusType againstCommit;
            if (indexToCompare.containsKey(path)) {
                if (indexToCompare.get(path).equals(workingDirectoryHash)) {
                    againstCommit = Repository.FileStatusType.SAME;
                }
                else {
                    againstCommit = Repository.FileStatusType.MODIFIED;
                }
            }
            else {
                againstCommit = Repository.FileStatusType.NEW;
            }
            statuses.add(new Repository.FileStatus(path, againstCommit));
        }
        var deletedAgainstStaged = indexToCompare.keySet().stream().filter(path -> !currentIndex.containsKey(path));
        deletedAgainstStaged.forEach(path -> statuses.add(new Repository.FileStatus(path, Repository.FileStatusType.DELETED)));
        return statuses;
    }
}
