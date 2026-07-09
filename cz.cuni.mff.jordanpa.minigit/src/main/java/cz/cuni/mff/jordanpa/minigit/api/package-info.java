/**
 * API of MiniGit. Contains {@link cz.cuni.mff.jordanpa.minigit.api.MiniGitApi} facade and its result types.
 *
 * <p>
 *     Unlike CLI commands, the API works with a single repository - it does not use all repositories from the project manager.
 * </p>
 *
 * <p>
 *     AI helped me with matching the API commands to the CLI commands by me designing the project structure + facade pattern,
 *     and AI drafting the first version of the facade, which I then refactored.
 *     The code in API commands is mostly copied from my old code in commands anyway.
 * </p>
 */
package cz.cuni.mff.jordanpa.minigit.api;
