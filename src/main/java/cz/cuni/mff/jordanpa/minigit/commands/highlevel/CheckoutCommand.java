package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.IOException;
import java.nio.file.Path;

public final class CheckoutCommand implements Command {
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

            if (repo.workingTreeDirty()) {
                IO.println("Working tree is not clean - cannot checkout.");
                return 1;
            }

            if (checkoutTo.equals(HEAD)) {
                checkoutTo = repo.getHeadCommitHash();
                if (checkoutTo == null) {
                    IO.println("No commits yet.");
                    return 1;
                }
            }

            MiniGitObject obj = repo.loadFromInternal(checkoutTo);
            if (!(obj instanceof Commit commit)) {
                IO.println("Object with specified data or name is not a commit.");
                return 1;
            }

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
            MiniGitObject maybeTree = repo.loadFromInternal(commit.getTreeHash());
            if (maybeTree instanceof Tree tree) {
                repo.checkoutTree(tree);
                if (repo.isBranch(checkoutTo) && moveBackBy == 0) {
                    repo.setBranch(checkoutTo, commit.miniGitSha1());
                    repo.setHeadToBranch(checkoutTo);
                    IO.println("Currently at branch " + checkoutTo);
                }
                else {
                    repo.setHeadToCommit(commit);
                    IO.println("HEAD at detached state");
                }
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
