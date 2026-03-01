package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class StatusCommand implements Command {
    @Override
    public String name() {
        return "status";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Print the status of the current repository";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));

            List<Repository.FileStatus> workToIndex = repo.getWorkingToIndexStatus();
            List<Repository.FileStatus> indexToHead = repo.getStagedToLastCommitStatus();

            // All code below is ChatGPT coded - I just provided the data with the format I came up with.

            boolean hasHead = repo.getHead().type() != Head.Type.UNSET;

            var w = groupByType(workToIndex);
            var h = groupByType(indexToHead);

            int stagedAdded = size(h.get(Repository.FileStatusType.NEW)); // "new in index" vs HEAD
            int stagedModified = size(h.get(Repository.FileStatusType.MODIFIED));
            int stagedDeleted = size(h.get(Repository.FileStatusType.DELETED));

            int unstagedModified = size(w.get(Repository.FileStatusType.MODIFIED));
            int unstagedDeleted = size(w.get(Repository.FileStatusType.DELETED));

            int untracked = size(w.get(Repository.FileStatusType.NEW));

            IO.println("Status");
            IO.println("  HEAD: " + (hasHead ? "set" : "no commits yet"));
            if (hasHead) {
                if (repo.getHead().type() == Head.Type.BRANCH) {
                    IO.println("  Branch: " + repo.getHead().data());
                }
                else {
                    IO.println("  DETACHED at Commit: " + repo.getHead().data());
                }
            }
            IO.println("  Summary: staged " + (stagedAdded + stagedModified + stagedDeleted)
                    + ", not staged " + (unstagedModified + unstagedDeleted)
                    + ", untracked " + untracked);
            IO.println("");

            boolean printedAnything = false;

            // 1) Staged (index -> HEAD)
            String stagedTitle = hasHead ? "Staged changes (will be committed)" : "Staged changes (first commit)";
            printedAnything |= printChangeSection(
                    stagedTitle,
                    h,
                    /*includeAddedFromUntracked=*/true
            );

            // 2) Not staged (working -> index) (excluding UNTRACKED, printed separately)
            printedAnything |= printWorkingSection(
                    "Changes not staged for commit",
                    w
            );

            // 3) Untracked (working -> index)
            printedAnything |= printFlatSection(
                    "Untracked files",
                    "?",
                    w.get(Repository.FileStatusType.NEW)
            );

            if (!printedAnything) {
                IO.println("Working tree clean.");
            }

            return 0;
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
    }

    private static EnumMap<Repository.FileStatusType, List<Path>> groupByType(List<Repository.FileStatus> statuses) {
        var map = new EnumMap<Repository.FileStatusType, List<Path>>(Repository.FileStatusType.class);
        for (var t : Repository.FileStatusType.values()) {
            map.put(t, new ArrayList<>());
        }
        for (var s : statuses) {
            map.get(s.status()).add(s.path());
        }
        return map;
    }

    private static int size(List<Path> paths) {
        return paths == null ? 0 : paths.size();
    }

    private static boolean printChangeSection(
            String title,
            EnumMap<Repository.FileStatusType, List<Path>> grouped,
            boolean includeAddedFromUntracked
    ) {
        List<Path> added = includeAddedFromUntracked ? grouped.get(Repository.FileStatusType.NEW) : List.of();
        List<Path> modified = grouped.get(Repository.FileStatusType.MODIFIED);
        List<Path> deleted = grouped.get(Repository.FileStatusType.DELETED);

        if (size(added) == 0 && size(modified) == 0 && size(deleted) == 0) return false;

        IO.println(title + ":");
        printIndentedGroup("Added", "+", added);
        printIndentedGroup("Modified", "~", modified);
        printIndentedGroup("Deleted", "-", deleted);
        IO.println("");
        return true;
    }

    private static boolean printWorkingSection(
            String title,
            EnumMap<Repository.FileStatusType, List<Path>> grouped
    ) {
        List<Path> modified = grouped.get(Repository.FileStatusType.MODIFIED);
        List<Path> deleted = grouped.get(Repository.FileStatusType.DELETED);

        if (size(modified) == 0 && size(deleted) == 0) return false;

        IO.println(title + ":");
        printIndentedGroup("Modified", "~", modified);
        printIndentedGroup("Deleted", "-", deleted);
        IO.println("");
        return true;
    }

    private static void printIndentedGroup(String groupName, String symbol, List<Path> paths) {
        if (paths == null || paths.isEmpty()) return;

        IO.println("  " + groupName + ":");
        paths.stream()
                .map(Path::toString)
                .sorted()
                .forEach(p -> IO.println("    " + symbol + " " + p));
    }

    private static boolean printFlatSection(String title, String symbol, List<Path> paths) {
        if (paths == null || paths.isEmpty()) return false;

        IO.println(title + ":");
        paths.stream()
                .map(Path::toString)
                .sorted()
                .forEach(p -> IO.println("  " + symbol + " " + p));
        IO.println("");
        return true;
    }
}