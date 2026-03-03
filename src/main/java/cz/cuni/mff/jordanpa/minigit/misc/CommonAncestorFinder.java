package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.MinigitObjectLoader;

import java.io.IOException;
import java.util.*;

public final class CommonAncestorFinder {
    private static Commit loadCommit(String hash, MinigitObjectLoader loader) throws IOException {
        MiniGitObject obj = loader.loadFromInternal(hash);
        if (!(obj instanceof Commit commit)) {
            throw new IOException("Repo corrupted. This is not a commit: " + hash);
        }
        return commit;
    }

    public static Commit findCommonAncestor(Commit from, Commit into, MinigitObjectLoader loader) throws IOException {
        Map<String, Integer> distancesToFromCommit = new HashMap<>();
        Deque<String> bfsQueue = new ArrayDeque<>();
        distancesToFromCommit.put(from.miniGitSha1(), 0);
        bfsQueue.add(from.miniGitSha1());

        while (!bfsQueue.isEmpty()) {
            String commitToProcessHash = bfsQueue.removeFirst();
            int distanceToProcessedCommit = distancesToFromCommit.get(commitToProcessHash);
            Commit commitToProcess = loadCommit(commitToProcessHash, loader);
            for (String parent : commitToProcess.getParents()) {
                if (!distancesToFromCommit.containsKey(parent)) {
                    distancesToFromCommit.put(parent, distanceToProcessedCommit + 1);
                    bfsQueue.addLast(parent);
                }
            }
        }

        Map<String, Integer> distancesToIntoCommit = new HashMap<>();
        distancesToIntoCommit.put(into.miniGitSha1(), 0);
        bfsQueue.add(into.miniGitSha1());

        int bestSum = Integer.MAX_VALUE;
        String bestHash = null;

        while (!bfsQueue.isEmpty()) {
            String commitToProcessHash = bfsQueue.removeFirst();
            int distanceToProcessedCommit = distancesToIntoCommit.get(commitToProcessHash);
            if (distanceToProcessedCommit >= bestSum) {
                continue;
            }
            Integer distanceFromFromCommit = distancesToFromCommit.get(commitToProcessHash);
            if (distanceFromFromCommit != null) {
                int sum = distanceFromFromCommit + distanceToProcessedCommit;
                if (sum < bestSum) {
                    bestSum = sum;
                    bestHash = commitToProcessHash;
                }
            }
            Commit commitToProcess = loadCommit(commitToProcessHash, loader);
            for (String parent : commitToProcess.getParents()) {
                if (distancesToIntoCommit.putIfAbsent(parent, distanceToProcessedCommit + 1) == null) {
                    bfsQueue.addLast(parent);
                }
            }
        }

        return bestHash != null ? loadCommit(bestHash, loader) : null;
    }
}
