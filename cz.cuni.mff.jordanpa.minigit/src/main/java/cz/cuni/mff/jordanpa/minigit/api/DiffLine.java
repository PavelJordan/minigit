package cz.cuni.mff.jordanpa.minigit.api;

/**
 * One line of diff, as returned by diff methods of MiniGitApi.
 *
 * <p>
 *     The diff API return the whole file, with new and deleted files marked accordingly.
 *     This represents one line of that.
 * </p>
 *
 * @param type How the line changed between the old and the new version.
 * @param line The line contents.
 */
public record DiffLine(Type type, String line) {

    /**
     * Type of change of one diff line.
     */
    public enum Type {
        SAME,
        DELETED,
        ADDED
    }
}
