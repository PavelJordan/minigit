# Command plugins

These commands are all the commands that are available to the user by MiniGit.

To create a new command, implement the Command interface and register it in the ServiceLoader through
`resources/META-INF/services/cz.cuni.mff.jordanpa.minigit.commands.Command`.

Later this might be changed to use jars with URLClassLoader.
