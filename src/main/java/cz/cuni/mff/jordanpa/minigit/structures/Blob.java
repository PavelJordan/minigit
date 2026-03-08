package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents raw file contents stored as a MiniGit blob object.
 *
 * <p>
 *     A blob stores only the file bytes. It does not store the file name,
 *     path, or any directory structure information.
 * </p>
 */
public final class Blob extends MiniGitObject implements TreeContent {
    private final byte[] content;
    private String sha1;

    /**
     * Creates a blob from raw bytes already available in memory.
     *
     * @param content raw file contents
     */
    public Blob(byte[] content) {
        this.content = content;
    }

    /**
     * Creates a blob by reading all bytes from a file (without header - not the blob file in .minigit/objects)
     *
     * @param path path to the source file
     * @throws IOException if the file cannot be read
     */
    public Blob(Path path) throws IOException {
        content = Files.readAllBytes(path);
    }

    /**
     * Returns the SHA-1 identifier of this blob.
     *
     * <p>
     *     The hash is computed from the serialized blob representation,
     *     including the MiniGit blob header, and then cached.
     * </p>
     *
     * @return SHA-1 hash of this blob, or an empty string if serialization fails
     */
    @Override
    public String miniGitSha1() {
        if (sha1 != null) {
            return sha1;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeToStream(baos);
            sha1 = getSha1FromBytes(baos.toByteArray());
            return sha1;
        }
        catch (IOException e) {
            IO.println(e);
            IO.println("Error writing blob to stream for SHA-1 calculation.");
            return "";
        }
    }

    /**
     * Returns a human-readable description of this blob.
     *
     * <p>
     *     This method is intended mainly for inspection and debugging.
     * </p>
     *
     * @return textual description of the blob and its contents
     */
    @Override
    public String getDescription() {
        return "Object:\nblob\nContent:\n" + new String(content);
    }

    /**
     * Writes only the raw blob contents to a file in the working tree.
     *
     * <p>
     *     If the parent directory does not exist, it is created first.
     * </p>
     *
     * @param path destination file path
     * @throws IOException if the file cannot be written
     */
    public void writeContentsTo(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(content);
        }
    }

    /**
     * Returns a buffered input stream over the blob contents.
     *
     * @return new input stream reading from the in-memory blob data
     */
    public BufferedInputStream getContentReader() {
        return new BufferedInputStream(new ByteArrayInputStream(content));
    }

    /**
     * Reads the blob contents as lines of text.
     *
     * <p>
     *     The bytes are decoded using the platform default charset and then split
     *     into lines.
     * </p>
     *
     * @return list of lines decoded from the blob contents
     * @throws IOException if reading from the in-memory stream fails
     */
    public List<String> readAllLines() throws IOException {
        List<String> lines;
        try (BufferedInputStream reader = getContentReader()) {
            lines = new String(reader.readAllBytes()).lines().toList();
        }
        return lines;
    }

    /**
     * Writes this blob in its internal MiniGit object format to the given path.
     *
     * @param path destination path inside MiniGit object storage
     * @throws IOException if writing fails
     */
    @Override
    void write(Path path) throws IOException {
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        writeToStream(baOs);
        writeBytes(baOs.toByteArray(), path);
    }

    /**
     * Writes the serialized blob representation to an output stream.
     *
     * <p>
     *     The written form consists of the blob header followed by the raw content bytes.
     * </p>
     *
     * @param out destination stream
     * @throws IOException if writing to the stream fails
     */
    private void writeToStream(OutputStream out) throws IOException {
        out.write(BLOB_HEADER);
        out.write(content);
    }
}
