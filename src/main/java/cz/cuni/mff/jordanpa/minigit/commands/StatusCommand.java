package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            List<Repository.FileStatus> statuses = repo.getStatus();

            var untrackedPaths = statuses.stream().filter(fileStatus -> fileStatus.status() == Repository.FileStatusType.UNTRACKED).map(Repository.FileStatus::path).toList();
            var modifiedPaths = statuses.stream().filter(fileStatus -> fileStatus.status() == Repository.FileStatusType.MODIFIED).map(Repository.FileStatus::path).toList();
            var deletedPaths = statuses.stream().filter(fileStatus -> fileStatus.status() == Repository.FileStatusType.DELETED).map(Repository.FileStatus::path).toList();
            var trackedPaths = statuses.stream().filter(fileStatus -> fileStatus.status() == Repository.FileStatusType.TRACKED).map(Repository.FileStatus::path).toList();

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
            trackedPaths.forEach(IO::println);
            IO.println("_____________");
            return 0;
        }
        catch(IOException e) {
            IO.println(e);
            return 1;
        }
    }
}
