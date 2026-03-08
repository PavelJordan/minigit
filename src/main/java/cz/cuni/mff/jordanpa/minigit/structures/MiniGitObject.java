package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

/**
 * Represents an immutable MiniGit object - blob, tree, or commit.
 *
 * <p>
 *     MiniGit objects are identified by their SHA-1 hash and stored in the internal object database.
 *     Blobs represent file contents, trees represent directory structure, and commits represent snapshots
 *     together with their metadata.
 * </p>
 */
public abstract sealed class MiniGitObject implements Sha1Hashable permits Blob, Tree, Commit {
    protected static final byte[] BLOB_HEADER =   "[[blob]]..".getBytes();
    protected static final byte[] TREE_HEADER =   "[[tree]]..".getBytes();
    protected static final byte[] COMMIT_HEADER = "[[commit]]".getBytes();
    protected static final int HEADER_SIZES = BLOB_HEADER.length;

    /**
     * Load an object from the internal object database based on its hash.
     *
     * <p>
     *     The object type is recognized from its header, and the corresponding concrete
     *     {@link MiniGitObject} subtype is created.
     * </p>
     *
     * @param objectsPath Path to the objects directory.
     * @param hash Hash of the object to load.
     * @return The loaded object, or null if it does not exist.
     * @throws IOException If the object has an unknown header or cannot be read.
     */
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
            return new Tree(contentsBuff.toByteArray(), false);
        }
        if  (Arrays.equals(header, COMMIT_HEADER)) {
            return new Commit(contentsBuff.toByteArray());
        }
        throw new IOException("Unknown object type: {" + new String(header) + "]}.");
    }

    /**
     * Save the specified object into the internal object database based on its hash.
     *
     * @param objectsPath Path to the objects directory.
     * @param obj The object to save.
     * @throws IOException If writing the object fails.
     */
    static void saveObjectBasedOnHash(Path objectsPath, MiniGitObject obj) throws IOException {
        Path objPath = objectsPath.resolve(Path.of(obj.miniGitSha1().substring(0, 2), obj.miniGitSha1().substring(2)));
        obj.write(objPath);
    }

    /**
     * Write this object in its internal MiniGit representation to the specified path.
     *
     * @param path The path to write the object to.
     * @throws IOException If writing fails.
     */
    abstract void write(Path path) throws IOException;

    /**
     * Returns a human-readable description of this object. For blob, it prints out contents, for trees, its contents and their types,
     * and for commits, its message and parent hashes. For commits, though, it is better to use {@link Commit#getAnnotatedDescription(Map, Map, Head, String)}.
     *
     * <p>
     *     This method is intended mainly for inspection and debugging.
     * </p>
     *
     * @return textual description of the {@link MiniGitObject} and its contents
     */
    public abstract String getDescription();

    /**
     * Compute the SHA-1 hash of the specified bytes.
     *
     * @param bytes The bytes to hash.
     * @return The SHA-1 hash as a hexadecimal string.
     */
    protected String getSha1FromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArray2Hex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            IO.println("SHA-1 algorithm not found. This should never happen.");
            return "";
        }
    }

    /**
     * Write the specified bytes to the specified file, unless it already exists.
     *
     * @param bytes The bytes to write.
     * @param path The file path to write to.
     * @throws IOException If writing fails.
     */
    protected void writeBytes(byte[] bytes, Path path) throws IOException{
        if (Files.exists(path)) {
            return;
        }
        IO.println("Object written with data [" + miniGitSha1() + "]");
        Files.createDirectories(path.getParent());
        try(var out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(bytes);
        }
    }
}
