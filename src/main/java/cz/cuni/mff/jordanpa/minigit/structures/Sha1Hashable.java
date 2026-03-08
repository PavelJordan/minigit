package cz.cuni.mff.jordanpa.minigit.structures;

import java.util.Formatter;

/**
 * Represents an object that can be uniquely identified by its SHA1 hash, and where if objects
 * have the same data, they should have the same hash.
 */
public interface Sha1Hashable {

    /**
     * Returns the SHA1 hash of the object. It should be unique for each object that has different data,
     * and the same when the data is the same.
     * Should be calculated only once, for example, when the object is first created, then cached.
     */
    String miniGitSha1();

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * <p>
     *     This is the SHA-1 hash used in MiniGit and files, not the byte array, for readability.
     * </p>
     *
     * @param hash the byte array to convert
     * @return the hexadecimal string representation of the byte array.
     */
    default String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
