package cz.cuni.mff.jordanpa.minigit;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.commands.highlevel.HelpCommand;
import cz.cuni.mff.jordanpa.minigit.commands.PluginLoader;

import java.util.Arrays;
import java.util.Map;

/**
 * Git clone in Java, with a subset of git features.
 * Also supports multiple repositories handling.
 * See user documentation for usage and programmer documentation for implementation details.
 */
public class MiniGit {

    /**
     * Private constructor to prevent instantiation.
     */
    private MiniGit() {}

    static void main(String[] args) {
        Map<String, Command> plugins = PluginLoader.getLoadedPlugins();
        if (args.length == 0) {
            IO.println("Usage: minigit <command> [<args>]. Type minigit help for list of available commands.");
            System.exit(1);
        }
        else {
            Command cmd = plugins.get(args[0]);
            if (cmd == null) {
                System.out.println("Unknown command: " + args[0] + ". Type help for list of available commands.");
                System.exit(1);
            }
            System.exit(cmd.execute(Arrays.copyOfRange(args, 1, args.length)));
        }
    }
}
