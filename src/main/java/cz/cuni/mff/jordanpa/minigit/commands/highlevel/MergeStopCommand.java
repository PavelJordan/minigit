package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class MergeStopCommand implements Command {
    @Override
    public String name() {
        return "merge-stop";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Stop merging. Will not change anything in index/CWD";
    }

    @Override
    public String usage() {
        return "minigit merge-stop";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            repo.stopMerge();
            repo.save();
            IO.println("Merge (if any) stopped.");
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
