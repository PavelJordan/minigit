package cz.cuni.mff.jordanpa.minigit.commands;

import java.util.*;

/**
 * Static class that loads all plugins implementing Command from the "commands" package.
 * I used ChatGPT to help me set up the service loader. Later in the summer semester, I learned how to use it,
 * so now I wouldn't need him to help me with this.
 */
public final class PluginLoader {
    private static Map<String, Command> commands;
    private static Map<String, Command> loadAllCommands() {
        Map<String, Command> commandRegistry = new HashMap<>();
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);

        for (Command cmd : loader) {
            commandRegistry.put(cmd.name(), cmd);
        }

        return commandRegistry;
    }

    public static Map<String, Command> getLoadedPlugins() {
        if (commands != null) {
            return commands;
        }
        commands = loadAllCommands();
        return commands;
    }
}
