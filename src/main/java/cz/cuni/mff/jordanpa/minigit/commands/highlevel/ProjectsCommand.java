package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;

public final class ProjectsCommand implements Command {
    @Override
    public String name() {
        return "projects";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "List all repositories registered in the project manager in the current directory.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 0) {
            System.out.println("Incorrect number of arguments. Provide no arguments.");
            return 1;
        }
        try {
            ProjectManager pm = ProjectManager.load(Path.of("./"));
            IO.println("Repositories:");
            pm.getProjects().forEach(System.out::println);
        }
        catch (IOException ex) {
            IO.println(ex);
            return 1;
        }
        return 0;
    }
}
