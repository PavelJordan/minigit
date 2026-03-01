package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class TagDeleteCommand implements Command {
    @Override
    public String name() {
        return "tag-delete";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Deletes specified tag.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            String tagName = args[0];
            String tagHash = repo.getTags().get(tagName);
            if (tagHash == null) {
                IO.println("Tag does not exist.");
                return 1;
            }
            repo.deleteTag(tagName);
            repo.save();
            IO.println("Tag " + tagName + " deleted. It pointed to " + tagHash + ".");
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
