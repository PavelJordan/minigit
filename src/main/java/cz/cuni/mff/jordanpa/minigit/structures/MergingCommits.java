package cz.cuni.mff.jordanpa.minigit.structures;

public record MergingCommits(String fromCommit, Head intoHead, String intoCommit) { }
