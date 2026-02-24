package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Folder, as represented in MiniGit. Contains blobs and more trees.
 */
public final class Tree extends MiniGitObject implements TreeContent {
    /**
     * Map name -> hash of tree content.
     */
    private final HashMap<String, String> contents = new HashMap<>();

    /**
     * Constructor for creating a tree
     */
    Tree(HashMap<String, TreeContent> contents) {
        contents.forEach((name, content) -> this.contents.put(name, content.miniGitSha1()));
    }

    /**
     * Constructor for loading an existing tree.
     */
    private Tree(String[] names, String[] contentHashes) {
        for (int i = 0; i < names.length; i++) {
            this.contents.put(names[i], contentHashes[i]);
        }
    }

    @Override
    public String miniGitSha1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    void write(Path path) throws IOException {

    }
}
