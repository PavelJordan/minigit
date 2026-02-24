package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public class AddCommand implements Command{
    @Override
    public String name() {
        return "add";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Add/update files into the staging area (index). You can provide list of files to add as arguments.";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            for (String arg : args) {
                IO.println("Adding " + arg + "...");
                repo.addToIndex(Path.of(arg));
            }
            repo.save();
            IO.println("Done.");
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
