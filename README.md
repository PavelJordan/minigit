# minigit
This is a repository for my git clone coded in java with the main git functionality implemented and a gui in JavaFX.
It also supports multiple minigit-repository handling.

This project is a home program project for the Java programming course nprg013 at CUNI for the academic year 2025/26.

## Documentation

You can find the user manual [here](docs/UserManual.md). The high-level programmer documentation is [here](docs/ProgrammerDocumentation.md).

For more detailed API documentation, you can run `mvn javadoc:javadoc` and open `target/reports/apidocs/index.html`
of the module you are interested in, in your browser.

There is also [this file](docs/DiffImplementation.txt), which describes the implementation of the diff algorithm.

There are also some unit tests in the `cz.cuni.mff.jordanpa.minigit/src/test` directory.

## Project structure

The project is a Maven project with two modules :

 - `cz.cuni.mff.jordanpa.minigit`: The core CLI of MiniGit, which you can use as-is. Use with `minigit`.
 - `cz.cuni.mff.jordanpa.minigit.gui`: the JavaFX graphical interface. Use with `minigit-gui`.

## Installation

Be warned! If you have a different program called `minigit`/`minigit-gui` installed, it might be overwritten.

 - To install MiniGit user-wide on Linux, run `./install_linux.sh` file.
 - To install MiniGit user-wide on Mac, run `./install_mac.sh` file. If `~/.local/bin` is not on your PATH,
  the script will tell you how to add it.
 - If you do not want to install minigit user-wide, build with `mvn clean package`,
  and then run `java -jar cz.cuni.mff.jordanpa.minigit/target/minigit.jar [args]` for the CLI
  or `java -jar cz.cuni.mff.jordanpa.minigit.gui/target/minigit-gui.jar` for the GUI.

The installers install both the `minigit` and the `minigit-gui` command.

## Legal

Educational reimplementation inspired by Git; not affiliated with the Git project.

[Repository](https://github.com/PavelJordan/minigit)

Licensed under the MIT License.
