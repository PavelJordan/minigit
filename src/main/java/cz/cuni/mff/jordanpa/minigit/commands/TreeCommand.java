package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class TreeCommand implements Command{
    @Override
    public String name() {
        return "tree";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Create a tree object from the staging area (index) and insert it into the repository database.";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            Map<Path, String> trackedFiles = repo.getTrackedFiles();
            List<Tree> trees = Tree.buildTree(trackedFiles);
            IO.println("\nPrinting descriptions");
            for (Tree tree : trees) {
                IO.println("Tree hash: " + tree.miniGitSha1() + " contents: ");
                IO.println(tree.getDescription());
            }
            trees.forEach(repo::storeInternally);
            repo.save();
            IO.println("Tree successfully built. The root tree hash is: " + trees.getLast().miniGitSha1());
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
