package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Command that deletes a tag.
 *
 * <p>
 *     Works with a project manager - applies to all repositories in the project.
 * </p>
 */
public final class TagDeleteCommand implements Command {
    /**
     * Constructor for the command.
     */
    public TagDeleteCommand() {}

    @Override
    public String name() {
        return "tag-delete";
    }

    @Override
    public String shortHelp() {
        return "Delete specified tag";
    }

    @Override
    public String help() {
        return "Deletes specified tag in this repo or multiple repos in project manager dir.";
    }

    @Override
    public String usage() {
        return "minigit tag-delete <tag-name>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name.");
            return 1;
        }
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Deleting tag " + args[0] + " in " + repo.getRootPath() + " ...");
                String tagName = args[0];
                String tagHash = repo.getTags().get(tagName);
                if (tagHash == null) {
                    IO.println("Tag does not exist.");
                    continue;
                }
                repo.deleteTag(tagName);
                repo.save();
                IO.println("Tag " + tagName + " deleted. It pointed to " + tagHash + ".");
            }

        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
