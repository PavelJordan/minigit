package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Folder, as represented in MiniGit. Contains blobs and more trees.
 */
public final class Tree extends MiniGitObject implements TreeContent {

    /**
     * Build a tree from its contents in a byte array
     */
    Tree(byte[] byteArray) {
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

    public enum TreeEntryType { BLOB, TREE }

    public record TreeEntry(String hash, TreeEntryType type) {}

    /**
     * Map name -> hash + type of tree content.
     */
    private final HashMap<String, TreeEntry> contents = new HashMap<>();

    /**
     * Map name -> hash + type of tree content.
     */
    Tree(Map<String, TreeEntry> contents) {
        this.contents.putAll(contents);
    }

    String sha1;

    /**
     * Create a new tree with the specified structure. The files (blobs) and trees must be saved in the database manually!
     * The last tree in the list is the root tree.
     */
    public static LinkedList<Tree> buildTree(Map<Path, String> files) {
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
            IO.println("Descending into: " + dir);
            HashMap<Path, String> filesInDir = new HashMap<>();
            files.entrySet().stream().filter(entry -> entry.getKey().startsWith(dir)).forEach(entry -> filesInDir.put(entry.getKey().subpath(1, entry.getKey().getNameCount()), entry.getValue()));
            LinkedList<Tree> lowerTrees = buildTree(filesInDir);
            if (lowerTrees.isEmpty()) {
                IO.println("Building failed here");
                continue;
            }
            contents.put(dir.getFileName().toString(), new TreeEntry(lowerTrees.getLast().miniGitSha1(), TreeEntryType.TREE));
            trees.addAll(lowerTrees);
            IO.println("Ascending from: " + dir);
        }
        Tree root = new Tree(contents);
        trees.add(root);
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
        IO.println("Tree written with hash [" + miniGitSha1() + "] to " + path);
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
}
