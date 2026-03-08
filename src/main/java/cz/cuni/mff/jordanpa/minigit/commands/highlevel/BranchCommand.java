package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Command that creates a new branch at the current commit (HEAD). If no HEAD is present, the command fails (gracefully, but exit code still 1).
 *
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class BranchCommand implements Command {
    @Override
    public String name() {
        return "branch";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Create a branch that points to the current commit.";
    }

    @Override
    public String usage() {
        return "minigit branch <branch-name>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name.");
            return 1;
        }
        try {
            String branchName = args[0];
            Repository repo = Repository.load(Path.of(".minigit"));
            Head head = repo.getHead();
            if (head.type() == Head.Type.COMMIT) {
                repo.setBranch(branchName, head.data());
            }
            else if (head.type() == Head.Type.BRANCH) {
                repo.setBranch(branchName, repo.getHeadCommitHash());
            }
            else {
                IO.println("Cannot create branch. There is no HEAD yet.");
                return 1;
            }
            repo.save();
            IO.println("Branch " + branchName + " successfully created.");
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
