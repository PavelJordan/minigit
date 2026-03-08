package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command that creates a new repository in the current directory.
 */
public final class InitCommand implements Command {
    /**
     * Constructor for the command.
     */
    public InitCommand() {}

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String shortHelp() {
        return "Initialize new repository";
    }

    @Override
    public String help() {
        return "Creates new repository in the current directory if there is none yet.";
    }

    @Override
    public String usage() {
        return "minigit init";
    }

    @Override
    public int execute(String[] args) {
        Path minigitPath = Path.of(".minigit");
        Path topMiniGitPath = Path.of(".topminigit");
        if (Files.exists(minigitPath) || Files.exists(topMiniGitPath)) {
            IO.println("Repository can't be created in this directory because it already exists or it is a project manager directory.");
            return 1;
        }
        try {
            Files.createDirectory(minigitPath);
            Repository repo = new Repository(minigitPath);
            repo.save();
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
