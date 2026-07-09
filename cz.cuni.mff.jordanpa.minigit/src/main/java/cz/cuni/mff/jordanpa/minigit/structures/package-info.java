/**
 * Contains all structures used by the commands - {@link cz.cuni.mff.jordanpa.minigit.structures.Blob}, {@link cz.cuni.mff.jordanpa.minigit.structures.Commit}, {@link cz.cuni.mff.jordanpa.minigit.structures.Repository}, etc.
 *
 * <p>
 *     All the git-like business logic is here. Most of the classes are immutable and can be used separately.
 *     At runtime, they use CWD-relative paths, and after saving, they use some specified root paths, mostly the repository-in-use root.
 *     The commands use these structures to do their work.
 * </p>
 */
package cz.cuni.mff.jordanpa.minigit.structures;
