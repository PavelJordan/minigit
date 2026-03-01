package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
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
            Author author = repo.loadCurrentAuthor();
            if (author == null) {
                IO.println("No author set. Use 'minigit author <name> <email>' to set one.");
                return 1;
            }
            List<Repository.FileStatus> stagedToLastCommitStatus = repo.getStagedToLastCommitStatus();
            if (stagedToLastCommitStatus.stream().allMatch(status -> status.status() == Repository.FileStatusType.SAME)) {
                IO.println("There is nothing to commit.");
                return 0;
            }
            Map<Path, String> trackedFiles = repo.getTrackedFiles();
            List<Tree> trees = Tree.buildTree(trackedFiles);
            Commit commit = getCommit(trees, repo, args[0], author);
            repo.setCommitAsNewHeadAndStoreInternally(commit);
            trees.forEach(repo::storeInternally);
            repo.save();
            IO.println("Successfully committed. Commit data is: " + commit.miniGitSha1());
        } catch (IOException e) {
            IO.println(e);
        }
        return 0;
    }

    private static Commit getCommit(List<Tree> trees, Repository repo, String message, Author author) throws IOException {
        Tree rootTree = trees.getLast();
        Head currentHead = repo.getHead();

        return switch (currentHead.type()) {
            case COMMIT -> new Commit(rootTree.miniGitSha1(), currentHead.data(), author, message, Date.from(Instant.now()));
            case UNSET -> new Commit(rootTree.miniGitSha1(), null, author, message, Date.from(Instant.now()));
            case BRANCH -> throw new IOException("Cannot commit on a branch. Not implemented yet.");
        };
    }
}
