package cz.cuni.mff.jordanpa.minigit.structures;

/**
 * Represents a state where we are merging two commits.
 *
 * <p>
 *     We also want to remember the head we are merging into, so we can warn the user
 *     if they move the head and then try to merge. (It is still possible to merge, but we still want
 *     to warn them.) Also, we want to know the head so we can move the branch it points to (if it is not detached).
 * </p>
 * @param fromCommit The commit we want to merge the changes from
 * @param intoHead The head we are merging into (branch, if we want to also move the branch)
 * @param intoCommit The commit we are merging into
 */
public record MergingCommits(String fromCommit, Head intoHead, String intoCommit) { }
