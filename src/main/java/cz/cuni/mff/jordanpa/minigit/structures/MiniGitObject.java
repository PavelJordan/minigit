package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Represents a MiniGit object - blob, tree, or commit.
 */
public abstract sealed class MiniGitObject implements Sha1Hashable permits Blob, Tree, Commit {
    protected static final byte[] BLOB_HEADER =   "[[blob]]..".getBytes();
    protected static final byte[] TREE_HEADER =   "[[tree]]..".getBytes();
    protected static final byte[] COMMIT_HEADER = "[[commit]]".getBytes();
    protected static final int HEADER_SIZES = BLOB_HEADER.length;

    public static MiniGitObject getObjectBasedOnHash(Path objectsPath, String hash) throws IOException {
        Path objPath = objectsPath.resolve(Path.of(hash.substring(0, 2), hash.substring(2)));
        if (!Files.exists(objPath)) {
            return null;
        }
        byte[] header;
        ByteArrayOutputStream contentsBuff = new ByteArrayOutputStream();
        try (var in = new BufferedInputStream(Files.newInputStream(objPath))) {
            header = in.readNBytes(HEADER_SIZES);
            in.transferTo(contentsBuff);
        }
        if (Arrays.equals(header, BLOB_HEADER)) {
            return new Blob(contentsBuff.toByteArray());
        }
        if (Arrays.equals(header, TREE_HEADER)) {
            return new Tree(contentsBuff.toByteArray());
        }
        if  (Arrays.equals(header, COMMIT_HEADER)) {
            return new Commit(contentsBuff.toByteArray());
        }
        throw new IOException("Unknown object type: {" + new String(header) + "]}.");
    }

    static void saveObjectBasedOnHash(Path objectsPath, String hash, MiniGitObject obj) throws IOException {
        Path objPath = objectsPath.resolve(Path.of(hash.substring(0, 2), hash.substring(2)));
        obj.write(objPath);
    }

    abstract void write(Path path) throws IOException;
    public abstract String getDescription();

    protected String getSha1FromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArray2Hex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            IO.println("SHA-1 algorithm not found. This should never happen.");
            return "";
        }
    }

    protected void writeBytes(byte[] bytes, Path path) throws IOException{
        if (Files.exists(path)) {
            return;
        }
        IO.println("Object written with hash [" + miniGitSha1() + "]");
        Files.createDirectories(path.getParent());
        try(var out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(bytes);
        }
    }
}
