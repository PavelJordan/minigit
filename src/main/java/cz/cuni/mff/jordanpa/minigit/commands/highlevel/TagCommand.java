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
            Head head = repo.getHead();
            if (head.type() == Head.Type.UNSET) {
                IO.println("No commits yet.");
                return 1;
            }
            if (head.type() == Head.Type.COMMIT) {
                repo.setTag(args[0], head.data());
            }
            else if (head.type() == Head.Type.BRANCH) {
                repo.setTag(args[0], repo.getBranches().get(head.data()));
            }
            repo.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
