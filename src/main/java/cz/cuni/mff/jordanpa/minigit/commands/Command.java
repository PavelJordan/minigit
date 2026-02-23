package cz.cuni.mff.jordanpa.minigit.commands;

/**
 * Command interface. To create a new command, implement this interface in this "commands" package.
 */
public interface Command {
    String name();

    String help();

    int execute();
}
