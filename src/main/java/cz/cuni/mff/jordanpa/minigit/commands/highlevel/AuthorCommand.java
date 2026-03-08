package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Sets the author of the current repository
 *
 * <p>
 *     Works with a project manager - applies to all repositories in the project.
 * </p>
 */
public final class AuthorCommand implements Command {

    @Override
    public String name() {
        return "author";
    }

    @Override
    public String shortHelp() {
        return "set name and email";
    }

    @Override
    public String help() {
        return "Set current author for commits in this repository or multiple repos in project manager dir. Provide name and email.";
    }

    @Override
    public String usage() {
        return "minigit author <name> <email>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 2) {
            IO.println("Incorrect number of arguments. Provide exactly 2 arguments: name email.");
            return 1;
        }
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                repo.setCurrentAuthor(new Author(args[0], args[1]));
                repo.save();
                IO.println("Author set in " + repo.getRootPath());
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
