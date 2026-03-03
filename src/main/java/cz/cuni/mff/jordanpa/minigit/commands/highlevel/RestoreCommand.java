package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RestoreCommand implements Command {

    @Override
    public String name() {
        return "restore";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Return the repository (or all repos in project manager) to index state (remove unstaged changes).";
    }

    @Override
    public int execute(String[] args) {
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Restoring " + repo.getRootPath() + " ...");
                Map<Path, String> trackedFiles = repo.getTrackedFiles();
                for (var entry : trackedFiles.entrySet()) {
                    boolean shouldOverwrite = true;
                    if (Files.exists(entry.getKey())) {
                        Blob currentBlob = new Blob(entry.getKey());
                        if (currentBlob.miniGitSha1().equals(entry.getValue())) {
                            shouldOverwrite = false;
                        }
                    }
                    if (shouldOverwrite) {
                        MiniGitObject trackedObject = repo.loadFromInternal(entry.getValue());
                        if (trackedObject instanceof Blob trackedBlob) {
                            trackedBlob.writeContentsTo(entry.getKey());
                        }
                        else {
                            IO.println("Error: tracked file is not a blob. Repository is corrupted.");
                            return 1;
                        }
                    }
                }
                IO.println("Restored to last saved index state in " + repo.getRootPath());
            }

        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
