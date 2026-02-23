package cz.cuni.mff.jordanpa.minigit.structures;

/**
 * File content, as represented in MiniGit.
 */
public final class Blob extends MiniGitObject implements TreeContent {
    private final byte[] content;

    public Blob(byte[] content) {
        this.content = content;
    }

    @Override
    public String sha1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
