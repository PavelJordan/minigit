package cz.cuni.mff.jordanpa.minigit.api;

import java.util.List;

/**
 * One chunk of file diff, as returned by diff methods of MiniGitApi.
 *
 * <p>
 *     Chunk format: from line oldStart to line oldStart + oldLines.size() are replaced by the given new lines. One of the sides can
 *     be empty - an empty old side = insertion, an empty new side = deletion.
 * </p>
 *
 * @param oldStart 0-based index of the first replaced line in the old version.
 * @param oldLines The replaced lines from the old version.
 * @param newStart 0-based index of the first replacement line in the new version.
 * @param newLines The replacement lines from the new version.
 */
public record DiffChunk(long oldStart, List<String> oldLines, long newStart, List<String> newLines) { }
