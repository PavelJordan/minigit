package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Folder, as represented in MiniGit. Contains blobs and more trees.
 */
public final class Tree extends MiniGitObject implements TreeContent {

    public enum TreeEntryType { BLOB, TREE }

    public record TreeEntry(String hash, TreeEntryType type) {}

    /**
     * Map name -> hash + type of tree content.
     */
    private final HashMap<String, TreeEntry> contents = new HashMap<>();

    @Override
    public String miniGitSha1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    void write(Path path) throws IOException {

    }

    @Override
    public String getDescription() {
        return "";
    }
}
