package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.structures.mocks.MockRepoWithTrees;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface mockIndex {
    HashMap<Path, String> getMockIndex(Path dir);
}

class emptyIndex implements mockIndex {
    public HashMap<Path, String> getMockIndex(Path dir) {
        return new HashMap<>();
    }
}

class oneLevelIndex implements mockIndex {
    public HashMap<Path, String> getMockIndex(Path dir) {
        HashMap<Path, String> index = new HashMap<>();
        index.put(dir.resolve("README.md"),  "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        index.put(dir.resolve("Main.java"),  "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3");
        return index;
    }
}

class twoLevelIndex implements mockIndex {
    public HashMap<Path, String> getMockIndex(Path dir) {
        HashMap<Path, String> index = new HashMap<>();
        index.put(dir.resolve("src/Foo.java"),       "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4");
        index.put(dir.resolve("src/Bar.java"),       "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5");
        index.put(dir.resolve("src/Baz.java"),       "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6");
        index.put(dir.resolve("README.md"),          "f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1");
        return index;
    }
}

class threeLevelIndex implements mockIndex {
    public HashMap<Path, String> getMockIndex(Path dir) {
        HashMap<Path, String> index = new HashMap<>();
        index.put(dir.resolve("src/main/App.java"),          "1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b");
        index.put(dir.resolve("src/main/Helper.java"),       "2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c");
        index.put(dir.resolve("src/test/AppTest.java"),      "3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d");
        index.put(dir.resolve("resources/config.properties"),"4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e");
        index.put(dir.resolve("README.md"),                  "5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f");
        return index;
    }
}

@ParameterizedClass
@ValueSource(classes = {emptyIndex.class, oneLevelIndex.class, twoLevelIndex.class, threeLevelIndex.class})
public class TreeTest {

    @Parameter
    Class<? extends mockIndex> mockIndexClass;

    mockIndex mockIndex;

    @TempDir
    Path tempDir;
    Path scratchPath;


    private MockRepoWithTrees treeObjLoader;

    @BeforeEach
    void init() {
        treeObjLoader = new MockRepoWithTrees(tempDir);
        scratchPath = tempDir.resolve("scratch");
        try {
            mockIndex = mockIndexClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            fail("Could not instantiate mock index: " + e);
        }
    }

    @Test
    void contentMatchesAfterBuildingFromIndex() {
        Map<Path, String> index = mockIndex.getMockIndex(tempDir);
        List<Tree> trees = Tree.buildTree(index, tempDir);
        Tree rootTree = trees.getLast();
        Map <String, Tree.TreeEntry> content = rootTree.getContents();
        for (Map.Entry<String, Tree.TreeEntry> entry : content.entrySet()) {
            Path path = tempDir.resolve(entry.getKey());
            if (isDirectoryInIndex(path, index)) {
                assertEquals(Tree.TreeEntryType.TREE, entry.getValue().type());
            }
            else if (!isDirectoryInIndex(path, index)) {
                assertEquals(Tree.TreeEntryType.BLOB, entry.getValue().type());
            }
        }
    }

    @Test
    void contentMatchesAfterBuildingFromIndexAndReloadingInFileSystem() {
        Map<Path, String> index = mockIndex.getMockIndex(tempDir);
        List<Tree> trees = Tree.buildTree(index, tempDir);
        Tree rootTree = trees.getLast();
        Tree loadedTree;
        try {
            rootTree.write(tempDir.resolve(rootTree.miniGitSha1()));
            loadedTree = new Tree(Files.readAllBytes(tempDir.resolve(rootTree.miniGitSha1())), true);
        } catch (IOException e) {
            fail("Could not write/load tree");
            return;
        }
        Map <String, Tree.TreeEntry> content = loadedTree.getContents();
        for (Map.Entry<String, Tree.TreeEntry> entry : content.entrySet()) {
            Path path = tempDir.resolve(entry.getKey());
            if (isDirectoryInIndex(path, index)) {
                assertEquals(Tree.TreeEntryType.TREE, entry.getValue().type());
            }
            else if (!isDirectoryInIndex(path, index)) {
                assertEquals(Tree.TreeEntryType.BLOB, entry.getValue().type());
            }
        }
    }

    private boolean isDirectoryInIndex(Path path, Map<Path, String> index) {
        for (Path filesPath : index.keySet()) {
            if (filesPath.equals(path)) {
                return false;
            }
            if (filesPath.startsWith(path)) {
                return true;
            }
        }
        fail("Path " + path + " not seen in index");
        throw new RuntimeException("Should not be here.");
    }

    @Test
    void indexMatchesAfterBuildingFromIndex() {
        Map<Path, String> index = mockIndex.getMockIndex(tempDir);
        List<Tree> trees = Tree.buildTree(index, tempDir);
        Tree rootTree = trees.getLast();
        trees.forEach(treeObjLoader::addTree);
        try {
            internalIndexMatches(index, rootTree);
        }
        catch (IOException e) {
            fail("Could not build index");
        }
    }

    @Test
    void indexMatchesAfterBuildingFromIndexAndSavingIntoFileSystem() {
        Map<Path, String> index = mockIndex.getMockIndex(tempDir);
        List<Tree> trees = Tree.buildTree(index, tempDir);
        List<Tree> loadedTrees = new ArrayList<>();
        for (Tree tree : trees) {
            try {
                tree.write(tempDir.resolve(Path.of(tree.miniGitSha1())));
                Tree loadedTree = new Tree(Files.readAllBytes(tempDir.resolve(Path.of(tree.miniGitSha1()))), true);
                loadedTrees.add(loadedTree);
            }
            catch (IOException e) {
                fail("Could not write tree");
            }
        }
        loadedTrees.forEach(treeObjLoader::addTree);
        try {
            internalIndexMatches(index, loadedTrees.getLast());
        }
        catch (IOException e) {
            fail("Could not build index");
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {emptyIndex.class, oneLevelIndex.class, twoLevelIndex.class, threeLevelIndex.class})
    void shaMatchesOnlyIfTreesTheSameConstructedFromIndex(Class<? extends mockIndex> otherMockIndexClass) {
        Map<Path, String> firstIndex = mockIndex.getMockIndex(tempDir);
        try {
            Map<Path, String> secondIndex = otherMockIndexClass.getDeclaredConstructor().newInstance().getMockIndex(tempDir);
            Tree rootTree1 = Tree.buildTree(firstIndex, tempDir).getLast();
            Tree rootTree2 = Tree.buildTree(secondIndex, tempDir).getLast();
            if (firstIndex.equals(secondIndex)) {
                assertEquals(rootTree1.miniGitSha1(), rootTree2.miniGitSha1());
            }
            else {
                assertNotEquals(rootTree1.miniGitSha1(), rootTree2.miniGitSha1());
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            fail("Could not instantiate mock index: " + e);
        }
    }

    void internalIndexMatches(Map<Path, String> index, Tree tree) throws IOException {
        Map<Path, String> retrievedIndex = tree.getIndex(treeObjLoader);
        for (Map.Entry<Path, String> entry : retrievedIndex.entrySet()) {
            Path path = entry.getKey().toAbsolutePath().normalize();
            String hash = entry.getValue();
            assertEquals(index.get(path), hash);
        }
    }
}
