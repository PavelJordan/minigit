package cz.cuni.mff.jordanpa.minigit.misc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public final class StringComparer {
    public record Change(String oldString, String newString) { }

    public static List<Change> compare(String oldString, String newString) {
        return List.of(new Change("Not implemented yet old", "Not implemented yet new"));
    }
}
