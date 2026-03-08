package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Command that unstages all changes in the repository.
 *
 * <p>
 *     Works with a project manager - applies to all repositories in the project.
 * </p>
 */
public final class RestoreStagedCommand implements Command {
    /**
     * Constructor for the command.
     */
    public RestoreStagedCommand() {}

    @Override
    public String name() {
        return "restore-staged";
    }

    @Override
    public String shortHelp() {
        return "Remove staged changes";
    }

    @Override
    public String help() {
        return "Unstage all changes in this repository, or multiple repos in project manager dir.";
    }


    @Override
    public String usage() {
        return "minigit restore-staged <file> [<file> ...]";
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
                repo.unstageToLastCommit();
                repo.save();
                IO.println("Everything unstaged in " + repo.getRootPath());
            }

        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
