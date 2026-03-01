# minigit
This is a repository for my git clone coded in java with the main git functionality implemented.
It also supports multiple minigit-repository handling.

This project is a home program project for the Java programming course nprg013 at CUNI for the academic year 2025/26.

## Installation

Be warned! If you have a different program called `minigit` installed, it might be overwritten.

 - To install minigit user-wide on linux, run `./install_linux.sh` file.
 - For windows installation, run `./install_windows.bat` file. (TODO)
 - If you do not want to install minigit user-wide, build with `mvn clean package`,
  and then run `java -jar path/to/repo/target/minigit.jar [args]`. You might want to add the `target` directory to your PATH.

## Legal

Educational reimplementation inspired by Git; not affiliated with the Git project.

Licensed under the MIT License.
