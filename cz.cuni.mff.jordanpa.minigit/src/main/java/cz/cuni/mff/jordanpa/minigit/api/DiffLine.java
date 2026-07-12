package cz.cuni.mff.jordanpa.minigit.api;

/**
 * One line of diff, as returned by diff methods of MiniGitApi.
 *
 * <p>
 *     The diff API return the whole file, with new and deleted files marked accordingly.
 *     This represents one line of that. Diffs spanning multiple files separate them
 *     with HEADER lines carrying the file path.
 * </p>
 *
 * @param type How the line changed between the old and the new version.
 * @param line The line contents, or the file path for a HEADER line.
 */
public record DiffLine(Type type, String line) {

    /**
     * Type of change of one diff line.
     */
    public enum Type {
        SAME,
        DELETED,
        ADDED,
        HEADER
    }
}
