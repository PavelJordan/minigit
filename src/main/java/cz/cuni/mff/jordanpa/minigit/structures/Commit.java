package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Represents a frozen state of a repository the user can refer to.
 * Can have multiple child commits == branching, or multiple parent commits == merge commit.
 * However, it does not have to remember the children, only the parents (the previous commits).
 * This is an immutable structure!
 */
public final class Commit extends MiniGitObject {
    /**
     * Hash of the tree object this commit refers to.
     */
    private final String Snapshot;
    /**
     * Hashes of the parent commits
     */
    private final String[] parents;
    private final String message;
    private final Author author;
    private final Date date;
    private String sha1;
    public final String AUTHOR_HEADER = "AUTHOR";

    /**
     * Creates a new commit.
     * @param snapshot the data of the tree object this commit refers to.
     * @param parent the data of the parent commit
     * @param author the author of this commit
     * @param message the commit message
     */
    public Commit(String snapshot, String parent, Author author, String message, Date date) {
        this.Snapshot = snapshot;
        this.parents = parent == null ? new String[0] : new String[]{parent};
        this.message = message;
        this.author = author;
        this.date = date;
    }

    public String [] getParents() {
        return Arrays.copyOf(parents, parents.length);
    }

    Commit(byte[] byteArray) {
        try(Scanner scanner = new Scanner(new ByteArrayInputStream(byteArray))) {
            this.Snapshot = scanner.nextLine();
            String nextLine = scanner.nextLine();
            List<String> parents = new ArrayList<>();
            while (!nextLine.equals(AUTHOR_HEADER)) {
                parents.add(nextLine);
                nextLine = scanner.nextLine();
            }
            this.parents = parents.toArray(new String[0]);
            String authorName = scanner.nextLine();
            String authorEmail =  scanner.nextLine();
            this.author = new Author(authorName, authorEmail);
            String dateStr = scanner.nextLine();
            this.date = DATE_FORMAT.parse(dateStr);
            StringBuilder sb = new StringBuilder();
            if (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                scanner.forEachRemaining(s -> sb.append("\n").append(s));
            }
            this.message = sb.toString();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void write(Path path) throws IOException {
        writeBytes(getFileBytes(), path);
    }

    @Override
    public String getDescription() {
        return getAnnotatedDescription(Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public String getAnnotatedDescription(Map<String, String> branchNameToHash, Map<String, String> tagNameToHash, Head head) {
        String hashStr = "Commit Hash: " + miniGitSha1();
        StringBuilder hashStrEnd = new StringBuilder();
        if (head != null && head.type() == Head.Type.COMMIT && head.data().equals(miniGitSha1())) {
            hashStrEnd.append(" (DETACHED HEAD)");
        }
        for (Map.Entry<String, String> entry : branchNameToHash.entrySet()) {
            if (entry.getValue().equals(miniGitSha1())) {
                hashStrEnd.append(" (branch [").append(entry.getKey()).append("]");
                if (head != null && head.type() == Head.Type.BRANCH && head.data().equals(entry.getKey())) {
                    hashStrEnd.append(" <- HEAD");
                }
                hashStrEnd.append(")");
            }
        }
        for (Map.Entry<String, String> entry : tagNameToHash.entrySet()) {
            if (entry.getValue().equals(miniGitSha1())) {
                hashStrEnd.append(" (tag [").append(entry.getKey()).append("])");
            }
        }
        String treeHashStr = "Tree data : " + Snapshot + "\n";
        String authorStr = "Author: " + author.name() + ", " + author.email() + "\n";
        String dateStr = "Date: " + date.toString() + "\n";
        String messageStr = "Message: \n[\n" + message + "\n]\n";
        return hashStr + hashStrEnd.append("\n") + treeHashStr + authorStr + dateStr + messageStr;
    }

    /**
     * Returns the data of this object.
     * Should be calculated only once, for example, when the object is first created.
     */
    @Override
    public String miniGitSha1() {
        if (sha1 == null) {
            sha1 = getSha1FromBytes(getFileBytes());
        }
        return sha1;
    }

    private byte[] getFileBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(COMMIT_HEADER);
            out.write(Snapshot.getBytes());
            for (String parent : parents) {
                out.write('\n');
                out.write(parent.getBytes());
            }
            out.write(("\n" + AUTHOR_HEADER + "\n").getBytes());
            out.write(author.name().getBytes());
            out.write('\n');
            out.write(author.email().getBytes());
            out.write('\n');
            out.write(DATE_FORMAT.format(date).getBytes());
            out.write('\n');
            out.write(message.getBytes());
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    public String getTreeHash() {
        return Snapshot;
    }
}
