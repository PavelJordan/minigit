package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;

public final class CheckoutCommand implements Command{
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
        return "Checkout a branch, tag or commit hash. Update head to point to it.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch, tag or commit hash.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            MiniGitObject obj = repo.loadFromInternal(args[0]);
            if (obj instanceof Commit commit) {
                MiniGitObject maybeTree = repo.loadFromInternal(commit.getTreeHash());
                if (maybeTree instanceof Tree tree) {
                    repo.checkoutTree(tree);
                    repo.setCommitAsNewHeadAndStoreInternally(commit);
                    repo.save();
                    IO.println("Currently at commit " + commit.miniGitSha1());
                }
            }
            else {
                IO.println("Object with specified hash or name is not a commit.");
                return 1;
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
