package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;

public final class CheckoutCommand implements Command {
    @Override
    public String name() {
        return "checkout";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Checkout a branch, tag or commit data. Update head to point to it.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch, tag or commit data.");
            return 1;
        }
        try {
            String checkoutTo = args[0];
            Repository repo = Repository.load(Path.of(".minigit"));
            MiniGitObject obj = repo.loadFromInternal(checkoutTo);
            if (obj instanceof Commit commit) {
                MiniGitObject maybeTree = repo.loadFromInternal(commit.getTreeHash());
                if (maybeTree instanceof Tree tree) {
                    repo.checkoutTree(tree);
                    if (repo.isBranch(checkoutTo)) {
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
            }
            else {
                IO.println("Object with specified data or name is not a commit.");
                return 1;
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
