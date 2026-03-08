package cz.cuni.mff.jordanpa.minigit.commands.lowlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates a new blob object from the given file and inserts it into the repository database (.minigit/objects).
 *
 * <p>
 *     It does not work with a project manager - only one repository at a time.
 * </p>
 */
public final class BlobCommand implements Command {
    /**
     * Constructor for the command.
     */
    public BlobCommand() {}

    @Override
    public String name() {
        return "blob";
    }

    @Override
    public String shortHelp() {
        return "Create a new blob object from file";
    }

    @Override
    public String help() {
        return "Create a new blob object from file and inserts it into the repository database. Shows you it's hash. " +
                "You can then use inspect to see the contents of the blob.";
    }

    @Override
    public String usage() {
        return "minigit blob <file>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide 1 file path.");
            return 1;
        }
        try{
            Path repoDir = Path.of(".minigit");
            Repository repo = Repository.load(repoDir);
            Path filePath = Path.of(args[0]);
            if (!Files.exists(filePath)) {
                IO.println("File does not exist.");
                return 1;
            }
            Blob blob = new Blob(filePath);

            // Set-ups the blob to be stored in the database
            repo.storeInternally(blob);

            // Actually save the file into .minigit/objects
            repo.save();
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        IO.println("Blob successfully created.");
        return 0;
    }
}
