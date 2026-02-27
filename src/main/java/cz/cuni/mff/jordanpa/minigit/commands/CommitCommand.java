package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class CommitCommand implements Command{
    @Override
    public String name() {
        return "commit";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Create a commit with the current index. Its parent will be HEAD, which will be updated to point to the new commit.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 message.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            Map<Path, String> trackedFiles = repo.getTrackedFiles();
            List<Tree> trees = Tree.buildTree(trackedFiles);
            Commit commit = getCommit(trees, repo, args[0]);
            repo.addCommitAsNewHeadAndStoreInternally(commit);
            trees.forEach(repo::storeInternally);
            repo.save();
            IO.println("Successfully committed. Commit hash is: " + commit.miniGitSha1());
        } catch (IOException e) {
            IO.println(e);
        }
        return 0;
    }

    private static Commit getCommit(List<Tree> trees, Repository repo, String message) throws IOException {
        Tree rootTree = trees.getLast();
        Head currentHead = repo.getHead();

        return switch (currentHead.type()) {
            case COMMIT -> new Commit(rootTree.miniGitSha1(), currentHead.hash(), Author.getDefaultAuthor(), message);
            case UNSET -> new Commit(rootTree.miniGitSha1(), null, Author.getDefaultAuthor(), message);
            case BRANCH -> throw new IOException("Cannot commit on a branch. Not implemented yet.");
        };
    }
}
