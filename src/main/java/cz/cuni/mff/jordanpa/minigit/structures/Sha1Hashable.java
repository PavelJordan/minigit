package cz.cuni.mff.jordanpa.minigit.structures;

import java.util.Formatter;

public interface Sha1Hashable {

    /**
     * Returns the data of this object.
     * Should be calculated only once, for example, when the object is first created.
     */
    String miniGitSha1();

    default String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
