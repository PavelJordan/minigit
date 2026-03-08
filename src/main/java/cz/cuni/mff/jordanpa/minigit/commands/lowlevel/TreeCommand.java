package cz.cuni.mff.jordanpa.minigit.commands.lowlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Command that creates a tree object from the staging area (index) and inserts it into the repository database.
 *
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class TreeCommand implements Command {
    @Override
    public String name() {
        return "tree";
    }

    @Override
    public String shortHelp() {
        return "Save the staged files";
    }

    @Override
    public String help() {
        return "Create a tree object from the staging area (index) and insert it into the repository database. " +
                "You can now use inspect to see the contents of the tree on its hash.";
    }

    @Override
    public String usage() {
        return "minigit tree <directory>";
    }

    @Override
    public int execute(String[] args) {
        try {
            if (args.length != 0) {
                IO.println("Incorrect number of arguments. Provide no arguments.");
                return 1;
            }
            Repository repo = Repository.load(Path.of(".minigit"));

            // Get the staged files (also named tracked files). As they are staged, they are already saved.
            Map<Path, String> trackedFiles = repo.getTrackedFiles();

            // Build the tree to save (includes the subtrees needed)
            // The root path specifies which tree should be the root tree, so when saved, it suits the repository root.
            List<Tree> trees = Tree.buildTree(trackedFiles, repo.getRootPath());

            // Print the hashes of all the trees. The last tree will be the root.
            IO.println("\nPrinting descriptions:");
            for (Tree tree : trees) {
                IO.println(tree.getDescription());
            }

            // Set up the tree objects to be saved into .minigit/objects
            trees.forEach(repo::storeInternally);

            // Commit the changes (save the trees)
            repo.save();

            IO.println("Tree successfully built. The root tree data is: " + trees.getLast().miniGitSha1());
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
