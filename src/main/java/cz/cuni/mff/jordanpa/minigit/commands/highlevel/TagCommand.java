package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
        return "Create single tag from HEAD with a name in this repo or multiple repos in project manager dir.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 tag name.");
            return 1;
        }
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Creating tag " + args[0] + " in " + repo.getRootPath() + " ...");
                String hashToTag = repo.getHeadCommitHash();
                if (hashToTag == null) {
                    IO.println("No commits yet.");
                    continue;
                }
                repo.setTag(args[0], hashToTag);
                repo.save();
                IO.println("Tag " + args[0] + " created.");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
