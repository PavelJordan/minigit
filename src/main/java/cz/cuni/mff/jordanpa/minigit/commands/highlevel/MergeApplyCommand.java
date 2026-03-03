package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class MergeApplyCommand implements Command {
    @Override
    public String name() {
        return "merge-apply";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Apply merge that is currently in progress. Use the index state (staged) as the merge result. Provide message for commit. Will fail if HEAD is different from the merge base.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 message.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            boolean successful = repo.mergeFromIndex(args[0]);
            if (!successful) {
                IO.println("Merge failed.");
                return 1;
            }
            repo.save();
        }
        catch(IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
