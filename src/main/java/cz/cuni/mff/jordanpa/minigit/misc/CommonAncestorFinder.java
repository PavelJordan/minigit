package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.MinigitObjectLoader;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for finding the best common ancestor of two commits. This is ChatGPT coded.
 *
 * <p>
 *     This is mainly used for merge operations. The algorithm does a breadth-first search
 *     from both commits through their parents and picks a common ancestor with the smallest
 *     sum of distances from both commits.
 * </p>
 */
public final class CommonAncestorFinder {

    /**
     * Find the best common ancestor of two commits.
     *
     * <p>
     *     The returned commit is a common ancestor reachable from both commits by following
     *     parent links. If there are multiple such commits, the one with the smallest sum
     *     of distances from both input commits is chosen.
     * </p>
     *
     * @param from The first commit.
     * @param into The second commit.
     * @param loader The loader to use to load parent commits.
     * @return The best common ancestor, or null if no common ancestor exists.
     * @throws IOException If some commit cannot be loaded, or the repository is corrupted.
     */
    public static Commit findCommonAncestor(Commit from, Commit into, MinigitObjectLoader loader) throws IOException {
        Map<String, Integer> distancesToFromCommit = new HashMap<>();
        Deque<String> bfsQueue = new ArrayDeque<>();
        distancesToFromCommit.put(from.miniGitSha1(), 0);
        bfsQueue.add(from.miniGitSha1());

        // First BFS: compute distances from the first commit to all its ancestors.
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

        // Second BFS: walk ancestors of the second commit and look for the best common one.
        while (!bfsQueue.isEmpty()) {
            String commitToProcessHash = bfsQueue.removeFirst();
            int distanceToProcessedCommit = distancesToIntoCommit.get(commitToProcessHash);

            // No need to continue from commits that are already worse than the best found ancestor.
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

    /**
     * Load a commit with the specified hash.
     *
     * @param hash The hash of the commit to load.
     * @param loader The loader to use to load the object.
     * @return The loaded commit.
     * @throws IOException If the object cannot be loaded, or it is not a commit.
     */
    private static Commit loadCommit(String hash, MinigitObjectLoader loader) throws IOException {
        MiniGitObject obj = loader.loadFromInternal(hash);
        if (!(obj instanceof Commit commit)) {
            throw new IOException("Repo corrupted. This is not a commit: " + hash);
        }
        return commit;
    }
}
