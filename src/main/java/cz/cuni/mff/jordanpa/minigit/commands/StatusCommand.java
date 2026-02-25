package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class StatusCommand implements Command {
    @Override
    public String name() {
        return "status";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Print the status of the current index";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            List<Path> existingPaths = getPaths(Path.of("./"));
            Set<Path> trackedPaths = repo.getTrackedFiles();
            ArrayList<Path> untrackedPaths = new ArrayList<>();
            ArrayList<Path> modifiedPaths = new ArrayList<>();
            for (Path path : existingPaths) {
                if (!trackedPaths.contains(path)) {
                    untrackedPaths.add(path);
                }
                else {
                    String trackedHash = repo.trackedFileHash(path);
                    Blob newBlob = new Blob(path);
                    if (!trackedHash.equals(newBlob.miniGitSha1())) {
                        modifiedPaths.add(path);
                    }
                }
            }
            List<Path> deletedPaths = trackedPaths.stream().filter(path -> !existingPaths.contains(path)).toList();
            if (!untrackedPaths.isEmpty()) {
                IO.println("Untracked files:");
                untrackedPaths.forEach(IO::println);
                IO.println("_____________");
            }
            if (!modifiedPaths.isEmpty()) {
                IO.println("Modified tracked files:");
                modifiedPaths.forEach(IO::println);
                IO.println("_____________");
            }
            if (!deletedPaths.isEmpty()) {
                IO.println("Deleted tracked files:");
                deletedPaths.forEach(IO::println);
                IO.println("_____________");
            }
            IO.println("Up-to-date tracked files:");
            trackedPaths.stream().filter(path -> !deletedPaths.contains(path) && !modifiedPaths.contains(path)).forEach(IO::println);
            IO.println("_____________");
            return 0;
        }
        catch(IOException e) {
            IO.println(e);
            return 1;
        }
    }

    private List<Path> getPaths(Path path) {
        ArrayList<Path> paths = new ArrayList<>();
        try(var dirStream = Files.newDirectoryStream(path)) {
            for (Path entry : dirStream) {
                if (entry.getFileName().toString().equals(".minigit")) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    paths.addAll(getPaths(entry));
                }
                else {
                    paths.add(entry.normalize());
                }
            }
        }
        catch(IOException e) {
            IO.println(e);
        }
        return paths;
    }
}
