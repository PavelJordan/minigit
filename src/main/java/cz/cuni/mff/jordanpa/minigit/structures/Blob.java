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
        content = Files.readAllBytes(path);
    }

    private String sha1;

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

    @Override
    void write(Path path) throws IOException {
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        writeToStream(baOs);
        writeBytes(baOs.toByteArray(), path);
        IO.println("Blob written with hash [" + miniGitSha1() + "] to " + path);
    }

    @Override
    public String getDescription() {
        return "Object:\nblob\nContent:\n" + new String(content);
    }

    public void writeContentsTo(Path path) throws IOException{
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
            out.write(content);
        }
    }

    private void writeToStream(OutputStream out) throws IOException {
        out.write(BLOB_HEADER);
        out.write(content);
    }
}
