package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.FileHelper.getFileStatusesFromComparison;

/**
 * Folder, as represented in MiniGit. Contains blobs and more trees.
 */
public final class Tree extends MiniGitObject implements TreeContent {

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

    private Path safeAbsFromCwdRelative(Path p, MinigitObjectLoader loader) throws IOException {
        Path abs = Path.of("./").toAbsolutePath().resolve(p).normalize();
        if (!abs.startsWith(loader.getRootPath().toAbsolutePath().normalize())) {
            throw new IOException("Path escapes repository: " + p);
        }
        return abs;
    }

    public enum TreeEntryType { BLOB, TREE }

    public record TreeEntry(String hash, TreeEntryType type) {}

    /**
     * Map name -> data + type of tree content.
     */
    private final SortedMap<String, TreeEntry> contents = new TreeMap<>();

    /**
     * Map name -> data + type of tree content.
     */
    Tree(Map<String, TreeEntry> contents) {
        this.contents.putAll(contents);
    }

    String sha1;

    public Map<String, TreeEntry> getContents() {
        return Map.copyOf(contents);
    }

    /**
     * Create a new tree with the specified structure. The files (blobs) and trees must be saved in the database manually!
     * The last tree in the list is the root tree.
     */
    public static LinkedList<Tree> buildTree(Map<Path, String> filesToBuild, Path root) {
        HashMap<Path, String> files = new HashMap<>();
        filesToBuild.forEach((path, hash) -> files.put(FileHelper.getRelativePathToDirectory(path, root), hash));
        return buildTreeInternal(files);
    }

    private static LinkedList<Tree> buildTreeInternal(Map<Path, String> files) {
        HashSet<Path> treesHere = new HashSet<>();
        LinkedList<Tree> trees = new LinkedList<>();
        HashMap<String, TreeEntry> contents = new HashMap<>();
        for (Path file : files.keySet()) {
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

    @Override
    void write(Path path) throws IOException {
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        writeToStream(baOs);
        writeBytes(baOs.toByteArray(), path);
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder("Object:\ntree\nContent:\n");
        for (var content : contents.entrySet()) {
            builder.append(content.getKey()).append(": ").append(content.getValue().type).append(" ").append(content.getValue().hash).append("\n");
        }
        return builder.toString();
    }

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

    public HashMap<Path, String> getIndex(MinigitObjectLoader loader) throws IOException {
        return new HashMap<>(getIndexOfTreeInternal(this, loader.getRootPath(), loader));
    }

    private HashMap<Path, String> getIndexOfTreeInternal(Tree tree, Path pathSoFar, MinigitObjectLoader loader) throws IOException {
        HashMap<Path, String> treeIndex = new HashMap<>();
        for (Map.Entry<String, Tree.TreeEntry> entry : tree.getContents().entrySet()) {
            String name = entry.getKey();
            Tree.TreeEntry value = entry.getValue();
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

    public void showTreeDiff(MinigitObjectLoader loader) throws IOException {
        IO.println("The tree " + miniGitSha1() + " added:");
        getIndex(loader).forEach((path, hash) -> {
            IO.println(path + " (" + hash + ")");
        });
    }

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
}
