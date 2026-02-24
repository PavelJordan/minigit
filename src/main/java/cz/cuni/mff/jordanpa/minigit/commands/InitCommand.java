package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InitCommand implements Command {

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Creates new repository in the current directory if there is none yet.";
    }

    @Override
    public int execute(String[] args) {
        Path minigitPath = Path.of(".minigit");
        if (Files.exists(minigitPath)) {
            IO.println("Repository already exists in this directory.");
            return 1;
        }
        try {
            Files.createDirectory(minigitPath);
            Repository repo = new Repository();
            repo.save(minigitPath);
        }
        catch (IOException e) {
            IO.println(e);
            IO.println("Error creating repository. Check your permissions and delete the .minigit directory manually if needed.");
            return 1;
        }
        IO.println("Repository successfully created.");
        return 0;
    }
}
