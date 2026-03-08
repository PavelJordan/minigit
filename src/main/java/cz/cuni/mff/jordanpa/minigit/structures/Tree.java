package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.FileHelper.getFileStatusesFromComparison;

/**
 * Folder, as represented in MiniGit. Contains blobs and more trees.
 *
 * <p>
 *     Similar as blobs, it does not know its own name and only
 *     remembers the hashes to the contents.
 * </p>
 * <p>
 *     Again, refer to the `docs/ProgrammerDocumentation.md` to understand why it is made this way.
 * </p>
 */
public final class Tree extends MiniGitObject implements TreeContent {

    /**
     * Type of tree entry.
     */
    public enum TreeEntryType {
        /**
         * A file in the repository.
         */
        BLOB,
        /**
         * A folder in the repository.
         */
        TREE
    }

    /**
     * Represents one tree entry
     * @param hash The hash of the object this entry refers to.
     * @param type The type of the entry.
     */

    public record TreeEntry(String hash, TreeEntryType type) {}

    /**
     * Map name -> hash + type of tree content.
     */
    private final SortedMap<String, TreeEntry> contents = new TreeMap<>();

    String sha1;

    /**
     * Build a tree from its contents in a byte array
     */
    Tree(byte[] byteArray, boolean isWithHeader) {
        if (isWithHeader) {
            byteArray = Arrays.copyOfRange(byteArray, HEADER_SIZES, byteArray.length);
        }
        try(Scanner scanner = new Scanner(new ByteArrayInputStream(byteArray))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                int delimiterIndex = line.indexOf(' ');
                String hash = line.substring(0, delimiterIndex);
                String rest = line.substring(delimiterIndex + 1);
                int typeIndex = rest.indexOf(' ');
                String typeStr = rest.substring(0, typeIndex);
                String name = rest.substring(typeIndex + 1);
                TreeEntryType type = TreeEntryType.valueOf(typeStr);
                contents.put(name, new TreeEntry(hash, type));
            }
        }
    }

    /**
     * Create a tree from its already prepared contents.
     *
     * @param contents Map name -> data + type of tree content.
     */
    Tree(Map<String, TreeEntry> contents) {
        this.contents.putAll(contents);
    }

    /**
     * Retrieve the contents of this tree.
     *
     * @return A copy of the map "name -> tree entry".
     */
    public Map<String, TreeEntry> getContents() {
        return Map.copyOf(contents);
    }

    /**
     * Create a new tree with the specified structure.
     *
     * <p>
     *     The files (blobs) and trees must be saved in the database manually;
     *     otherwise the saved trees will point to non-existing objects, after saving to the file system.
     * </p>
     *
     * @param filesToBuild The files to build the tree from.
     * @param root The root directory. All paths, as will be saved into the filesystem, will be relative to this directory.
     *             You want this to be the root of the repository where the tree is built,
     *             so the root-tree will have the files and directories in the root directory.
     *
     * @return All the trees in the tree hierarchy, the last one being the root tree.
     */
    public static LinkedList<Tree> buildTree(Map<Path, String> filesToBuild, Path root) {
        HashMap<Path, String> files = new HashMap<>();
        filesToBuild.forEach((path, hash) -> files.put(FileHelper.getRelativePathToDirectory(path, root), hash));
        return buildTreeInternal(files);
    }

    /**
     * Recursively build a hierarchy of trees from repository-root-relative paths.
     *
     * @param files Files to build the tree from.
     * @return All the trees in the tree hierarchy, the last one being the current root tree.
     */
    private static LinkedList<Tree> buildTreeInternal(Map<Path, String> files) {
        HashSet<Path> treesHere = new HashSet<>();
        LinkedList<Tree> trees = new LinkedList<>();
        HashMap<String, TreeEntry> contents = new HashMap<>();

        for (Path file : files.keySet()) {
            // Files directly in this directory become blob entries, deeper paths are grouped by their first directory.
            if (file.getParent() == null) {
                IO.println("Blob found: " + file);
                contents.put(file.getFileName().toString(), new TreeEntry(files.get(file), TreeEntryType.BLOB));
            }
            else  {
                Path dir = file.subpath(0, 1);
                treesHere.add(dir);
            }
        }

        for (Path dir : treesHere) {
            HashMap<Path, String> filesInDir = new HashMap<>();
            files.entrySet().stream().filter(entry -> entry.getKey().startsWith(dir)).forEach(entry -> filesInDir.put(entry.getKey().subpath(1, entry.getKey().getNameCount()), entry.getValue()));

            // Recursively build the subtree and then reference its root from the current tree.
            LinkedList<Tree> lowerTrees = buildTreeInternal(filesInDir);
            if (lowerTrees.isEmpty()) {
                IO.println("Building failed here");
                continue;
            }
            contents.put(dir.getFileName().toString(), new TreeEntry(lowerTrees.getLast().miniGitSha1(), TreeEntryType.TREE));
            trees.addAll(lowerTrees);
        }

        Tree treeRoot = new Tree(contents);
        trees.add(treeRoot);
        return trees;
    }

    /**
     * @return SHA-1 hash of this tree.
     */
    @Override
    public String miniGitSha1() {
        if (sha1 != null) {
            return sha1;
        }
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        try {
            writeToStream(baOs);
        } catch (IOException e) {
            IO.println(e);
            IO.println("Error writing tree to stream for SHA-1 calculation.");
            return "";
        }
        sha1 = getSha1FromBytes(baOs.toByteArray());
        return sha1;
    }

    /**
     * Write this tree in its internal MiniGit object format to the given path.
     *
     * @param path Destination path inside MiniGit object storage.
     * @throws IOException If writing fails.
     */
    @Override
    void write(Path path) throws IOException {
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        writeToStream(baOs);
        writeBytes(baOs.toByteArray(), path);
    }

    /**
     * @return A human-readable description of this tree.
     */
    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder("Object:\ntree\nContent:\n");
        for (var content : contents.entrySet()) {
            builder.append(content.getKey()).append(": ").append(content.getValue().type).append(" ").append(content.getValue().hash).append("\n");
        }
        return builder.toString();
    }

    /**
     * Get the index represented by this tree.
     *
     * @param loader The loader to use to load subtrees.
     * @return Map "path -> blob hash" represented by this tree.
     * @throws IOException If loading subtrees fails.
     */
    public HashMap<Path, String> getIndex(MinigitObjectLoader loader) throws IOException {
        return new HashMap<>(getIndexOfTreeInternal(this, loader.getRootPath(), loader));
    }

    /**
     * Recursively build the index of the specified tree.
     *
     * @param tree The tree to build the index of.
     * @param pathSoFar The absolute path of the current tree root in the repository.
     * @param loader The loader to use to load subtrees.
     * @return Map "path -> blob hash" represented by this tree.
     * @throws IOException If loading subtrees fails.
     */
    private HashMap<Path, String> getIndexOfTreeInternal(Tree tree, Path pathSoFar, MinigitObjectLoader loader) throws IOException {
        HashMap<Path, String> treeIndex = new HashMap<>();
        for (Map.Entry<String, Tree.TreeEntry> entry : tree.getContents().entrySet()) {
            String name = entry.getKey();
            Tree.TreeEntry value = entry.getValue();

            // Trees are expanded recursively, blobs are added directly to the resulting index.
            if (value.type() == Tree.TreeEntryType.TREE) {
                MiniGitObject possiblySubtreeToRestore = loader.loadFromInternal(value.hash());
                if (possiblySubtreeToRestore instanceof Tree subtreeToRestore) {
                    treeIndex.putAll(getIndexOfTreeInternal(subtreeToRestore, pathSoFar.resolve(name), loader));
                } else {
                    IO.println("Object with specified data is not a tree, even though it should. Repository is corrupted.");
                }
            } else if (value.type() == Tree.TreeEntryType.BLOB) {
                Path resolvedPath = FileHelper.getRelativePathToDirectory(pathSoFar.resolve(name), Path.of("./"));
                treeIndex.put(resolvedPath, value.hash());

            }
        }
        return treeIndex;
    }

    /**
     * Show the difference between this tree and another tree.
     *
     * @param treeToCompare The tree to compare this tree to.
     * @param loader The loader to use to load the tree contents.
     * @throws IOException If building indices of the trees fails.
     */
    public void showTreeDiff(Tree treeToCompare, MinigitObjectLoader loader) throws IOException{
        IO.println("The tree " + treeToCompare.miniGitSha1() + " has changed from " + miniGitSha1() + " in the following way:");
        HashMap<Path, String> baseTreeIndex = getIndex(loader);
        HashMap<Path, String> treeToCompareIndex = treeToCompare.getIndex(loader);
        getFileStatusesFromComparison(treeToCompareIndex, baseTreeIndex).forEach(status -> {
            if (status.status() != Repository.FileStatusType.SAME) {
                IO.println(status.path() + " (" + status.status() + ")");
            }
        });
    }

    /**
     * Show all files contained in this tree.
     *
     * @param loader The loader to use to load the tree contents.
     * @throws IOException If building the tree index fails.
     */
    public void showTreeDiff(MinigitObjectLoader loader) throws IOException {
        IO.println("The tree " + miniGitSha1() + " added:");
        getIndex(loader).forEach((path, hash) -> {
            IO.println(path + " (" + hash + ")");
        });
    }

    /**
     * Puts the contents of this tree into the current working directory (uses {@link MinigitObjectLoader#getRootPath()} as the root of this tree).
     * @param loader The loader to use to load the blobs and subtrees.
     * @throws IOException If the tree cannot be restored (some files cannot be written, or the repository is corrupted.
     * For example, object headers are not recognized)
     */
    public void forcedCheckoutTree(MinigitObjectLoader loader) throws IOException {
        for (HashMap.Entry<Path, String> entry : getIndex(loader).entrySet()) {
            MiniGitObject possibleBlobToRestore = loader.loadFromInternal(entry.getValue());
            if (possibleBlobToRestore instanceof Blob blobToRestore) {
                Path absTarget = safeAbsFromCwdRelative(entry.getKey(), loader);
                if (Files.exists(absTarget)) {
                    IO.println("File " + absTarget + " already exists. Cannot overwrite!");
                    return;
                }
                IO.println("Restoring file " + absTarget + "...");
                try {
                    blobToRestore.writeContentsTo(absTarget);
                } catch (IOException e) {
                    IO.println(e);
                }
            }
            else {
                IO.println("Object with specified data is not a blob, even though it should. Repository is corrupted.");
            }
        }
    }

    /**
     * Write the serialized tree representation to an output stream.
     *
     * @param out The output stream to write to.
     * @throws IOException If writing to the stream fails.
     */
    private void writeToStream(OutputStream out) throws IOException {
        out.write(TREE_HEADER);
        for (var entry : contents.entrySet()) {
            out.write(entry.getValue().hash.getBytes());
            out.write(' ');
            out.write(entry.getValue().type.name().getBytes());
            out.write(' ');
            out.write(entry.getKey().getBytes());
            out.write('\n');
        }
    }

    /**
     * Resolve a CWD-relative path to an absolute path and ensure it stays inside the repository.
     *
     * @param p The CWD-relative path.
     * @param loader The loader that provides the repository root path.
     * @return The absolute normalized path.
     * @throws IOException If the path escapes the repository.
     */
    private Path safeAbsFromCwdRelative(Path p, MinigitObjectLoader loader) throws IOException {
        Path abs = Path.of("./").toAbsolutePath().resolve(p).normalize();
        if (!abs.startsWith(loader.getRootPath().toAbsolutePath().normalize())) {
            throw new IOException("Path escapes repository: " + p);
        }
        return abs;
    }
}
