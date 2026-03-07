package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.CommonAncestorFinder.findCommonAncestor;

public final class Merger {

    private record ThreeWayMergeResult(Blob blob, boolean hasConflicts) { }

    public enum MergeResultType {
        MERGED_INTO_FS,
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

    private record ChangeCluster(int start, int end, int oursEndIndex, int theirsEndIndex) { }

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

        List<String> baseLines = readLines(baseBlob);
        List<String> oursLines = readLines(oursBlob);
        List<String> theirsLines = readLines(theirsBlob);

        List<String> mergedLines = new ArrayList<>();
        boolean hasConflicts = false;

        int basePos = 0;
        int oursDiffIndex = 0;
        int theirsDiffIndex = 0;

        while (oursDiffIndex < baseOursDiff.size() || theirsDiffIndex < baseTheirsDiff.size()) {
            int nextOursStart = nextDiffStart(baseOursDiff, oursDiffIndex);
            int nextTheirsStart = nextDiffStart(baseTheirsDiff, theirsDiffIndex);
            int nextStart = Math.min(nextOursStart, nextTheirsStart);

            appendRange(mergedLines, baseLines, basePos, nextStart);

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

    private static List<String> readLines(Blob blob) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = blob.getContentReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static int nextDiffStart(List<MiniGitDiff.DiffResult> diffs, int index) {
        if (index >= diffs.size()) {
            return Integer.MAX_VALUE;
        }
        return Math.toIntExact(diffs.get(index).replaceFrom());
    }

    private static boolean belongsToCluster(MiniGitDiff.DiffResult diff, int clusterStart, int clusterEnd) {
        int diffStart = Math.toIntExact(diff.replaceFrom());
        if (clusterEnd == clusterStart) {
            return diffStart == clusterStart;
        }
        return diffStart < clusterEnd;
    }

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

    private static List<String> copyRange(List<String> lines, int from, int to) {
        List<String> result = new ArrayList<>();
        appendRange(result, lines, from, to);
        return result;
    }

    private static void appendRange(List<String> out, List<String> source, int from, int to) {
        for (int i = from; i < to; i++) {
            out.add(source.get(i));
        }
    }

    private static void addConflictMarkers(List<String> out, List<String> oursRegion, List<String> theirsRegion) {
        out.add("<<<<<<< OURS");
        out.addAll(oursRegion);
        out.add("=======");
        out.addAll(theirsRegion);
        out.add(">>>>>>> THEIRS");
    }
}
