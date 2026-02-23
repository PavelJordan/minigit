package cz.cuni.mff.jordanpa.minigit.structures;

public interface Sha1Hashable {

    /**
     * Returns the hash of this object.
     * Should be calculated only once, for example, when the object is first created.
     */
    String sha1();
}
