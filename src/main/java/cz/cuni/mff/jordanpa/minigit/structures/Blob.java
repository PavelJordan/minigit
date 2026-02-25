package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * File content, as represented in MiniGit.
 */
public final class Blob extends MiniGitObject implements TreeContent {
    private final byte[] content;

    public Blob(byte[] content) {
        this.content = content;
    }
    public Blob(Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    private String sha1;

    @Override
    public String miniGitSha1() {
        if (sha1 != null) {
            return sha1;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeToStream(baos);
            sha1 = byteArray2Hex(md.digest(baos.toByteArray()));
            return sha1;
        } catch (NoSuchAlgorithmException e) {
            IO.println("SHA-1 algorithm not found. This should never happen.");
            return "";
        }
        catch (IOException e) {
            IO.println(e);
            IO.println("Error writing blob to stream for SHA-1 calculation.");
            return "";
        }
    }

    @Override
    void write(Path path) throws IOException {
        if (Files.exists(path)) {
            IO.println("Object already exists with hash [" + miniGitSha1() + "] at " + path + ". Skipping write.");
            return;
        }
        Files.createDirectories(path.getParent());
        try(var out = new BufferedOutputStream(Files.newOutputStream(path))) {
            writeToStream(out);
        }
        IO.println("Blob written with hash [" + miniGitSha1() + "] to " + path);
    }

    @Override
    public String getDescription() {
        return "Object:\nblob\nContent:\n" + new String(content);
    }

    private void writeToStream(OutputStream out) throws IOException {
        out.write(BLOB_HEADER);
        out.write(content);
    }

    public void writeContentsTo(Path path) throws IOException{
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(content);
        }
    }
}
