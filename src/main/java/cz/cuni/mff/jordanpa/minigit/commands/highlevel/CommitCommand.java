package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.misc.ProjectManager;
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

public final class CommitCommand implements Command {
    private static final String FIRST_BRANCH_NAME = "master";
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
        return "Create a commit with the current index in this repo (or multiple with PM). Its parent will be HEAD, which will be updated to point to the new commit.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 message.");
            return 1;
        }
        try {
            List<Repository> repos = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : repos) {
                IO.println("Committing " + repo.getRootPath() + " ...");
                Author author = repo.getCurrentAuthor();
                if (author == null) {
                    IO.println("No author set. Use 'minigit author <name> <email>' to set one.");
                    continue;
                }
                List<Repository.FileStatus> stagedToLastCommitStatus = repo.getStagedToLastCommitStatus();
                if (stagedToLastCommitStatus.stream().allMatch(status -> status.status() == Repository.FileStatusType.SAME)) {
                    IO.println("There is nothing to commit.");
                    continue;
                }
                Map<Path, String> trackedFiles = repo.getTrackedFiles();
                List<Tree> trees = Tree.buildTree(trackedFiles, repo.getRootPath());
                Commit commit = getCommit(trees, repo, args[0], author);
                repo.storeInternally(commit);
                trees.forEach(repo::storeInternally);
                if (repo.getHead().type() == Head.Type.BRANCH) {
                    IO.println("Committing to branch " + repo.getHead().data());
                    repo.setBranch(repo.getHead().data(), commit.miniGitSha1());
                    repo.setHeadToBranch(repo.getHead().data());
                }
                else if (repo.getHead().type() == Head.Type.UNSET) {
                    IO.println("No HEAD yet. Creating master branch.");
                    repo.setBranch(FIRST_BRANCH_NAME, commit.miniGitSha1());
                    repo.setHeadToBranch(FIRST_BRANCH_NAME);
                }
                else {
                    IO.println("Committing to detached state");
                    repo.setHeadToCommit(commit);
                }
                repo.save();
                IO.println("Successfully committed " + repo.getRootPath() + ". Commit data is: " + commit.miniGitSha1());
            }
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
            case BRANCH -> new Commit(rootTree.miniGitSha1(), repo.getBranches().get(currentHead.data()), author, message, Date.from(Instant.now()));
        };
    }
}
