package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.CommonAncestorFinder.findCommonAncestor;

/**
 * Helper class for merging commits and files.
 *
 * <p>
 *     This class handles commit-level merge logic, including fast-forward detection,
 *     conflict detection, and three-way file merging.
 * </p>
 */
public final class Merger {

    /**
     * Private constructor to prevent instantiation.
     */
    private Merger() { }

    /**
     * Result of a three-way merge on a single file.
     * @param blob The resulting merged blob.
     * @param hasConflicts Whether the merge contains conflict markers.
     */
    private record ThreeWayMergeResult(Blob blob, boolean hasConflicts) { }

    /**
     * Type of merge result.
     */
    public enum MergeResultType {
        /**
         * The merge was performed into the filesystem and index.
         */
        MERGED_INTO_FS,

        /**
         * The merge can be resolved by fast-forward.
         */
        FAST_FORWARD,

        /**
         * The merge failed due to invalid state or corrupted data.
         */
        ERROR,

        /**
         * There is nothing to do because the commits are already effectively merged.
         */
        NOTHING_TO_DO
    }

    /**
     * A merge conflict on a single path.
     * @param path The conflicting path.
     * @param message Human-readable description of the conflict.
     */
    public record Conflict(Path path, String message) { }

    /**
     * Result of a commit merge operation.
     * @param type The type of merge result.
     * @param newStagedIndex The new staged index after merging.
     * @param mergeIndexBlobsToSave Blobs that should be saved into the repository after merging.
     * @param conflicts List of conflicts detected during the merge.
     */
    public record MergeResult(MergeResultType type, HashMap<Path, String> newStagedIndex, HashMap<Path, Blob> mergeIndexBlobsToSave, List<Conflict> conflicts) { }

    /**
     * Merge one commit into another.
     *
     * <p>
     *     This method first finds the common ancestor, handles trivial cases such as fast-forward
     *     and nothing-to-do, and then merges all files from the three involved trees.
     * </p>
     *
     * @param from The commit to merge from.
     * @param into The commit to merge into.
     * @param objectLoader The loader used to load trees and blobs.
     * @return Result of the merge operation.
     * @throws IOException If repository data is corrupted or files cannot be loaded/written.
     */
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
            String fromHash = fromIndex.get(path);
            String toHash = intoIndex.get(path);

            // NO CONFLICT SCENARIOS
            // No change in both
            if (Objects.equals(baseHash, fromHash) && Objects.equals(baseHash, toHash)) {
                newStagedIndex.put(path, baseHash);
                continue;
            }
            // Same change in both (or deleted in both)
            if (Objects.equals(fromHash, toHash) && baseHash != null) {
                if (fromHash != null) {
                    newStagedIndex.put(path, fromHash);
                    saveBlob(objectLoader, fromHash, path);
                }
                continue;
            }
            // Deleted in one, no change in the other
            if (fromHash == null && Objects.equals(toHash, baseHash)) {
                Files.deleteIfExists(path);
                continue;
            }
            if (toHash == null && Objects.equals(fromHash, baseHash)) {
                Files.deleteIfExists(path);
                continue;
            }
            // Added in one, not in the other
            if (baseHash == null && fromHash == null) {
                newStagedIndex.put(path, toHash);
                saveBlob(objectLoader, toHash, path);
                continue;
            }
            if (baseHash == null && toHash == null) {
                newStagedIndex.put(path, fromHash);
                saveBlob(objectLoader, fromHash, path);
                continue;
            }
            // Added the same in both
            if (baseHash == null && Objects.equals(fromHash, toHash)) {
                newStagedIndex.put(path, fromHash);
                saveBlob(objectLoader, fromHash, path);
                continue;
            }
            // Modified in one, not in the other
            if (fromHash != null && Objects.equals(fromHash, baseHash)) {
                newStagedIndex.put(path, toHash);
                saveBlob(objectLoader, toHash, path);
                continue;
            }
            if (toHash != null && Objects.equals(toHash, baseHash)) {
                newStagedIndex.put(path, fromHash);
                saveBlob(objectLoader, fromHash, path);
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
                conflicts.add(new Conflict(path, "Conflict: " + path + " was deleted in one commit, modified in the other."));
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
            newStagedIndex.put(path, toHash);
            ThreeWayMergeResult result = threeWayMerge(baseHash, toHash, fromHash, objectLoader);
            result.blob.writeContentsTo(path);
            if (result.hasConflicts) {
                conflicts.add(new Conflict(path, "Conflict: " + path + " was modified in both commits. Resolve by modifying the file and staging it. Then do merge-apply."));
                newStagedIndex.put(path, baseHash);
            }
            else {
                mergeIndexBlobsToSave.put(path, result.blob);
                newStagedIndex.put(path, result.blob.miniGitSha1());
            }
        }

        return new MergeResult(MergeResultType.MERGED_INTO_FS, newStagedIndex, mergeIndexBlobsToSave, conflicts);
    }

    /**
     * Restore the specified blob into the filesystem.
     *
     * @param loader The loader used to load the blob.
     * @param blobHash Hash of the blob to restore.
     * @param path Path to restore the blob contents to.
     * @throws IOException If the blob cannot be loaded or written.
     */
    private static void saveBlob(MinigitObjectLoader loader, String blobHash, Path path) throws IOException {
        MiniGitObject blobObj = loader.loadFromInternal(blobHash);
        if (!(blobObj instanceof Blob blob)) {
            throw new IOException("Cannot save blob. Blob is not of type blob.");
        }
        blob.writeContentsTo(path);
    }

    // Code below is by ChatGPT - I provide regions where branches change something (coded by me, see MiniGitDiff.DiffResult),
    // ChatGPT merged non-overlapping, and wrote conflicts for overlapping changes.
    // This is because the code is mechanical, long, and I don't want to
    // spend time on it, as the project already spans thousands of lines, and I want to spend my attention on
    // architectonically interesting problems.

    /**
     * Cluster of overlapping or adjacent changes in the base file.
     * @param start Start of the cluster in base lines.
     * @param end End of the cluster in base lines.
     * @param oursEndIndex End index in the diff list for ours.
     * @param theirsEndIndex End index in the diff list for theirs.
     */
    private record ChangeCluster(int start, int end, int oursEndIndex, int theirsEndIndex) { }

    /**
     * Perform a three-way merge of three blobs.
     *
     * <p>
     *     The base version is compared to both modified versions. Non-overlapping changes are merged
     *     automatically, while overlapping changes produce conflict markers.
     * </p>
     *
     * @param baseHash Hash of the base blob.
     * @param oursHash Hash of our blob.
     * @param theirsHash Hash of their blob.
     * @param loader The loader used to load the blobs.
     * @return Resulting merged blob and information whether conflicts occurred.
     * @throws IOException If the blobs cannot be loaded or are invalid.
     */
    private static ThreeWayMergeResult threeWayMerge(
            String baseHash,
            String oursHash,
            String theirsHash,
            MinigitObjectLoader loader
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MiniGitObject baseBlobObj = loader.loadFromInternal(baseHash);
        MiniGitObject oursBlobObj = loader.loadFromInternal(oursHash);
        MiniGitObject theirsBlobObj = loader.loadFromInternal(theirsHash);

        if (!(baseBlobObj instanceof Blob baseBlob
                && oursBlobObj instanceof Blob oursBlob
                && theirsBlobObj instanceof Blob theirsBlob)) {
            throw new IOException("Cannot merge. Blobs are not of type blob.");
        }

        List<MiniGitDiff.DiffResult> baseOursDiff = MiniGitDiff.diff(baseBlob, oursBlob);
        List<MiniGitDiff.DiffResult> baseTheirsDiff = MiniGitDiff.diff(baseBlob, theirsBlob);

        List<String> baseLines = baseBlob.readAllLines();
        List<String> oursLines = oursBlob.readAllLines();
        List<String> theirsLines = theirsBlob.readAllLines();

        List<String> mergedLines = new ArrayList<>();
        boolean hasConflicts = false;

        int basePos = 0;
        int oursDiffIndex = 0;
        int theirsDiffIndex = 0;

        while (oursDiffIndex < baseOursDiff.size() || theirsDiffIndex < baseTheirsDiff.size()) {
            int nextOursStart = nextDiffStart(baseOursDiff, oursDiffIndex);
            int nextTheirsStart = nextDiffStart(baseTheirsDiff, theirsDiffIndex);
            int nextStart = Math.min(nextOursStart, nextTheirsStart);

            // First copy the unchanged part before the next change cluster.
            appendRange(mergedLines, baseLines, basePos, nextStart);

            // Then compute one whole cluster of related changes in both branches.
            ChangeCluster cluster = computeCluster(baseOursDiff, oursDiffIndex, baseTheirsDiff, theirsDiffIndex);

            List<String> baseRegion = copyRange(baseLines, cluster.start(), cluster.end());
            List<String> oursRegion = materializeRegion(
                    baseLines, oursLines, baseOursDiff,
                    oursDiffIndex, cluster.oursEndIndex(),
                    cluster.start(), cluster.end()
            );
            List<String> theirsRegion = materializeRegion(
                    baseLines, theirsLines, baseTheirsDiff,
                    theirsDiffIndex, cluster.theirsEndIndex(),
                    cluster.start(), cluster.end()
            );

            // Standard three-way merge cases.
            if (oursRegion.equals(theirsRegion)) {
                mergedLines.addAll(oursRegion);
            }
            else if (oursRegion.equals(baseRegion)) {
                mergedLines.addAll(theirsRegion);
            }
            else if (theirsRegion.equals(baseRegion)) {
                mergedLines.addAll(oursRegion);
            }
            else {
                hasConflicts = true;
                addConflictMarkers(mergedLines, oursRegion, theirsRegion);
            }

            basePos = cluster.end();
            oursDiffIndex = cluster.oursEndIndex();
            theirsDiffIndex = cluster.theirsEndIndex();
        }

        appendRange(mergedLines, baseLines, basePos, baseLines.size());

        for (int i = 0; i < mergedLines.size(); i++) {
            out.write(mergedLines.get(i).getBytes());
            if (i + 1 < mergedLines.size()) {
                out.write('\n');
            }
        }

        return new ThreeWayMergeResult(new Blob(out.toByteArray()), hasConflicts);
    }

    /**
     * Get the start of the next diff, or {@link Integer#MAX_VALUE} if there is none.
     *
     * @param diffs List of diffs.
     * @param index Current diff index.
     * @return Start line of the next diff.
     */
    private static int nextDiffStart(List<MiniGitDiff.DiffResult> diffs, int index) {
        if (index >= diffs.size()) {
            return Integer.MAX_VALUE;
        }
        return Math.toIntExact(diffs.get(index).replaceFrom());
    }

    /**
     * Check whether a diff belongs to the current change cluster.
     *
     * @param diff The diff to test.
     * @param clusterStart Start of the cluster.
     * @param clusterEnd End of the cluster.
     * @return True if the diff belongs to the cluster, false otherwise.
     */
    private static boolean belongsToCluster(MiniGitDiff.DiffResult diff, int clusterStart, int clusterEnd) {
        int diffStart = Math.toIntExact(diff.replaceFrom());
        if (clusterEnd == clusterStart) {
            return diffStart == clusterStart;
        }
        return diffStart < clusterEnd;
    }

    /**
     * Compute one cluster of overlapping or adjacent changes from both diff lists.
     *
     * @param oursDiffs Diffs from base to ours.
     * @param oursStartIndex Starting index in our diff list.
     * @param theirsDiffs Diffs from base to theirs.
     * @param theirsStartIndex Starting index in their diff list.
     * @return Computed change cluster.
     */
    private static ChangeCluster computeCluster(
            List<MiniGitDiff.DiffResult> oursDiffs,
            int oursStartIndex,
            List<MiniGitDiff.DiffResult> theirsDiffs,
            int theirsStartIndex
    ) {
        int oursStart = nextDiffStart(oursDiffs, oursStartIndex);
        int theirsStart = nextDiffStart(theirsDiffs, theirsStartIndex);
        int clusterStart = Math.min(oursStart, theirsStart);
        int clusterEnd = clusterStart;

        int o = oursStartIndex;
        int t = theirsStartIndex;

        boolean advanced;
        do {
            advanced = false;

            while (o < oursDiffs.size() && belongsToCluster(oursDiffs.get(o), clusterStart, clusterEnd)) {
                clusterEnd = Math.max(clusterEnd, Math.toIntExact(oursDiffs.get(o).replaceTo()));
                o++;
                advanced = true;
            }

            while (t < theirsDiffs.size() && belongsToCluster(theirsDiffs.get(t), clusterStart, clusterEnd)) {
                clusterEnd = Math.max(clusterEnd, Math.toIntExact(theirsDiffs.get(t).replaceTo()));
                t++;
                advanced = true;
            }
        } while (advanced);

        return new ChangeCluster(clusterStart, clusterEnd, o, t);
    }

    /**
     * Materialize one region of a changed file by applying relevant diffs to the base region.
     *
     * @param baseLines Lines of the base file.
     * @param changedLines Lines of the changed file.
     * @param diffs Diff list from base to changed file.
     * @param fromDiffIndex First relevant diff index.
     * @param toDiffIndex End diff index.
     * @param regionStart Start of the region in base lines.
     * @param regionEnd End of the region in base lines.
     * @return Materialized merged region.
     */
    private static List<String> materializeRegion(
            List<String> baseLines,
            List<String> changedLines,
            List<MiniGitDiff.DiffResult> diffs,
            int fromDiffIndex,
            int toDiffIndex,
            int regionStart,
            int regionEnd
    ) {
        List<String> result = new ArrayList<>();
        int basePos = regionStart;

        for (int i = fromDiffIndex; i < toDiffIndex; i++) {
            MiniGitDiff.DiffResult diff = diffs.get(i);

            int replaceFrom = Math.toIntExact(diff.replaceFrom());
            int replaceTo = Math.toIntExact(diff.replaceTo());
            int replaceWithFrom = Math.toIntExact(diff.replaceWithFrom());
            int replaceWithTo = Math.toIntExact(diff.replaceWithTo());

            appendRange(result, baseLines, basePos, replaceFrom);
            appendRange(result, changedLines, replaceWithFrom, replaceWithTo);
            basePos = replaceTo;
        }

        appendRange(result, baseLines, basePos, regionEnd);
        return result;
    }

    /**
     * Copy a range of lines into a new list.
     *
     * @param lines Source lines.
     * @param from Start index.
     * @param to End index.
     * @return Copied range.
     */
    private static List<String> copyRange(List<String> lines, int from, int to) {
        List<String> result = new ArrayList<>();
        appendRange(result, lines, from, to);
        return result;
    }

    /**
     * Append a range of lines from one list to another.
     *
     * @param out Output list.
     * @param source Source list.
     * @param from Start index.
     * @param to End index.
     */
    private static void appendRange(List<String> out, List<String> source, int from, int to) {
        for (int i = from; i < to; i++) {
            out.add(source.get(i));
        }
    }

    /**
     * Add standard conflict markers and both conflicting regions.
     *
     * @param out Output list of merged lines.
     * @param oursRegion Our conflicting region.
     * @param theirsRegion Their conflicting region.
     */
    private static void addConflictMarkers(List<String> out, List<String> oursRegion, List<String> theirsRegion) {
        out.add("<<<<<<< OURS");
        out.addAll(oursRegion);
        out.add("=======");
        out.addAll(theirsRegion);
        out.add(">>>>>>> THEIRS");
    }
}
