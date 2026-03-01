package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class RefsCommand implements Command {
    @Override
    public String name() {
        return "refs";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "List all branches and tags.";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            IO.println("Branches: ");
            for (var name : repo.getBranches().keySet()) {
                IO.println(name);
            }
            IO.println("\nTags: ");
            for (var name : repo.getTags().keySet()) {
                IO.println(name);
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
