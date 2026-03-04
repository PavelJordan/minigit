package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.CommonAncestorFinder.findCommonAncestor;

public final class Merger {

    public enum MergeResultType {
        MERGED,
        FAST_FORWARD,
        ERROR,
        NOTHING_TO_DO
    }
    public record Conflict(Path path, String message) { }
    public record MergeResult(MergeResultType type, HashMap<Path, String> newStagedIndex, HashMap<Path, Blob> mergeIndexBlobsToSave, List<Conflict> conflicts) { }
    public static MergeResult merge(Commit from, Commit into, MinigitObjectLoader objectLoader) throws IOException {

        MiniGitObject fromTreeObj = objectLoader.loadFromInternal(from.getTreeHash());
        MiniGitObject intoTreeObj = objectLoader.loadFromInternal(into.getTreeHash());
        Commit commonAncestor = findCommonAncestor(from, into, objectLoader);
        if (commonAncestor == null || !(fromTreeObj instanceof Tree fromTree && intoTreeObj instanceof Tree intoTree)) {
            return new MergeResult(MergeResultType.ERROR, null, new HashMap<>(), List.of());
        }

        MiniGitObject baseTreeObj = objectLoader.loadFromInternal(commonAncestor.getTreeHash());
        if (!(baseTreeObj instanceof Tree baseTree)) {
            return new MergeResult(MergeResultType.ERROR, null, new HashMap<>(), List.of());
        }
        if (commonAncestor.miniGitSha1().equals(from.miniGitSha1())) {
            return new MergeResult(MergeResultType.NOTHING_TO_DO, null, new HashMap<>(), List.of());
        }
        else if (commonAncestor.miniGitSha1().equals(into.miniGitSha1())) {
            return new MergeResult(MergeResultType.FAST_FORWARD, null, new HashMap<>(), List.of());
        }

        Map<Path, String> fromIndex = fromTree.getIndex(objectLoader);
        Map<Path, String> intoIndex = intoTree.getIndex(objectLoader);
        Map<Path, String> baseIndex = baseTree.getIndex(objectLoader);

        Set<Path> allPaths = new HashSet<>(fromIndex.keySet());
        allPaths.addAll(intoIndex.keySet());
        allPaths.addAll(baseIndex.keySet());

        HashMap<Path, String> newStagedIndex = new HashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        HashMap<Path, Blob> mergeIndexBlobsToSave = new HashMap<>();

        for (Path path : allPaths) {
            String baseHash = baseIndex.get(path);
            String fromHash = baseIndex.get(path);
            String toHash = baseIndex.get(path);

            // NO CONFLICT SCENARIOS

            // Deleted in both
            if (baseHash != null && fromHash == null && toHash == null) {
                continue;
            }
            // Deleted in one, no change in the other
            if (fromHash == null && toHash.equals(baseHash)) {
                continue;
            }
            if (toHash == null && fromHash.equals(baseHash)) {
                continue;
            }
            // Added in one, not in the other
            if (baseHash == null && fromHash == null) {
                newStagedIndex.put(path, toHash);
                continue;
            }
            if (baseHash == null && toHash == null) {
                newStagedIndex.put(path, fromHash);
                continue;
            }
            // Added the same in both
            if (baseHash == null && fromHash.equals(toHash)) {
                newStagedIndex.put(path, fromHash);
                continue;
            }
            // Modified in one, not in the other
            if (fromHash != null && fromHash.equals(baseHash) && !toHash.equals(baseHash)) {
                newStagedIndex.put(path, toHash);
                continue;
            }
            if (toHash != null && toHash.equals(baseHash) && !fromHash.equals(baseHash)) {
                newStagedIndex.put(path, fromHash);
                continue;
            }
            // CONFLICT SCENARIOS

            // Deleted in one, modified in the other -> unstaged modification
            if (fromHash != null && toHash == null) {
                newStagedIndex.put(path, baseHash);
                saveBlob(objectLoader, fromHash, path);
                conflicts.add(new Conflict(path, "Conflict: " + path + " was deleted in one commit, modified in the other."));
                continue;
            }
            if (fromHash == null) {
                newStagedIndex.put(path, baseHash);
                saveBlob(objectLoader, toHash, path);
                conflicts.add(new Conflict(path, "Conflict: " + path + " was added in one commit, deleted in the other."));
                continue;
            }
            // Added in both, but different -> stage from, unstage to
            if (baseHash == null) {
                newStagedIndex.put(path, fromHash);
                saveBlob(objectLoader, toHash, path);
                conflicts.add(new Conflict(path, "Conflict: " + path + " was added from both commits. Staged one, unstaged the other. Choose by staging unstaged/restoring unstaged and merge-apply"));
                continue;
            }
            // Both modified -> three-way merge
            newStagedIndex.put(path, baseHash);
            conflicts.add(new Conflict(path, "Conflict: " + path + " was modified in both commits. Resolve by modifying the file and staging it. Then do merge-apply."));
            mergeIndexBlobsToSave.put(path, threeWayMerge(baseHash, toHash, fromHash, objectLoader));

        }

        return new MergeResult(MergeResultType.MERGED, newStagedIndex, new HashMap<>(), conflicts);
    }

    private static Blob threeWayMerge(String baseHash, String oursHash, String theirsHash, MinigitObjectLoader loader) throws IOException {
        throw new RuntimeException("Not implemented yet.");
    }

    private static void saveBlob(MinigitObjectLoader loader, String blobHash, Path path) throws IOException {
        MiniGitObject blobObj = loader.loadFromInternal(blobHash);
        if (!(blobObj instanceof Blob blob)) {
            throw new IOException("Cannot save blob. Blob is not of type blob.");
        }
        blob.writeContentsTo(path);
    }
}
