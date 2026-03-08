package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;

public final class ShowCommand implements Command {
    @Override
    public String name() {
        return "show";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Show differences made in the specified commit.";
    }

    @Override
    public String usage() {
        return "minigit show <commit-hash|branch-name|tag-name>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 data or name (branch/tag) of commit.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            MiniGitObject maybeCommit = repo.loadFromInternal(args[0]);
            if (!(maybeCommit instanceof Commit commit)) {
                IO.println("Object with specified data is not a commit.");
                return 1;
            }
            Tree commitTree = (Tree) repo.loadFromInternal(commit.getTreeHash());
            if (commitTree == null) {
                IO.println("Commit tree is not in repository. That means repo is corrupted.");
                return 1;
            }
            String[] parentCommits = commit.getParents();
            if (parentCommits.length > 1) {
                IO.println("Merge commits do not have diffs (not supported yet?).");
                return 1;
            }
            if (parentCommits.length == 0) {
                commitTree.showTreeDiff(repo);
                return 0;
            }
            Commit parent = (Commit) repo.loadFromInternal(commit.getParents()[0]);
            if (parent == null) {
                IO.println("Parent commit is not in repository. That means repo is corrupted.");
                return 1;
            }
            Tree baseTree = (Tree) repo.loadFromInternal(parent.getTreeHash());
            if (baseTree == null) {
                IO.println("Base tree is not in repository. That means repo is corrupted.");
                return 1;
            }
            baseTree.showTreeDiff(commitTree, repo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
