package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.commands.PluginLoader;

import java.util.Comparator;
import java.util.Map;

/**
 * Command that prints the help message of all commands.
 * With a parameter, it prints the help message of the specified command.
 */
public final class HelpCommand implements Command {

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Print this help message";
    }

    @Override
    public String usage() {
        return "minigit help [<command>]";
    }

    @Override
    public int execute(String[] args) {
        try {
            Map<String, Command> plugins = PluginLoader.getLoadedPlugins();
            if (args.length == 1) {
                System.out.println("Usage: " + plugins.get(args[0]).usage());
                System.out.println();
                System.out.println(plugins.get(args[0]).shortHelp());
                System.out.println();
                System.out.println(plugins.get(args[0]).help());
                return 0;
            }
            System.out.println("Usage: minigit <command> [<args>]");
            System.out.println("Available commands:");
            plugins.values().stream().sorted(Comparator.comparing(Command::name)).forEach(command -> System.out.println("  " + command.name() + " ".repeat(20 - command.name().length()) + command.shortHelp()));
        }
        catch (Exception e) {
            IO.println(e);
            IO.println("Error loading plugins. Contact the developer.");
            return 1;
        }
        return 0;
    }
}
