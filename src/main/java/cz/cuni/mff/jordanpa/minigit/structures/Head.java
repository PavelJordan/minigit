package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Represents a head of a repository.
 *
 * <p>
 *     A head is a pointer to a commit or a branch (or UNSET). If the commit is UNSET, data contains undefined things.
 * </p>
 * <p>
 *     If the head is a pointer to a commit, the data field contains the hash of the commit.
 *     After a new commit is made, it simply advances. The head is said to be in a DETACHED state.
 * </p>
 * <p>
 *     If the head is a pointer to a branch, the data field contains the name of the branch.
 *     After a new commit is made, the branch advances together with the head.
 * </p>
 * @param type UNSET, detached to COMMIT, or following a BRANCH
 * @param data garbage when UNSET, hash of the commit when detached, name of the branch when following
 */
public record Head(Type type, String data) {
    public enum Type {
        BRANCH,
        COMMIT,
        UNSET
    }

    public static Head loadHead(Path path) throws IOException {
        if  (Files.notExists(path)) {
            return new Head(Type.UNSET, null);
        }
        try (var out = Files.newBufferedReader(path)) {
            String type = out.readLine();
            if (Type.valueOf(type) != Type.UNSET) {
                String hash = out.readLine();
                return new Head(Type.valueOf(type), hash);
            }
            return new Head(Type.UNSET, null);
        }
    }

    public void write(Path path) throws IOException {
        try(var out = Files.newBufferedWriter(path)) {
            out.write(type.name() + "\n");
            if (data != null) {
                out.write(data);
            }
        }
    }

    public HashMap<Path, String> getCommitIndex(MinigitObjectLoader loader) throws IOException {
        String hash;
        if (type() == Head.Type.BRANCH) {
            hash = loader.getHashFromRef(data());
        }
        else if (type() == Head.Type.COMMIT) {
            hash = data();
        }
        else {
            return new HashMap<>();
        }
        MiniGitObject maybeCommit = loader.loadFromInternal(hash);
        if (maybeCommit instanceof Commit commit) {
            MiniGitObject maybeTree = loader.loadFromInternal(commit.getTreeHash());
            if (maybeTree instanceof Tree tree) {
                return tree.getIndex(loader);
            }
            else {
                throw new IOException("Cannot restore tree. Tree is not in repository.");
            }
        }
        else {
            throw new IOException("Cannot restore head.");
        }
    }
}
