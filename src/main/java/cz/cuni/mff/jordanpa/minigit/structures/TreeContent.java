package cz.cuni.mff.jordanpa.minigit.structures;

public sealed interface TreeContent extends Sha1Hashable permits Tree, Blob {
}
