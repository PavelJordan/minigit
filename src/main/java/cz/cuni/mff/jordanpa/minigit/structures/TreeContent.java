package cz.cuni.mff.jordanpa.minigit.structures;

/**
 * Represents a content of a tree - either a blob or a tree (subtree)
 */
public sealed interface TreeContent extends Sha1Hashable permits Tree, Blob {
}
