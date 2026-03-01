package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class TagCommand implements Command {
    @Override
    public String name() {
        return "tag";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Create single tag from HEAD with a name.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 tag name.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            String hashToTag = repo.getHeadCommitHash();
            if (hashToTag == null) {
                IO.println("No commits yet.");
                return 1;
            }
            repo.setTag(args[0], hashToTag);
            IO.println("Tag " + args[0] + " created.");
            repo.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
