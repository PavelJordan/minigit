package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;

public final class ProjectAddCommand implements Command {
    @Override
    public String name() {
        return "project-add";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Add repository to project manager in the current directory.";
    }

    @Override
    public String usage() {
        return "minigit project-add <path>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            System.out.println("Incorrect number of arguments. Provide at least 1 repository path (to their root).");
            return 1;
        }
        try {
            ProjectManager pm = ProjectManager.load(Path.of("./"));
            for (String path : args) {
                IO.println("Adding " + path + "...");
                pm.addProject( Path.of(path));
            }
            pm.save();
            IO.println("Done.");
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
