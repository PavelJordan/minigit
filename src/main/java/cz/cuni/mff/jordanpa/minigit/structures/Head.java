package cz.cuni.mff.jordanpa.minigit.structures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

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
