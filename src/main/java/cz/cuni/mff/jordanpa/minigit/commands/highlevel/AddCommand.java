package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Command to add files (patterns) to the staging area (index).
 *
 * <p>
 *     User can even provide a list of files to add. The paths are supposed to be "patterns", rather than actual paths.
 * </p>
 * <p>
 *     Works with a project manager - applies to all repositories in the project.
 * </p>
 */
public final class AddCommand implements Command {
    /**
     * Constructor for the command.
     */
    public AddCommand() {}
    @Override
    public String name() {
        return "add";
    }

    @Override
    public String shortHelp() {
        return "Stage files for commit/merge-apply.";
    }

    @Override
    public String help() {
        return "Add/update files into the staging area (index) in repo or all repos in project manager. You can provide list of files to add as arguments.";
    }

    @Override
    public String usage() {
        return "minigit add <file> [<file> ...]";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            IO.println("Incorrect number of arguments. Provide at least 1 file to add.");
            return 1;
        }
        try {
            List<Repository> reposHere = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));

            // Iterate over all repositories in the project manager (or a single repository if there is no project manager)
            for (Repository repo : reposHere) {
                IO.println("Updating " + repo.getRootPath() + " ...");

                // Get ignored files from the repository (.minigitignore file)
                List<String> toIgnore = repo.getIgnored();

                // Filter out ignored files from the list of files to add
                for (String pattern : args) {
                    for (Path file: FileHelper.getAllFiles(Path.of(pattern), toIgnore)) {

                        // Add the file to the index (staged file)
                        boolean successful = repo.addToIndex(file);
                        if (successful) {
                            IO.println("Staging " + file + "...");
                        }
                    }
                }

                // Save the list of staged files
                repo.save();
                IO.println("Done.");
            }
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
