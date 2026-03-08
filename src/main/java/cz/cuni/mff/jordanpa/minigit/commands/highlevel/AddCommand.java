package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class AddCommand implements Command {
    @Override
    public String name() {
        return "add";
    }

    @Override
    public String shortHelp() {
        return "Stage files for commit/merge-apply.";
    }

    @Override
    public String help() {
        return "Add/update files into the staging area (index) in repo or all repos in project manager. You can provide list of files to add as arguments.";
    }

    @Override
    public String usage() {
        return "minigit add <file> [<file> ...]";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            IO.println("Incorrect number of arguments. Provide at least 1 file to add.");
            return 1;
        }
        try {
            List<Repository> reposHere = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : reposHere) {
                IO.println("Updating " + repo.getRootPath() + " ...");
                List<String> toIgnore = repo.getIgnored();
                for (String pattern : args) {
                    for (Path file: FileHelper.getAllFiles(Path.of(pattern), toIgnore)) {
                        boolean successful = repo.addToIndex(file);
                        if (successful) {
                            IO.println("Staging " + file + "...");
                        }
                    }
                }
                repo.save();
                IO.println("Done.");
            }
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
