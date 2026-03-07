package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CommitTest {
    @TempDir
    Path tempDir;
    final String snapshot = "snapshot";
    final String parent = "parent";
    final String otherParent = "otherParent";
    final Author author = new Author("author", "email");
    final String message = "message";
    final Date date = new Date(1234567890);

    @Test
    void commitDoesntChangeWhenSavedIntoFileSystem() {
        Commit commit = new Commit(snapshot, parent, author, message, date);
        try {
            commit.write(tempDir.resolve(commit.miniGitSha1()));
            Commit loadedCommit = new Commit(Files.readAllBytes(tempDir.resolve(commit.miniGitSha1())));
            loadedCommit.equals(commit);
        } catch (IOException e) {
            fail("Could not write commit to file");
        }
    }

    @Test
    void commitDoesntChangeWhenSavedIntoFileSystemMultipleParents() {
        Commit commit = new Commit(snapshot, parent, otherParent, author, message, date);
        try {
            commit.write(tempDir.resolve(commit.miniGitSha1()));
            Commit loadedCommit = new Commit(Files.readAllBytes(tempDir.resolve(commit.miniGitSha1())));
            loadedCommit.equals(commit);
        } catch (IOException e) {
            fail("Could not write commit to file");
        }
    }

    @Test
    void differentShaWhenAnythingChanges() {
        Commit commit = new Commit(snapshot, parent, author, message, date);
        Commit commitDate = new Commit(snapshot, parent, author, message, new Date(1234568890));
        Commit commitAuthor = new Commit(snapshot, parent, new Author("author2", "email"), message, date);
        Commit commitMessage = new Commit(snapshot, parent, author, "message2", date);
        Commit commitParent = new Commit(snapshot, "parent2", author, message, date);
        Commit commitSnapshot = new Commit("snapshot2", parent, author, message, date);
        assertNotEquals(commit.miniGitSha1(), commitDate.miniGitSha1());
        assertNotEquals(commit.miniGitSha1(), commitAuthor.miniGitSha1());
        assertNotEquals(commit.miniGitSha1(), commitMessage.miniGitSha1());
        assertNotEquals(commit.miniGitSha1(), commitParent.miniGitSha1());
        assertNotEquals(commit.miniGitSha1(), commitSnapshot.miniGitSha1());
    }
}
