package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Command that deletes a branch. If the branch is HEAD, the command fails, similarly, if the branch does not exist (gracefully, but exit code still 1).
 *
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class BranchDeleteCommand implements Command {
    /**
     * Constructor for the command.
     */
    public BranchDeleteCommand() {}

    @Override
    public String name() {
        return "branch-delete";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Delete the specified branch.";
    }

    @Override
    public String usage() {
        return "minigit branch-delete <branch-name>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            String branchName = args[0];
            String branchHash = repo.getBranches().get(branchName);
            if (branchHash == null) {
                IO.println("Branch does not exist.");
                return 1;
            }
            if (repo.getHead().type() == Head.Type.BRANCH && repo.getHead().data().equals(branchName)) {
                IO.println("Cannot delete HEAD branch.");
                return 1;
            }
            repo.deleteBranch(branchName);
            repo.save();
            IO.println("Branch " + branchName + " deleted. It pointed to " + branchHash + ".");
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
