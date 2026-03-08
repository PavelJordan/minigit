package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.MiniGitDiff;
import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Shows the diff between two blobs. The blobs are specified by their hash and must be found in the current repository.
 *
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class BlobDiffCommand implements Command {
    /**
     * Constructor for the command.
     */
    public BlobDiffCommand() {}
    @Override
    public String name() {
        return "blob-diff";
    }

    @Override
    public String shortHelp() {
        return "Show diff between two blobs";
    }

    @Override
    public String help() {
        return "Show diff of the specified 2 blobs, provided by their hash. You can find their hashes " +
                "with the command minigit inspect on commit or tree objects.";
    }

    @Override
    public String usage() {
        return "minigit blob-diff <hash1> <hash2>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 2) {
            IO.println("Incorrect number of arguments. Provide exactly 2 data.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));

            // Load both blobs
            MiniGitObject blob1Obj = repo.loadFromInternal(args[0]);
            MiniGitObject blob2Obj = repo.loadFromInternal(args[1]);
            if (!(blob1Obj instanceof Blob blob1) || !(blob2Obj instanceof Blob blob2)) {
                IO.println("Blobs with specified data does not exist.");
                return 1;
            }

            // Diff them
            List<MiniGitDiff.DiffResult> result = MiniGitDiff.diff(blob1, blob2);

            // Get their lines to show the lines that differ
            List<String> lines1 = new String(blob1.getContentReader().readAllBytes()).lines().toList();
            List<String> lines2 = new String(blob2.getContentReader().readAllBytes()).lines().toList();

            // Print the diff - go over the results, and print the lines that differ
            for (MiniGitDiff.DiffResult diffResult : result) {
                IO.println("Change from line " + (diffResult.replaceFrom() + 1));
                IO.println("<<<<<<<<<<<< FROM");
                for (int i = (int) diffResult.replaceFrom(); i < diffResult.replaceTo(); i++) {
                    IO.println(lines1.get(i));
                }
                IO.println("============");
                for (int i = (int) diffResult.replaceWithFrom(); i < diffResult.replaceWithTo(); i++) {
                    IO.println(lines2.get(i));
                }
                IO.println(">>>>>>>>>>>> TO");
            }
        }
        catch (IOException ex) {
            IO.println(ex);
            return 1;
        }
        return 0;
    }
}
