package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

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
    public int execute(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            IO.println("Enter with some text to quit.");
            Repository repo = Repository.load(Path.of(".minigit"));
            Head currentHead = repo.getHead();
            if (currentHead.type() == Head.Type.UNSET) {
                IO.println("No commits yet.");
                return 0;
            }
            String currentCommitHash = switch (currentHead.type()) {
                case BRANCH -> repo.getBranches().get(currentHead.data());
                case COMMIT -> currentHead.data();
                case null, default -> throw new RuntimeException("Head type is not supported.");
            };
            Map<String, String> refs = repo.getAllRefs();
            while (currentCommitHash != null) {
                Commit commit = (Commit) repo.loadFromInternal(currentCommitHash);
                assert commit != null;
                IO.println(commit.getAnnotatedDescription(refs, currentHead));
                String[] parents = commit.getParents();
                if (parents.length == 1) {
                    currentCommitHash = parents[0];
                }
                else if (parents.length == 0) {
                    currentCommitHash = null;
                }
                else {
                    IO.println("Commit chain is branched - cannot handle that yet");
                    return 0;
                }
                String input = scanner.nextLine();
                if (!input.isEmpty()) {
                    return 0;
                }
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        catch (ClassCastException e) {
            IO.println("Commit chain is not a chain of commits.");
            return 1;
        }
        return 0;
    }
}
