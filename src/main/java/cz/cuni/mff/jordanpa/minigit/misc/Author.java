package cz.cuni.mff.jordanpa.minigit.misc;

/**
 * Representation of a commit author.
 */
public record Author(String name, String email) {
    public static Author getDefaultAuthor() {
        return new Author("Unknown", "unknown@unknown");
    }
}
