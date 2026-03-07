package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


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

    public BufferedInputStream getContentReader() {
        return new BufferedInputStream(new ByteArrayInputStream(content));
    }

    private void writeToStream(OutputStream out) throws IOException {
        out.write(BLOB_HEADER);
        out.write(content);
    }

    public List<String> readAllLines() throws IOException {
        List<String> lines;
        try (BufferedInputStream reader = getContentReader()) {
            lines = new String(reader.readAllBytes()).lines().toList();
        }
        return lines;
    }
}
