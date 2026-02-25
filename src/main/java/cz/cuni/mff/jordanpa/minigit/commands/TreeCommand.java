package cz.cuni.mff.jordanpa.minigit.commands;

public final class TreeCommand implements Command{
    @Override
    public String name() {
        return "tree";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Create a tree object from the staging area (index) and insert it into the repository database.";
    }

    @Override
    public int execute(String[] args) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
