package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.MergingCommits;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class MergeCommand implements Command {
    @Override
    public String name() {
        return "merge";
    }

    @Override
    public String shortHelp() {
        return "Merge changes from another commit";
    }

    @Override
    public String help() {
        return "Merge the specified branch/commit into the current branch/HEAD. If merging fails, you can then update index to your liking and use merge-apply";
    }

    @Override
    public String usage() {
        return "minigit merge <branch-name|commit-hash>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 1) {
            IO.println("Incorrect number of arguments. Provide exactly 1 branch name/commit hash/tag name.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            if (repo.workingTreeDirty()) {
                IO.println("Cannot merge - working tree is not clean.");
                return 1;
            }
            var mergeFrom = repo.loadFromInternal(args[0]);
            var mergeHEAD = repo.getHead();
            if (mergeHEAD.type() == Head.Type.UNSET) {
                IO.println("Cannot merge yet. No HEAD.");
                return 1;
            }
            var mergeInto = repo.loadFromInternal(mergeHEAD.data());
            if (!(mergeFrom instanceof Commit mergeFromCommit) || !(mergeInto instanceof Commit mergeIntoCommit)) {
                IO.println("Cannot merge - from/to");
                return 1;
            }
            if (repo.isMerging()) {
                IO.println("Cannot merge - already merging");
                return 1;
            }
            Repository.MergeStatus mergeStatus = repo.startMerging(new MergingCommits(mergeFromCommit.miniGitSha1(), mergeHEAD, mergeIntoCommit.miniGitSha1()));
            if (mergeStatus == Repository.MergeStatus.CONFLICT) {
                IO.println("Cannot merge - resolve conflicts, stage the changes and create merge commit via merge-apply");
                repo.save();
                return 2;
            }
            else if (mergeStatus == Repository.MergeStatus.APPLIED) {
                IO.println("Merge successful.");
                if (repo.isMerging()){
                    IO.println(" Use merge-apply with message to apply the merge, stage changes before applying to change the merge, or merge-stop to stop merging.");
                }
                repo.save();
                return 0;
            }
            else if (mergeStatus == Repository.MergeStatus.INVALID) {
                IO.println("This merge is impossible.");
                return 1;
            }
        }
        catch (IOException ex) {
            IO.println(ex);
            return 1;
        }
        return 3;
    }
}
