package cz.cuni.mff.jordanpa.minigit.structures.mocks;

import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.MinigitObjectLoader;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of {@link MinigitObjectLoader} for tests working with trees.
 *
 * <p>
 *     This mock stores trees only in runtime memory and allows them to be retrieved by their hash.
 *     It is useful in tests where full repository behavior is not needed.
 * </p>
 */
public final class MockRepoWithTrees implements MinigitObjectLoader {
    private final Path rootPath;
    private final List<Tree> trees = new ArrayList<>();

    /**
     * Create a new mock repository with the specified root path.
     * @param rootPath the root path of the mock repository
     */
    public MockRepoWithTrees(Path rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Add a tree to the mock repository.
     * @param tree the tree to add
     */
    public void addTree(Tree tree) {
        trees.add(tree);
    }

    /**
     * Get a tree with the specified hash.
     * @param hash the hash of the tree
     * @return the tree, or null if it is not found
     */
    public Tree getTree(String hash) {
        for (Tree tree: trees) {
            if (tree.miniGitSha1().equals(hash)) {
                return tree;
            }
        }
        return null;
    }

    /**
     * Load the object from the internal database with the specified hash.
     * @param hash the hash of the object
     * @return the tree with the specified hash, or null if it is not found
     * @throws IOException never meaningfully thrown here, but required by the interface
     */
    @Override
    public MiniGitObject loadFromInternal(String hash) throws IOException {
        return getTree(hash);
    }

    /**
     * Returns the root path of the mock repository.
     */
    @Override
    public Path getRootPath() {
        return rootPath;
    }

    /**
     * This mock does not support references.
     * @param ref the reference to convert (branch name, tag name)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public String getHashFromRef(String ref) {
        throw new UnsupportedOperationException();
    }
}
