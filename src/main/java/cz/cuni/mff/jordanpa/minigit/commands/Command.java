package cz.cuni.mff.jordanpa.minigit.commands;

/**
 * Command interface. All commands must implement this.
 *
 * <p>
 *     To create a new command, implement this interface
 *     and register it in the PluginLoader in resources/META-INF/services/cz.cuni.mff.jordanpa.minigit.commands.Command.
 * </p>
 */
public interface Command {

    /**
     * The name of the command as will be used in the command line.
     */
    String name();

    /**
     * Usage message for the command, seen in `minigit help name` right before shortHelp
     */
    String usage();

    /**
     * Short help message for the command, seen in `minigit help` and in `minigit help name`
     */
    String shortHelp();

    /**
     * Long help message for the command, seen only in `minigit help name` right after shortHelp.
     */
    String help();

    /**
     * Execute the command.
     * @param args The arguments of the command that follow the command name.
     * @return The exit code of the command.
     */
    int execute(String[] args);
}
