package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Command that initializes the projects manager for the current directory.
 *
 * <p>
 *     The directory must not contain .minigit, only the subdirectories.
 * </p>
 */
public final class ProjectsInitCommand implements Command {
    @Override
    public String name() {
        return "projects-init";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Initialize projects manager for repositories in the current directory.";
    }

    @Override
    public String usage() {
        return "minigit projects-init";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 0) {
            System.out.println("Incorrect number of arguments. Provide no arguments.");
            return 1;
        }
        try {
            ProjectManager pm = new ProjectManager(Path.of("./"));
            pm.save();
            IO.println("Projects manager initialized.");
        } catch (IOException | RuntimeException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
