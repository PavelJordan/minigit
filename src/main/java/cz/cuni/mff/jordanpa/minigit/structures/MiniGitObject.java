package cz.cuni.mff.jordanpa.minigit.structures;

/**
 * Represents a MiniGit object - blob, tree, or commit.
 */
public abstract sealed class MiniGitObject implements Sha1Hashable permits Blob, Tree, Commit {
}
