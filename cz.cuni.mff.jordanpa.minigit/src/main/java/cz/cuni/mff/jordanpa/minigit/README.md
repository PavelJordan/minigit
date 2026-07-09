# MiniGit java source

Here are the java source files of MiniGit.

- The `MiniGit.java` is the main class.
- The `commands` package contains the commands as plugins, divided into highlevel and lowlevel. They all need to implement the `Command` interface.
- The `misc` package contains miscellaneous classes - `FileHelper`, `Merger`, `MiniGitDiff`, `ProjectManager`, `CommonAncestorFinder` and `Author`.
- The `structures` package contains the data structures used by MiniGit, together with their business logic.
