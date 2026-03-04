package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MiniGitDiff {
    public record DiffResult(Integer replaceFrom, Integer replaceTo, Integer replaceWithFrom, Integer replaceWithTo) {}
    private record LineIndices(List<Integer> lines) {}
    private record KCandidate(int i, int j) {}
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
            return new KCandidate(Integer.MAX_VALUE, Integer.MAX_VALUE);
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
            result.removeLast();
            return result.reversed();
        }
    }

    public static List<DiffResult> diff(Blob from, Blob to) throws IOException {
        List<LineIndices> matchingLines = getMatchingLines(from, to);
        KVector kVec = new KVector();
        for (int i = 0; i < matchingLines.size(); i++) {
            LineIndices lineIndices = matchingLines.get(i);
            for (int j = lineIndices.lines().size() - 1; j >= 0; j--) {
                kVec.tryInsertCandidate(new KCandidate(i, lineIndices.lines().get(j)));
            }
        }
        List<KCandidate> track = kVec.getTrack();
        List<DiffResult> results = new ArrayList<>();
        KCandidate previous = track.removeFirst();
        for (KCandidate kCandidate : track) {
            if (kCandidate.i == previous.i + 1 && kCandidate.j == previous.j + 1) {
                previous = kCandidate;
                continue;
            }
            int replaceBegin = previous.i() + 1;
            int replaceEnd = kCandidate.i();
            int replaceWithBegin = previous.j() + 1;
            int replaceWithEnd = kCandidate.j();
            results.add(new DiffResult(replaceBegin, replaceEnd, replaceWithBegin, replaceWithEnd));
            previous = kCandidate;
        }
        return results;
    }

    private static HashMap<String, LineIndices> getEquivalenceClasses(BufferedReader fileStream) throws IOException {
        HashMap<String, LineIndices> equivalenceClasses = new HashMap<>();
        String line;
        int lineIndex = 0;
        while((line = fileStream.readLine()) != null) {
            if (!equivalenceClasses.containsKey(line)) {
                equivalenceClasses.put(line, new LineIndices(new ArrayList<>()));
            }
            equivalenceClasses.get(line).lines().add(lineIndex);
            lineIndex++;
        }
        return equivalenceClasses;
    }

    private static List<LineIndices> getMatchingLines(Blob baseFile, Blob findMatchingLinesIn) throws IOException {
        HashMap<String, LineIndices> equivalenceClasses;
        try (BufferedReader findMatchingLinesInStream = findMatchingLinesIn.getContentReader()) {
            equivalenceClasses = getEquivalenceClasses(findMatchingLinesInStream);
        }
        List<LineIndices> matchingLines = new ArrayList<>();
        try(BufferedReader baseFileStream = baseFile.getContentReader()) {
            String line;
            while((line = baseFileStream.readLine()) != null) {
                if (equivalenceClasses.containsKey(line)) {
                    matchingLines.add(equivalenceClasses.get(line));
                }
                else {
                    matchingLines.add(new LineIndices(List.of()));
                }
            }
        }
        return matchingLines;
    }
}
