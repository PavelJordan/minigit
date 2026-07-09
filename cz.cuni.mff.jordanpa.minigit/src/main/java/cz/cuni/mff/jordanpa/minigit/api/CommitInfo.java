package cz.cuni.mff.jordanpa.minigit.api;

import cz.cuni.mff.jordanpa.minigit.misc.Author;

import java.util.Date;
import java.util.List;

/**
 * Information about one commit in the history - returned by log method of MiniGitApi.
 *
 * @param hash The hash of the commit.
 * @param parents Hashes of the parent commits - empty for the first commit, two for a merge commit, otherwise one.
 * @param author The author of the commit.
 * @param date The date of the commit.
 * @param message The commit message.
 * @param branches Names of the branches pointing to this commit.
 * @param tags Names of the tags pointing to this commit.
 * @param isHead Whether HEAD currently points to this commit.
 */
public record CommitInfo(String hash, List<String> parents, Author author, Date date, String message,
                         List<String> branches, List<String> tags, boolean isHead) { }
