package cz.cuni.mff.jordanpa.minigit.commands;

public final class InitCommand implements Command {

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Creates new repository in the current directory if there is none yet.";
    }

    @Override
    public int execute(String[] args) {
        return 0;
    }
}
