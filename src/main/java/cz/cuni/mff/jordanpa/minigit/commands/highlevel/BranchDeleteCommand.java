package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;

public final class BranchDeleteCommand implements Command {

    @Override
    public String name() {
        return "branch-delete";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Delete the specified branch. Works only if it is not the current branch and stays reachable from HEAD.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name.");
            return 1;
        }
        throw new UnsupportedOperationException("Not yet implemented");
        // return 0;
    }
}
