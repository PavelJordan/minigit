package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public record MergeResult(MergeResultType type, HashMap<Path, String> newStagedIndex, HashMap<Path, Blob> mergeIndexBlobsToSave, List<Path> conflicts) { }
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

        throw new RuntimeException("Not implemented");
    }

    private static ByteArrayInputStream threeWayMergeArrayStreams(InputStream base, InputStream ours, InputStream theirs) {
        return null;
    }
}
