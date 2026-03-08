package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class RefsCommand implements Command {
    @Override
    public String name() {
        return "refs";
    }

    @Override
    public String shortHelp() {
        return "List all branches and tags";
    }

    @Override
    public String help() {
        return "List all branches and tags in this repository or multiple repos in project manager dir.";
    }

    @Override
    public String usage() {
        return "minigit refs";
    }

    @Override
    public int execute(String[] args) {
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("\nRepository: " + repo.getRootPath());
                IO.println("Branches: ");
                for (var name : repo.getBranches().keySet()) {
                    IO.println(name);
                }
                IO.println("\nTags: ");
                for (var name : repo.getTags().keySet()) {
                    IO.println(name);
                }
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
