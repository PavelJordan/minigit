package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public final class LogCommand implements Command {
    @Override
    public String name() {
        return "log";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Show the commit log";
    }

    @Override
    public String usage() {
        return "minigit log";
    }

    @Override
    public int execute(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            IO.println("Press Enter to continue, type anything to quit.");

            Repository repo = Repository.load(Path.of(".minigit"));
            Head currentHead = repo.getHead();

            if (currentHead.type() == Head.Type.UNSET) {
                IO.println("No commits yet.");
                return 0;
            }

            String startCommitHash = switch (currentHead.type()) {
                case BRANCH -> repo.getBranches().get(currentHead.data());
                case COMMIT -> currentHead.data();
                default -> throw new RuntimeException("Unsupported HEAD type: " + currentHead.type());
            };

            if (startCommitHash == null) {
                IO.println("HEAD does not point to any commit.");
                return 1;
            }

            Set<String> visited = new HashSet<>();
            boolean finished = printCommitGraph(startCommitHash, repo, currentHead, scanner, visited, "");
            return finished ? 0 : 0;

        } catch (IOException e) {
            IO.println(e);
            return 1;
        } catch (ClassCastException e) {
            IO.println("Commit history is corrupted: encountered a non-commit object.");
            return 1;
        }
    }

    /**
     * Prints commit graph reachable from commitHash.
     *
     * @return true if traversal finished normally, false if user chose to quit
     */
    private boolean printCommitGraph(
            String commitHash,
            Repository repo,
            Head currentHead,
            Scanner scanner,
            Set<String> visited,
            String indent
    ) throws IOException {
        // ChatGPT has coded this method - it is simple DFS
        if (commitHash == null || visited.contains(commitHash)) {
            return true;
        }
        visited.add(commitHash);

        MiniGitObject obj = repo.loadFromInternal(commitHash);
        if (!(obj instanceof Commit commit)) {
            throw new ClassCastException("Object " + commitHash + " is not a commit.");
        }

        IO.print(commit.getAnnotatedDescription(repo.getBranches(), repo.getTags(), currentHead, indent));

        String[] parents = commit.getParents();
        if (parents.length > 1) {
            IO.println(indent + "Merge parents:");
            for (String parent : parents) {
                IO.println(indent + "  - " + parent);
            }
        }

        String input = scanner.nextLine();
        if (!input.isEmpty()) {
            return false;
        }

        for (String parent : parents) {
            boolean finished = printCommitGraph(parent, repo, currentHead, scanner, visited, indent + "|");
            if (!finished) {
                return false;
            }
        }

        return true;
    }
}
