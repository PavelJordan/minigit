package cz.cuni.mff.jordanpa.minigit.api;

import cz.cuni.mff.jordanpa.minigit.structures.Head;
import cz.cuni.mff.jordanpa.minigit.structures.MergingCommits;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents status of a repository.
 *
 * @param head The current HEAD (following a branch, detached to a commit, or UNSET).
 * @param headCommitHash The hash of the commit HEAD points to, or null if HEAD is UNSET.
 * @param staged Differences between the index and the last commit - the "staged changes".
 * @param unstaged Differences between the working directory and the index - the "unstaged changes". Files with status NEW are the untracked files.
 * @param merging The merge currently in progress, or null if no merge is in progress.
 */
public record StatusResult(Head head, String headCommitHash, List<Repository.FileStatus> staged, List<Repository.FileStatus> unstaged, MergingCommits merging) {

    /**
     * Whether a merge is currently in progress.
     *
     * @return True if a merge is in progress, false otherwise.
     */
    public boolean isMerging() {
        return merging != null;
    }

    /**
     * Get the untracked files - the NEW files of unstaged changes.
     *
     * @return List of CWD-relative paths of untracked files.
     */
    public List<Path> untracked() {
        return unstaged.stream()
                .filter(s -> s.status() == Repository.FileStatusType.NEW)
                .map(Repository.FileStatus::path)
                .toList();
    }
}
