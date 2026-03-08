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

/**
 * Command that restores the repository to the index state (removes unstaged changes).
 *
 * <p>
 *     Works with a project manager - applies to all repositories in the project.
 * </p>
 */
public final class RestoreCommand implements Command {

    @Override
    public String name() {
        return "restore";
    }

    @Override
    public String shortHelp() {
        return "Remove unstaged changes";
    }

    @Override
    public String help() {
        return "Return the repository (or all repos in project manager) to index state (remove unstaged changes).";
    }

    @Override
    public String usage() {
        return "minigit restore <file> [<file> ...]";
    }

    @Override
    public int execute(String[] args) {
        try {
            if (args.length != 0) {
                IO.println("Incorrect number of arguments. Provide no arguments.");
                return 1;
            }
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Restoring " + repo.getRootPath() + " ...");

                // Get staged files
                Map<Path, String> trackedFiles = repo.getTrackedFiles();

                for (var entry : trackedFiles.entrySet()) {
                    boolean shouldOverwrite = true;

                    // Test whether the staged file is the same as the current file
                    if (Files.exists(entry.getKey())) {
                        Blob currentBlob = new Blob(entry.getKey());
                        if (currentBlob.miniGitSha1().equals(entry.getValue())) {
                            shouldOverwrite = false;
                        }
                    }

                    // If not, overwrite the file
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
