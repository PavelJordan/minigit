# minigit
This is a repository for my git clone coded in java with the main git functionality implemented.
It also supports multiple minigit-repository handling.

This project is a home program project for the Java programming course nprg013 at CUNI for the academic year 2025/26.

## Documentation

You can find the user manual [here](docs/UserManual.md). The high-level programmer documentation is [here](docs/ProgrammerDocumentation.md).

For more detailed API documentation, you can run `mvn javadoc:javadoc` and open `target/reports/apidocs/index.html` in your browser.

There is also [this file](docs/DiffImplementation.txt), which describes the implementation of the diff algorithm.

There are also some unit tests in the `src/test` directory.

## Installation

Be warned! If you have a different program called `minigit` installed, it might be overwritten.

 - To install minigit user-wide on linux, run `./install_linux.sh` file.
 - For windows installation, run `./install_windows.bat` file. (TODO)
 - If you do not want to install minigit user-wide, build with `mvn clean package`,
  and then run `java -jar path/to/repo/target/minigit.jar [args]`. You might want to add the `target` directory to your PATH.

## Legal

Educational reimplementation inspired by Git; not affiliated with the Git project.

[Repository](https://github.com/PavelJordan/minigit)

Licensed under the MIT License.
