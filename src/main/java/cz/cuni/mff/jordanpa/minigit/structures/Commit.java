package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;

import java.util.Arrays;

/**
 * Represents a frozen state of a repository the user can refer to.
 * Can have multiple child commits == branching, or multiple parent commits == merge commit.
 * However, it does not have to remember the children, only the parents (the previous commits).
 * This is an immutable structure!
 */
public final class Commit extends MiniGitObject {
    /**
     * Hash of the tree object this commit refers to.
     */
    private final String Snapshot;
    /**
     * Hashes of the parent commits
     */
    private final String[] parents;
    private final Author author;
    private final String message;

    /**
     * Constructor for creating a new commit
     */
    public Commit(Tree snapshot, Commit[] parents, Author author, String message) {
        this.Snapshot = snapshot.sha1();
        this.parents = Arrays.stream(parents).map(Commit::sha1).toArray(String[]::new);
        this.author = author;
        this.message = message;
    }

    /**
     * Constructor for loading an existing commit. Used by storage parser.
     */
    Commit(String snapshot, String[] parents, Author author, String message) {
        this.Snapshot = snapshot;
        this.parents = parents.clone();
        this.author = author;
        this.message = message;
    }

    @Override
    public String sha1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
