package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The class implementing the diff algorithm, as described in `docs/DiffImplementation.txt`.
 *
 * <p>
 *     The only difference is that the lines here are indexed 0-based. Also,
 *     instead of binary-searching for the best path, we use a simple linear search,
 *     which should be probably replaced with a binary search in the future.
 * </p>
 */
public final class MiniGitDiff {

    /**
     * Result of a diff operation.
     * @param replaceFrom 0-based index of the first line to replace
     * @param replaceTo 0-based index of the last line to replace
     * @param replaceWithFrom 0-based index of the first line to replace with
     * @param replaceWithTo 0-based index of the last line to replace with
     */
    public record DiffResult(long replaceFrom, long replaceTo, long replaceWithFrom, long replaceWithTo) {}

    /**
     * List of line indices
     */
    private record LineIndices(List<Integer> lines) {}

    /**
     * A k-candidate for the LCS problem.
     *
     * <p>
     *     Says that the "least advancing" path of "k" equal lines so far is at these indices.
     *     For an exact definition, see `docs/DiffImplementation.txt`.
     * </p>
     * @param i Line in the "to" file
     * @param j Line in the "from" file
     */
    private record KCandidate(long i, long j) {}
    private static final class KVector {
        // indexed by k
        private final List<KCandidate> kVector = new ArrayList<>();
        private final HashMap<KCandidate, KCandidate> backTrack = new HashMap<>();

        public KVector() {
            KCandidate start = new KCandidate(-1, -1);
            kVector.add(start);
            backTrack.put(start, null);
        }
        public KCandidate getBest(int k) {
            if (k < kVector.size()) {
                return kVector.get(k);
            }
            return new KCandidate(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        public void tryInsertCandidate(KCandidate candidate) {
            int currentK = 0;
            // TODO use binary search
            while(getBest(currentK).j() < candidate.j()) {
                currentK++;
            }
            if (getBest(currentK).j() == candidate.j()) {
                return;
            }
            if (currentK == kVector.size()) {
                backTrack.put(candidate, getBest(currentK - 1));
                kVector.add(candidate);
            }
            else {
                backTrack.put(candidate, getBest(currentK - 1));
                kVector.set(currentK, candidate);
            }
        }

        public List<KCandidate> getTrack() {
            ArrayList<KCandidate> result = new ArrayList<>();
            for (KCandidate bestPath = getBest(kVector.size() - 1); bestPath != null; bestPath = backTrack.get(bestPath)) {
                result.add(bestPath);
            }
            Collections.reverse(result);
            return result;
        }
    }

    /**
     * Diff two blobs.
     *
     * <p>
     *     Use the diff algorithm, as described in `docs/DiffImplementation.txt`.
     * </p>
     * <p>
     *     Works for binary files too, but the result will not really be human-readable. Still useful for
     *     machine comparison.
     * </p>
     *
     * @see DiffResult
     * @param from The first blob to compare to - these lines replace the other blob
     * @param to The second blob to compare to - the lines that are replaced
     * @return List of diff results, sorted by replaceFrom
     * @throws IOException If the blobs cannot be read. This is almost guaranteed to not happen, as we don't need to read the blobs from the filesystem.
     */
    public static List<DiffResult> diff(Blob from, Blob to) throws IOException {

        // Find all lines that match between the two blobs (their indices). Essentially models 2dim matrix, but compactly
        List<LineIndices> matchingLines = getMatchingLines(from, to);

        // Use K-candidates to solve the LCS problem. See the description in `docs/DiffImplementation.txt`,
        // as this mirrors the algorithm described there, and its correctness.
        // The only difference is that the lines here are indexed 0-based
        KVector kVec = new KVector();
        for (int i = 0; i < matchingLines.size(); i++) {
            LineIndices lineIndices = matchingLines.get(i);
            for (int j = lineIndices.lines().size() - 1; j >= 0; j--) {
                kVec.tryInsertCandidate(new KCandidate(i, lineIndices.lines().get(j)));
            }
        }

        // Get the track of the best path found by solving LCS.
        List<KCandidate> track = kVec.getTrack();

        // Get the number of lines in both blobs
        long fromLen, toLen;
        fromLen = from.readAllLines().size();
        toLen = to.readAllLines().size();

        // Add a fake candidate to not leave out the lines at the end
        track.add(new KCandidate(fromLen, toLen));

        // Convert the track to a list of diff results
        List<DiffResult> results = new ArrayList<>();
        KCandidate previous = track.removeFirst();
        for (KCandidate kCandidate : track) {
            if (kCandidate.i == previous.i + 1 && kCandidate.j == previous.j + 1) {
                // The lines are equal, continue
                previous = kCandidate;
                continue;
            }
            // The lines are not equal, add a diff result
            long replaceBegin = previous.i() + 1;
            long replaceEnd = kCandidate.i();
            long replaceWithBegin = previous.j() + 1;
            long replaceWithEnd = kCandidate.j();
            results.add(new DiffResult(replaceBegin, replaceEnd, replaceWithBegin, replaceWithEnd));
            previous = kCandidate;
        }

        return results;
    }

    private static HashMap<String, LineIndices> getEquivalenceClasses(List<String> lines) throws IOException {
        HashMap<String, LineIndices> equivalenceClasses = new HashMap<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (!equivalenceClasses.containsKey(line)) {
                equivalenceClasses.put(line, new LineIndices(new ArrayList<>()));
            }
            equivalenceClasses.get(line).lines().add(lineIndex);
        }
        return equivalenceClasses;
    }

    private static List<LineIndices> getMatchingLines(Blob baseFile, Blob findMatchingLinesIn) throws IOException {
        HashMap<String, LineIndices> equivalenceClasses;
        equivalenceClasses = getEquivalenceClasses(findMatchingLinesIn.readAllLines());
        List<LineIndices> matchingLines = new ArrayList<>();
        for (String line : baseFile.readAllLines()) {
            if (equivalenceClasses.containsKey(line)) {
                matchingLines.add(equivalenceClasses.get(line));
            }
            else {
                matchingLines.add(new LineIndices(List.of()));
            }
        }
        return matchingLines;
    }
}
