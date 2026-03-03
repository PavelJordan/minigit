package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.Commit;
import cz.cuni.mff.jordanpa.minigit.structures.MinigitObjectLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Merger {
    public record MergeResult(Map<Path, String> mergeIndexWithoutConflicts, List<Blob> mergeIndexBlobsToSave, List<Path> conflicts) { }
    public static MergeResult merge(Commit from, Commit into, MinigitObjectLoader objectLoader) {
        // Three-way-merge
        return null;
    }
    private static ByteArrayInputStream threeWayMergeArrayStreams(InputStream base, InputStream ours, InputStream theirs) {
        return null;
    }
}
