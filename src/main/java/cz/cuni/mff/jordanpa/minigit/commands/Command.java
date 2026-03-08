package cz.cuni.mff.jordanpa.minigit.commands;

/**
 * Command interface. To create a new command, implement this interface
 * and register it in the PluginLoader in resources/META-INF/services/cz.cuni.mff.jordanpa.minigit.commands.Command.
 */
public interface Command {
    String name();
    String shortHelp();

    String help();

    String usage();

    int execute(String[] args);
}
