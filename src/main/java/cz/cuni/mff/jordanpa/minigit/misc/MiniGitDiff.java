package cz.cuni.mff.jordanpa.minigit.misc;

import java.util.List;

public final class MiniGitDiff {
    public record DiffResult(Integer replaceFrom, Integer replaceTo, List<String> replaceByLines) {}
    public static List<DiffResult> diff(List<String> from, List<String> to) {
        throw new RuntimeException("Not implemented");
    }
}
