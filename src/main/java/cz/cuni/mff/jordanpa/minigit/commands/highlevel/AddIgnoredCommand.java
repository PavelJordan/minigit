package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class AddIgnoredCommand implements Command {
    @Override
    public String name() {
        return "add-ignored";
    }

    @Override
    public String shortHelp() {
        return "Stage ignored files for deletion.";
    }

    @Override
    public String help() {
        return "Stages specified ignored files for deletion. Works well if you want to delete newly ignored files. Works in repos and multiple repos in project manager.";
    }

    @Override
    public String usage() {
        return "minigit add-ignored <pattern>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            IO.println("Incorrect number of arguments. Provide at least 1 file to add.");
            return 1;
        }
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Updating " + repo.getRootPath() + " ...");
                List<String> ignored = repo.getIgnored();
                for (String pattern : args) {
                    for (Path file: FileHelper.getAllFiles(Path.of(pattern), List.of())) {
                        if (FileHelper.isExcluded(file, ignored)) {
                            IO.println("Staging ignored file " + file + " for deletion...");
                            repo.addToIndex(file);
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
