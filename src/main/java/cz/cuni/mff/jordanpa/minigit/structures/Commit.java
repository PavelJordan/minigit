package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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

    public Commit(String snapshot, String[] parents, Author author, String message) {
        Snapshot = snapshot;
        this.parents = parents;
        this.author = author;
        this.message = message;
    }

    @Override
    void write(Path path) throws IOException {

    }

    @Override
    public String getDescription() {
        return "";
    }

    /**
     * Returns the hash of this object.
     * Should be calculated only once, for example, when the object is first created.
     */
    @Override
    public String miniGitSha1() {
        throw new UnsupportedOperationException();
    }
}
