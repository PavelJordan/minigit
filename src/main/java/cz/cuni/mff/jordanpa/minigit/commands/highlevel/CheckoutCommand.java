package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Command that moves the HEAD to the specified commit/branch/commit.
 *
 * <p>
 *     Gracefully fails, if the CWD is dirty (not everything is commited/staged),
 *     the specified commit does not exist, etc...
 * </p>
 * <p>
 *     User can check out to a branch, following it, or to a commit/tag, detaching the head.
 *     User can even check out the HEAD, which is useful when moving back via ~.
 * </p>
 * <p>
 *     User can specify a number of commits to move back by, e.g. "checkout HEAD~2".
 * </p>
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class CheckoutCommand implements Command {
    /**
     * Constructor for the command.
     */
    public CheckoutCommand() {}

    private static final String HEAD = "HEAD";
    @Override
    public String name() {
        return "checkout";
    }

    @Override
    public String shortHelp() {
        return "Move to commit/branch/tag";
    }

    @Override
    public String help() {
        return "Checkout a branch, tag or commit data. Update head to point to it. " +
                "Use ~NUM to move back by number of commits. If you checkout a branch, you start to follow it, otherwise, you move to detached HEAD";
    }

    @Override
    public String usage() {
        return "minigit checkout <branch-name|commit-hash>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch, tag or commit data.");
            return 1;
        }
        try {
            String checkoutTo;
            int moveBackBy;

            // First, resolve if the user wants to move back by some number of commits
            if (args[0].contains("~")) {
                checkoutTo = args[0].substring(0, args[0].indexOf("~"));
                moveBackBy = Integer.parseInt(args[0].substring(args[0].indexOf("~") + 1));
                if (moveBackBy < 0) {
                    IO.println("Cannot move back by negative number of commits.");
                    return 1;
                }
            }
            else {
                checkoutTo = args[0];
                moveBackBy = 0;
            }

            Repository repo = Repository.load(Path.of(".minigit"));

            // Ensure the CWD is clean
            if (repo.workingTreeDirty()) {
                IO.println("Working tree is not clean - cannot checkout.");
                return 1;
            }

            // If we want to check out to HEAD, we are checking out to the commit hash it points to.
            if (checkoutTo.equals(HEAD)) {
                checkoutTo = repo.getHeadCommitHash();
                if (checkoutTo == null) {
                    IO.println("No commits yet.");
                    return 1;
                }
            }

            // Now, ensure, we are pointing to a real commit
            MiniGitObject obj = repo.loadFromInternal(checkoutTo);
            if (!(obj instanceof Commit commit)) {
                IO.println("Object with specified data or name is not a commit.");
                return 1;
            }

            // Move back by the specified number of commits
            for (int i = 0; i < moveBackBy; i++) {
                if (commit.getParents().length != 1) {
                    IO.println("Cannot move back by " + moveBackBy + " commits.");
                    return 1;
                }
                obj = repo.loadFromInternal(commit.getParents()[0]);
                if (obj instanceof Commit parent) {
                    commit = parent;
                }
                else {
                    IO.println("Cannot move back by " + moveBackBy + " commits.");
                    return 1;
                }
            }

            // Load the tree from the commit we arrived at and check out to it
            MiniGitObject maybeTree = repo.loadFromInternal(commit.getTreeHash());
            if (maybeTree instanceof Tree tree) {
                repo.checkoutTree(tree);

                // If we are checking out a branch, we start following it.
                if (repo.isBranch(checkoutTo) && moveBackBy == 0) {
                    repo.setHeadToBranch(checkoutTo);
                    IO.println("Currently at branch " + checkoutTo);
                }

                // If we are at detached HEAD, we only update the head to the commit we are checking out.
                else {
                    repo.setHeadToCommit(commit);
                    IO.println("HEAD at detached state");
                }

                // Save the changes (the HEAD position)
                repo.save();

                IO.println("Working tree is commit " + commit.miniGitSha1());
            }

        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        catch (NumberFormatException e) {
            IO.println("Invalid number of commits to move back by.");
            return 1;
        }
        return 0;
    }
}
