package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;

public final class TreeCheckoutCommand implements Command {
    @Override
    public String name() {
        return "tree-checkout";
    }

    @Override
    public String shortHelp() {
        return "Load tree contents into CWD";
    }

    @Override
    public String help() {
        return "Checkout a tree object. Provide one hash. HEAD stays - you can rollback commits this way by unstaging based on what you want.";
    }

    @Override
    public String usage() {
        return "minigit tree-checkout <tree-hash>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 data.");
            return 1;
        }
        String treeHash = args[0];
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            MiniGitObject possiblyTree = repo.loadFromInternal(treeHash);
            if (possiblyTree instanceof Tree tree) {
                repo.checkoutTree(tree);
            }
            else {
                IO.println("Object with specified data is not a tree.");
                return 1;
            }
            repo.save();
            IO.println("Currently at tree " +  treeHash);
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
