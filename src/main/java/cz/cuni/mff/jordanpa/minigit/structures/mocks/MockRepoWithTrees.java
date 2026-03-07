package cz.cuni.mff.jordanpa.minigit.structures.mocks;

import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.MinigitObjectLoader;
import cz.cuni.mff.jordanpa.minigit.structures.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MockRepoWithTrees implements MinigitObjectLoader {
    private Path rootPath;
    private List<Tree> trees = new ArrayList<>();

    public void addTree(Tree tree) {
        trees.add(tree);
    }

    public Tree getTree(String hash) {
        for (Tree tree: trees) {
            if (tree.miniGitSha1().equals(hash)) {
                return tree;
            }
        }
        return null;
    }

    public MockRepoWithTrees(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public MiniGitObject loadFromInternal(String hash) throws IOException {
        return getTree(hash);
    }

    @Override
    public Path getRootPath() {
        return rootPath;
    }

    @Override
    public String getHashFromRef(String ref) {
        throw new UnsupportedOperationException();
    }
}
