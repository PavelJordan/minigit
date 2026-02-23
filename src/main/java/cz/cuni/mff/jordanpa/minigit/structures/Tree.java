package cz.cuni.mff.jordanpa.minigit.structures;

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
    public Tree(HashMap<String, TreeContent> contents) {
        contents.forEach((name, content) -> this.contents.put(name, content.sha1()));
    }

    /**
     * Constructor for loading an existing tree. Used by storage parser.
     */
    Tree(String[] names, String[] contentHashes) {
        for (int i = 0; i < names.length; i++) {
            this.contents.put(names[i], contentHashes[i]);
        }
    }

    @Override
    public String sha1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
