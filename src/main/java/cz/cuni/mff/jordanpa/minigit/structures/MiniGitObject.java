package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        ByteArrayOutputStream contentsBuff = new ByteArrayOutputStream();
        byte[] header;
        try (var in = new BufferedInputStream(Files.newInputStream(objPath))) {
            header = in.readNBytes(HEADER_SIZES);
            in.transferTo(contentsBuff);
        }
        if (Arrays.equals(header, BLOB_HEADER)) {
            return new Blob(contentsBuff.toByteArray());
        }
        throw new IOException("Unknown object type: {" + new String(header) + "]}.");
    }

    public static void saveObjectBasedOnHash(Path objectsPath, String hash, MiniGitObject obj) throws IOException {
        Path objPath = objectsPath.resolve(Path.of(hash.substring(0, 2), hash.substring(2)));
        obj.write(objPath);
    }

    abstract void write(Path path) throws IOException;
    public abstract String getDescription();
}
