package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class InspectCommand implements Command {
    @Override
    public String name() {
        return "inspect";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Inspect the repository database for the given hash.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 hash.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            MiniGitObject obj = repo.loadFromInternal(args[0]);
            if (obj == null) {
                IO.println("Object with specified hash does not exist.");
                return 1;
            }
            IO.println(obj.getDescription());
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
