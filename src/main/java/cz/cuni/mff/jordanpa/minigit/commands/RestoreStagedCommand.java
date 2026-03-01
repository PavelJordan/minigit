package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class RestoreStagedCommand implements Command{
    @Override
    public String name() {
        return "restore-staged";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Unstage all changes";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            repo.unstageToLastCommit();
            IO.println("Everything unstaged.");
            repo.save();
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
