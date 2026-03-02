package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.commands.PluginLoader;

import java.util.Comparator;
import java.util.Map;

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
    public int execute(String[] args) {
        try {
            Map<String, Command> plugins = PluginLoader.getLoadedPlugins();
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
