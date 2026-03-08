package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;
import cz.cuni.mff.jordanpa.minigit.misc.Merger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static cz.cuni.mff.jordanpa.minigit.misc.FileHelper.getFileStatusesFromComparison;

/**
 * Represents a MiniGit repository with its mutable state and access to the immutable object database.
 *
 * <p>
 *     The repository manages the currently loaded runtime data, such as HEAD, staged index,
 *     references, current author, ignored files, and merging state. It also provides the main
 *     operations used by commands.
 * </p>
 * <p>
 *     While blobs, trees, and commits form the immutable persistent core of MiniGit,
 *     Repository provides the mutable layer above them.
 * </p>
 */
public final class Repository implements MinigitObjectLoader {

    /**
     * Type of change between two compared versions of a file.
     */
    public enum FileStatusType {
        SAME,
        MODIFIED,
        DELETED,
        NEW
    }

    /**
     * Result of starting or performing a merge.
     */
    public enum MergeStatus {
        /**
         * The merge was started, but conflicts were detected and must be resolved manually.
         */
        CONFLICT,

        /**
         * The merge was successfully applied.
         */
        APPLIED,

        /**
         * The merge could not be started or completed because the input/state was invalid.
         */
        INVALID,
    }

    private static final String REF_TYPE_BRANCH = "BRANCH";
    private static final String REF_TYPE_TAG = "TAG";

    public record FileStatus(Path path, FileStatusType status) {}

    /**
     * Map data -> actual object. Only the objects that are currently in runtime memory.
     */
    private final HashMap<String, MiniGitObject> objects = new HashMap<>();

    /**
     * Map human-readable name -> data for branches and tags. Only the references that are currently in runtime memory.
     */
    private final HashMap<String, String> branches = new HashMap<>();
    private final HashMap<String, String> tags = new HashMap<>();
    private Author currentAuthor = null;
    private final Path mainRepoPath;
    private final Path repoInternalPath;
    private final Path objectsPath;
    private final Path indexPath;
    private final Path headPath;
    private final Path authorPath; // Not in remote!
    private final Path refPath;
    private final Path ignoredPath;
    private final Path mergingPath;
    private MergingCommits mergingCommits = null;
    private Head head;

    /**
     * Map path -> data. In memory are relative paths
     */
    private HashMap<Path, String> stagedIndex;

    private final List<String> ignored = new ArrayList<>();

    /**
     * Create a repository object for the specified .minigit directory.
     *
     * <p>
     *     This only initializes the repository paths. To load the actual repository contents from disk,
     *     use {@link #load(Path)}.
     * </p>
     *
     * @param path The path to the .minigit directory.
     */
    public Repository(Path path) {
        this.repoInternalPath = path;
        this.mainRepoPath = path.resolve("../").normalize();
        this.objectsPath = path.resolve("objects");
        this.indexPath = path.resolve("index");
        this.headPath = path.resolve("HEAD");
        this.authorPath = path.resolve("author");
        this.refPath = path.resolve("refs");
        this.mergingPath = path.resolve("merging");
        this.ignoredPath = path.resolve("../.minigitignore").normalize();
    }

    /**
     * Load the repository from the given path (that should be the .minigit folder).
     *
     * @param loadFrom The .minigit folder of the repository.
     * @return The repository object.
     * @throws IOException If the repository does not exist, or some other files are corrupted.
     *
     * <p>
     *     The repository files are lazy-loaded from disk, where possible - so this is rather quick.
     *     Searches up to 10 parent directories for the repository if the loadFrom is not .minigit folder.
     * </p>
     */
    public static Repository load(Path loadFrom) throws IOException {
        // Search in parent directories
        final int toSearch = 10;
        for (int i = 0; !Files.exists(loadFrom) && i < toSearch; i++) {
            loadFrom = Path.of("../").resolve(loadFrom).normalize();
        }

        if (!Files.exists(loadFrom)) {
            throw new IOException("Repository does not exist. Searched up to " + loadFrom);
        }

        Repository repo = new Repository(loadFrom);

        repo.loadHead();
        repo.loadRef();
        repo.loadIndex();
        repo.loadCurrentAuthor();
        repo.loadIgnored();
        repo.loadMerging();
        return repo;
    }

    /**
     * Save the index, head, references (branches and tags), current author merging information, and all objects
     * set up through {@link #storeInternally(MiniGitObject)} to the disk.
     *
     * <p>
     *     Creates all necessary files for the directory, including creating the .minigit folder if it does not exist.
     *     If you don't call this method, the changes you made in other methods will not be saved.
     * </p>
     * <p>
     *     Only the user files in repo will stay (in the case you checked out a commit, for example).
     *     The files are saved with repo-relative paths, whereas the runtime-representation is CWD relative paths.
     * </p>
     *
     * @throws IOException in the case you don't have the permissions, or some files are corrupted (for example,
     * they are already directories with the same name).
     *
     */
    public void save() throws IOException {

        // Ensure we have .minigit folder and .minigit/objects folder.
        if (!Files.exists(objectsPath)) {
            Files.createDirectories(objectsPath);
        }

        // save objects
        for (Map.Entry<String, MiniGitObject> obj : objects.entrySet()) {
            MiniGitObject.saveObjectBasedOnHash(objectsPath, obj.getValue());
        }

        // save mutable data
        saveIndex();
        saveHead();
        saveRef();
        saveCurrentAuthor();
        saveMerging();
    }

    /**
     * @return The root path of this repository.
     */
    public Path getRootPath() {
        return mainRepoPath;
    }

    /**
     * Set up the MiniGitObject to be stored in the repository.
     *
     * <p>
     *     The objects are saved in a map - the key is its hash.
     *     If the hash already exists, the object is not stored.
     *     The object is saved into the file system only after the {@link #save()} method is called.
     * </p>
     *
     * @param obj The object to store.
     */
    public void storeInternally(MiniGitObject obj) {
        if (!objects.containsKey(obj.miniGitSha1())) {
            objects.put(obj.miniGitSha1(), obj);
        }
    }

    /**
     * Load an object with the specified hash or name.
     *
     * <p>
     *     If the name exists, it first retrieves the hash corresponding to that branch/tag.
     *     Otherwise, it loads the object with that hash directly. You can then use `instanceof` to check the type of the object loaded.
     *     Once loaded, the next `loadFromInternal` with the same hash only returns the object from the runtime map.
     * </p>
     * <p>
     *     As the objects on the disks have repo-relative paths, the returned object contains a path converted to CWD-relative path.
     * </p>
     *
     * @param hashOrName The hash or name (branch/tag) of the object to load.
     * @return The object, or null if it does not exist.
     * @throws IOException If the repository is corrupted - for example, the object file has an unknown header.
     */
    public MiniGitObject loadFromInternal(String hashOrName) throws IOException {
        String hash;

        // Resolve name
        if (branches.containsKey(hashOrName)) {
            hash = branches.get(hashOrName);
        }
        else {
            hash = tags.getOrDefault(hashOrName, hashOrName);
        }

        if (!objects.containsKey(hash)) {
            // The object was not yet loaded
            try {
                MiniGitObject obj = MiniGitObject.getObjectBasedOnHash(objectsPath, hash);
                objects.put(hash, obj);
            }
            catch(IOException e) {
                IO.println(e);
                IO.println("Error loading object from repository. Check your permissions and data and try again.");
                return null;
            }
        }

        return objects.get(hash);
    }

    /**
     * Add the specified file into the staged files list (index).
     *
     * <p>
     *     The staged files list (index) contains relative paths to CWD at runtime,
     *     while, in the file system, the paths are repo-relative (once you call {@link #save()}.
     *     If the file does not exist, it is deleted from the index (if it was there).
     *     Also saves the file as a blob into the repository database (again, commited via {@link #save()}, otherwise it would be lost).
     * </p>
     * <p>
     *     Works only if the file is in the repository root path/subpath
     * </p>
     *
     * @param file To add to the index.
     * @return Whether the file was added to the index/removed from the index successfully. Prints an error message if some IO exception occurs.
     */
    public boolean addToIndex(Path file) {

        // Ignore files not belonging to the repository - useful when working with multiple repositories.
        if (!isInRepo(file)) {
            return false;
        }

        try {
            Path abs = safeAbsFromCwdRelative(file.normalize());
            Path normalized = cwdAbsPath().relativize(abs).normalize();

            // If the file is not in the file system, try to delete it from the index
            if (!Files.exists(abs) || FileHelper.isExcluded(normalized, getIgnored())) {
                stagedIndex.remove(normalized);
                return true;
            }

            // If the file is in the file system, the blob is created, staged, and saved.
            Blob blob = new Blob(abs);
            stagedIndex.put(normalized, blob.miniGitSha1());
            storeInternally(blob);
            return true;
        }
        catch(IOException e) {
            IO.println(e);
            IO.println("Error adding file " + file + " to index. Continuing...");
            return false;
        }
    }

    /**
     * Get the index (the tracked files/staged files) in this repository.
     *
     * <p>
     *     As the files are staged, their blobs are already saved.
     * </p>
     *
     * @return Copy of the map "path -> blob hash".
     */
    public Map<Path, String> getTrackedFiles(){
        return Map.copyOf(stagedIndex);
    }

    /**
     * Restore the staged index to the state of the commit currently pointed to by HEAD.
     *
     * @throws IOException If retrieving the commit index fails.
     */
    public void unstageToLastCommit() throws IOException {
        stagedIndex = head.getCommitIndex(this);
    }

    /**
     * Build an index of the current working directory.
     *
     * @return Map of CWD-relative paths to blob hashes of the current working tree files.
     * @throws IOException If reading the files in the working directory fails.
     */
    public HashMap<Path, String> getIndexOfWorkingDirectory() throws IOException {
        List<Path> paths = FileHelper.getAllFiles(mainRepoPath, getIgnored());
        HashMap<Path, String> workingDirIndex = new HashMap<>();
        for(Path path : paths) {
            String hash = new Blob(path).miniGitSha1();
            workingDirIndex.put(path.normalize(), hash);
        }
        return workingDirIndex;
    }

    /**
     * Compare the staged index to the last commit pointed to by HEAD.
     *
     * @return List of file statuses describing the differences.
     * @throws IOException If the commit index cannot be retrieved.
     */
    public List<FileStatus> getStagedToLastCommitStatus() throws IOException {
        HashMap<Path, String> commitIndex = head.getCommitIndex(this);
        return getFileStatusesFromComparison(stagedIndex, commitIndex);
    }

    /**
     * Compare the current working directory to the staged index.
     *
     * @return List of file statuses describing the differences.
     * @throws IOException If the working directory index cannot be built.
     */
    public List<FileStatus> getWorkingToIndexStatus() throws IOException {
        HashMap<Path, String> workingDirIndex = getIndexOfWorkingDirectory();
        return getFileStatusesFromComparison(workingDirIndex, stagedIndex);
    }

    /**
     * Check whether the working tree contains staged or unstaged changes.
     *
     * @return True if the working tree is dirty, false otherwise.
     * @throws IOException If the working tree or commit status cannot be computed.
     */
    public boolean workingTreeDirty() throws IOException {
        List<Repository.FileStatus> statusesWorking = getWorkingToIndexStatus();
        List<Repository.FileStatus> statusesCommited = getStagedToLastCommitStatus();
        return !statusesWorking.stream().allMatch(status -> status.status() == FileStatusType.SAME || status.status() == FileStatusType.NEW)
                || !statusesCommited.stream().allMatch(status -> status.status() == FileStatusType.SAME);
    }

    /**
     * @return The current HEAD, which is either unset, detached to commit, or following branch, as loaded from its file.
     */
    public Head getHead(){
        return head;
    }

    /**
     * Get the commit hash currently pointed to by HEAD.
     *
     * @return The commit hash, or null if HEAD is unset.
     */
    public String getHeadCommitHash() {
        if (head.type() == Head.Type.BRANCH) {
            return branches.get(head.data());
        }
        else if (head.type() == Head.Type.COMMIT) {
            return head.data();
        }
        else {
            return null;
        }
    }

    /**
     * Set HEAD to the specified commit, resulting in its DETACHED state.
     * @param commit The commit to set HEAD to. The commit must be in the repository database
     *               (so, use the {@link #storeInternally(MiniGitObject)} method to save it there if the commit is new).
     */
    public void setHeadToCommit(Commit commit) {
        head = new Head(Head.Type.COMMIT, commit.miniGitSha1());
    }

    /**
     * Set HEAD to the specified branch, resulting in HEAD following that branch.
     * @param checkoutTo The branch to set HEAD to. If the branch does not exist, nothing happens.
     */
    public void setHeadToBranch(String checkoutTo) {
        if (!branches.containsKey(checkoutTo)) {
            return;
        }
        head = new Head(Head.Type.BRANCH, checkoutTo);
    }

    /**
     * Checks out a tree.
     *
     * <p>
     *     This means that the repository first checks whether the CWD is clean - no staged/unstaged changes.
     *     After that check is successful, the tree is checked out - its contents are copied into the CWD.
     * </p>
     * @param treeToRestore Tree to put contents from into a file system. Its root has to be equal to this repository root.
     * @throws IOException If the tree is not in the repository, the CWD is not clean, or writing the files fails.
     */
    public void checkoutTree(Tree treeToRestore) throws IOException {
        List<FileStatus> statusesStaged = getWorkingToIndexStatus();
        List<FileStatus> statusesCommited = getStagedToLastCommitStatus();

        // Check that the CWD is clean
        if (statusesStaged.stream().anyMatch(status -> status.status() != FileStatusType.SAME && status.status() != FileStatusType.NEW)) {
            throw new IOException("Cannot restore tree. There are unstaged uncommitted changes.");
        }
        if (statusesCommited.stream().anyMatch(status -> status.status() != FileStatusType.SAME && status.status() != FileStatusType.NEW)) {
            throw new IOException("Cannot restore tree. There are staged uncommitted changes.");
        }

        Tree CurrentRoot = Tree.buildTree(stagedIndex, mainRepoPath).getLast();

        // Check that the tree is in the repository
        if (loadFromInternal(CurrentRoot.miniGitSha1()) == null) {
            throw new IOException("Cannot restore tree. Current root tree is not in repository. Use tree command to build it.");
        }

        // If the tree is already checked out, do nothing (which means the current index is the same as the tree index)
        if (CurrentRoot.miniGitSha1().equals(treeToRestore.miniGitSha1())) {
            IO.println("Tree is already checked out.");
            return;
        }

        // Delete all files in the CWD that are currently tracked (committed in the last commit - we can retrieve them later,
        // but it might not be in the tree we are checking out - we want to have only the new files)
        for (FileStatus status : statusesStaged.stream().filter(s -> s.status() == FileStatusType.SAME).toList()) {
            if (status.status() == FileStatusType.SAME) {
                IO.println("Deleting tracked file " + status.path());
                Files.delete(safeAbsFromCwdRelative(status.path()));
            }
        }

        // Set new staged files (so they now match the tree)
        stagedIndex = treeToRestore.getIndex(this);
        IO.println("Restoring tree...");

        // Put the tree contents into the CWD (root being the repo root)
        treeToRestore.forcedCheckoutTree(this);
    }

    /**
     * @return A copy of the map "branch name -> commit hash".
     */
    public Map<String, String> getBranches() {
        return Map.copyOf(branches);
    }

    /**
     * Simply sets a branch to a commit hash. If the branch exists, it is overwritten (and the user is warned in the console).
     *
     * <p>
     *     The change is, as with most other methods, applied only after using {@link #save()}.
     * </p>
     *
     * @param arg The name of the branch to set.
     * @param hash The hash of the commit the branch points to.
     */
    public void setBranch(String arg, String hash){
        if (branches.containsKey(arg)) {
            IO.println("Branch " + arg + " moved from: " + branches.get(arg) + " to " + hash);
        }
        branches.put(arg, hash);
    }

    /**
     * Deletes the specified branch (if it exists - otherwise does nothing).
     *
     * <p>
     *     Of course, the changes are applied only after calling {@link #save()},
     *     as with most of the other methods.
     * </p>
     *
     * @param branchName The branch name to delete
     */
    public void deleteBranch(String branchName) {
        branches.remove(branchName);
    }

    /**
     * Check whether a branch with the specified name exists.
     * @param name The name of the branch to check.
     * @return True if the branch exists, false otherwise.
     */
    public boolean isBranch(String name) {
        return branches.containsKey(name);
    }

    /**
     * @return A copy of the map "tag name -> commit hash".
     */
    public Map<String, String> getTags() {
        return Map.copyOf(tags);
    }

    /**
     * Simply sets a tag to a commit hash. If the tag exists, it is overwritten.
     *
     * <p>
     *     The change is, as with most other methods, applied only after using {@link #save()}.
     * </p>
     *
     * @param arg The name of the tag to set.
     * @param data The hash of the commit the tag points to.
     */
    public void setTag(String arg, String data) {
        tags.put(arg, data);
    }

    /**
     * Deletes the specified tag (if it exists - otherwise does nothing).
     *
     * <p>
     *     Of course, the changes are applied only after calling {@link #save()},
     * </p>
     *
     * @param tagName The tag name to delete
     */
    public void deleteTag(String tagName) {
        tags.remove(tagName);
    }

    /**
     * Resolve a branch or tag name to the commit hash it points to.
     *
     * @param ref The branch or tag name.
     * @return The referenced commit hash, or null if the reference does not exist.
     */
    public String getHashFromRef(String ref) {
        if (branches.containsKey(ref)) {
            return branches.get(ref);
        }
        else return tags.getOrDefault(ref, null);
    }

    /**
     * @return The current author of the repository, or null if no author is set.
     */
    public Author getCurrentAuthor() {
        return currentAuthor;
    }

    /**
     * Set the current author of the repository. On the next save, it will be persisted.
     * @param author The author to set.
     */
    public void setCurrentAuthor(Author author) {
        currentAuthor = author;
    }

    /**
     * Get the list of ignored patterns, as loaded from the.minigitignore file, including the by-default ignored .minigit folder.
     * @return The list of ignored patterns
     */
    public List<String> getIgnored() {
        return ignored;
    }

    /**
     * @return Whether there is currently a merging in progress.
     */
    public boolean isMerging() {
        return mergingCommits != null;
    }

    /**
     * @return The currently active merging information, or null if no merging is in progress.
     */
    public MergingCommits getMergingCommits() {
        return mergingCommits;
    }

    /**
     * Start a merge operation and try to apply it.
     *
     * @param mergingCommits The commits and head information to merge.
     * @return The merge status after the merge attempt.
     * @throws IOException If the merge operation fails due to an IO problem.
     */
    public MergeStatus startMerging(MergingCommits mergingCommits) throws IOException {
        if (isMerging()) {
            IO.println("Cannot start merging. There is already a merging in progress. Use stop-merge first, or apply-merge.");
            return MergeStatus.INVALID;
        }
        this.mergingCommits = mergingCommits;
        return merge();
    }

    /**
     * Finish the current merge by creating a merge commit from the staged index.
     *
     * @param message The commit message of the resulting merge commit.
     * @return Whether the merge commit was created successfully.
     * @throws IOException If reading objects or creating the merge commit fails.
     */
    public boolean mergeFromIndex(String message) throws IOException {
        if (!isMerging()) {
            IO.println("Cannot merge. There is no merging in progress.");
            return false;
        }

        // Load the commits we are merging
        MergingCommits merging = mergingCommits;
        String intoCommitHash = merging.intoCommit();
        String fromCommitHash = merging.fromCommit();
        Head intoHead = merging.intoHead();

        // If user moved head, warn them
        if (!loadFromInternal(intoHead.data()).miniGitSha1().equals(intoCommitHash)) {
            IO.println("Warning: merging into a commit that is not the current head. This is not recommended.");
        }

        // Build tree from the staged index, which will now be the merge result
        List<Tree> newTrees = Tree.buildTree(stagedIndex, mainRepoPath);

        // Create the commit, save it, update the head, and conclude the merge.
        Commit result = new Commit(newTrees.getLast().miniGitSha1(), intoCommitHash, fromCommitHash, getCurrentAuthor(), message, Date.from(Instant.now()));
        newTrees.forEach(this::storeInternally);
        storeInternally(result);
        updateHeadToCommit(intoHead, result);
        stopMerge();
        return true;
    }

    /**
     * Stop the current merge and clear the in-memory merging information.
     */
    public void stopMerge() {
        this.mergingCommits = null;
    }

    /**
     * Update the specified head to point to the given commit.
     *
     * @param intoHead The head to update.
     * @param result The commit to point the head to.
     */
    private void updateHeadToCommit(Head intoHead, Commit result) {
        if (intoHead.type() == Head.Type.BRANCH) {
            setBranch(intoHead.data(), result.miniGitSha1());
            setHeadToBranch(intoHead.data());
        }
        else if (intoHead.type() == Head.Type.COMMIT) {
            setHeadToCommit(result);
        }
        else {
            throw new UnsupportedOperationException("Invalid head type while moving head: " + intoHead.type());
        }
    }

    /**
     * Perform the actual merge according to the current merging information.
     *
     * @return The merge status after the merge attempt.
     * @throws IOException If loading objects, checking out files, or writing merge data fails.
     */
    private MergeStatus merge() throws IOException{

        IO.println("Will merge from " + mergingCommits.fromCommit());
        IO.println("To " + mergingCommits.intoHead());

        // Get the commits we are merging
        MiniGitObject oursObject = loadFromInternal(mergingCommits.intoCommit());
        MiniGitObject theirsObject = loadFromInternal(mergingCommits.fromCommit());
        if (!(oursObject instanceof Commit oursCommit) || !(theirsObject instanceof Commit theirsCommit)) {
            IO.println("Cannot merge. Commits are not of type commit.");
            stopMerge();
            return MergeStatus.INVALID;
        }

        // Try to merge. Based on the result, decide.
        Merger.MergeResult MR = Merger.merge(theirsCommit, oursCommit, this);
        switch (MR.type()) {

            case ERROR -> {
                IO.println("Cannot merge. Merging failed.");
                stopMerge();
                return MergeStatus.INVALID;
            }

            case NOTHING_TO_DO -> {
                IO.println("Nothing to merge. Commits are identical.");
                stopMerge();
                return MergeStatus.APPLIED;
            }

            case FAST_FORWARD -> {
                IO.println("Fast-forward merge.");
                Head mergeHead = mergingCommits.intoHead();
                checkoutTree((Tree)loadFromInternal(theirsCommit.getTreeHash()));
                updateHeadToCommit(mergeHead, theirsCommit);
                stopMerge();
                return MergeStatus.APPLIED;
            }

            case MERGED_INTO_FS -> {

                // In this case, we are making a real merge and will have to make a merge commit.
                // In other cases, it is just moving branches around, or nothing (or error).
                // Now, almost certainly, something was written into the file system
                IO.println("Regular merge.");

                // Get which files should now be staged. See how the merge is concluded in Merger to understand.
                stagedIndex = MR.newStagedIndex();

                // Store the blobs that should be stored in the repo database
                MR.mergeIndexBlobsToSave().values().forEach(this::storeInternally);

                // If there are no conflicts, say that the merge was applied, and the user can do merge-apply right away
                if (MR.conflicts().isEmpty()) {
                    return MergeStatus.APPLIED;
                }
                // If there are conflicts, inform the user and say it to the caller.
                else {
                    IO.println("Conflicts detected. MiniGit asks you to resolve them:");
                    for (var conflict : MR.conflicts()) {
                        IO.println(conflict.toString());
                    }
                    return MergeStatus.CONFLICT;
                }
            }
        }

        // We should not get here...
        return MergeStatus.CONFLICT;
    }

    /**
     * Load merging information from the disk, if present.
     */
    private void loadMerging(){
        if (!Files.exists(mergingPath)) {
            return;
        }
        try (var in = Files.newBufferedReader(mergingPath)) {
            String fromCommit = in.readLine();
            String headType = in.readLine();
            String headData = in.readLine();
            String intoCommit = in.readLine();
            mergingCommits = new MergingCommits(fromCommit, new Head(Head.Type.valueOf(headType), headData), intoCommit);
        }
        catch (Exception e) {
            IO.println("Error loading merging information. Aborting merge");
            mergingCommits = null;
        }
    }

    /**
     * Save the current merging information to disk.
     *
     * @throws IOException If writing the merging file fails.
     */
    private void saveMerging() throws IOException {
        if (mergingCommits == null) {
            Files.deleteIfExists(mergingPath);
            return;
        }
        if (!Files.exists(mergingPath)) {
            Files.createDirectories(mergingPath.getParent());
            Files.createFile(mergingPath);
        }
        try (var out = Files.newBufferedWriter(mergingPath)) {
            out.write(mergingCommits.fromCommit());
            out.newLine();
            out.write(mergingCommits.intoHead().type().name());
            out.newLine();
            out.write(mergingCommits.intoHead().data());
            out.newLine();
            out.write(mergingCommits.intoCommit());
        }
    }

    /**
     * @return The absolute normalized path to the repository root.
     */
    private Path mainRepoAbsPath() { return mainRepoPath.toAbsolutePath().normalize(); }

    /**
     * @return The absolute normalized path to the current working directory.
     */
    private Path cwdAbsPath()  { return Path.of("").toAbsolutePath().normalize(); }

    /**
     * Resolve a CWD-relative path to an absolute path and ensure it stays inside the repository.
     *
     * @param p The CWD-relative path.
     * @return The absolute normalized path.
     * @throws IOException If the path escapes the repository.
     */
    private Path safeAbsFromCwdRelative(Path p) throws IOException {
        Path abs = cwdAbsPath().resolve(p).normalize();
        if (!abs.startsWith(mainRepoAbsPath())) {
            throw new IOException("Path escapes repository: " + p);
        }
        return abs;
    }

    /**
     * Check whether a CWD-relative path belongs to this repository.
     *
     * @param p The path to check.
     * @return True if the path is inside the repository, false otherwise.
     */
    private boolean isInRepo(Path p) {
        Path abs = cwdAbsPath().resolve(p).normalize();
        return abs.startsWith(mainRepoAbsPath());
    }

    /**
     * Convert a CWD-relative path to a repository-root-relative path.
     *
     * @param cwdRelative The CWD-relative path.
     * @return The corresponding repository-root-relative path.
     * @throws IOException If the path escapes the repository.
     */
    private Path getPathRelativeToRepo(Path cwdRelative) throws IOException {
        Path abs = safeAbsFromCwdRelative(cwdRelative);
        return mainRepoAbsPath().relativize(abs).normalize();
    }

    /**
     * Loads the index from the disk (path -> blob data).
     */
    private void loadIndex() throws IOException {
        stagedIndex = new HashMap<>();
        try(Scanner scanner = new Scanner(indexPath)) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                int delimiterIndex = nextLine.indexOf(' ');
                String hash = nextLine.substring(0, delimiterIndex);
                Path path = Path.of(nextLine.substring(delimiterIndex + 1)).normalize();
                Path resolvedPath = FileHelper.getRelativePathToDirectory(mainRepoPath.resolve(path), Path.of("./"));
                stagedIndex.put(resolvedPath, hash);
            }
        }
    }

    /**
     * Saves the index to the disk (path -> blob data).
     */
    private void saveIndex() throws IOException {
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath.getParent());
            Files.createFile(indexPath);
        }
        if (stagedIndex == null) {
            return;
        }
        try (var out = Files.newBufferedWriter(indexPath)) {
            for (Map.Entry<Path, String> entry : stagedIndex.entrySet()) {
                out.write(entry.getValue() + " " + getPathRelativeToRepo(entry.getKey()) + "\n");
            }
        }
    }

    /**
     * Save HEAD to disk.
     *
     * @throws IOException If writing the HEAD file fails.
     */
    private void saveHead() throws IOException {
        if (!Files.exists(headPath)) {
            Files.createDirectories(headPath.getParent());
            Files.createFile(headPath);
            new Head(Head.Type.UNSET, null).write(headPath);
        }
        if (head != null) {
            head.write(headPath);
        }
    }

    /**
     * Load HEAD from disk.
     *
     * @throws IOException If reading the HEAD file fails.
     */
    private void loadHead() throws IOException {
        head = Head.loadHead(headPath);
    }

    /**
     * Save references (branches and tags) to disk.
     *
     * @throws IOException If writing the reference file fails.
     */
    private void saveRef() throws IOException {
        if (!Files.exists(refPath)) {
            Files.createDirectories(refPath.getParent());
            Files.createFile(refPath);
        }
        try (var out = Files.newBufferedWriter(refPath)) {
            for (Map.Entry<String, String> entry : branches.entrySet()) {
                out.write(REF_TYPE_BRANCH + " " + entry.getValue() + " " + entry.getKey() + "\n");
            }
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                out.write(REF_TYPE_TAG + " " + entry.getValue() + " " + entry.getKey() + "\n");
            }
        }
    }

    /**
     * Load references (branches and tags) from the disk.
     *
     * @throws IOException If the reference file contains invalid data.
     */
    private void loadRef() throws IOException {
        if (!Files.exists(refPath)) {
            return;
        }
        try (var in = Files.newBufferedReader(refPath)) {
            String line;
            while ((line = in.readLine()) != null) {
                int delimiterIndex = line.indexOf(' ');
                String nameOrTag = line.substring(0, delimiterIndex);
                String rest = line.substring(delimiterIndex + 1);
                int typeIndex = rest.indexOf(' ');
                String hash = rest.substring(0, typeIndex);
                String name = rest.substring(typeIndex + 1);
                if (nameOrTag.equals(REF_TYPE_TAG)) {
                    tags.put(name, hash);
                }
                else if (nameOrTag.equals(REF_TYPE_BRANCH)) {
                    branches.put(name, hash);
                }
                else {
                    throw new IOException("Invalid reference type: " + nameOrTag);
                }
            }
        }
    }

    /**
     * Load the current author from the disk.
     *
     * @throws IOException If reading the author file fails.
     */
    private void loadCurrentAuthor() throws IOException {
        if (!Files.exists(authorPath)) {
            return;
        }
        try(var in = Files.newBufferedReader(authorPath)) {
            String name = in.readLine();
            String email = in.readLine();
            currentAuthor = new Author(name, email);
        }
    }

    /**
     * Save the current author to disk.
     *
     * @throws IOException If writing the author file fails.
     */
    private void saveCurrentAuthor() throws IOException {
        if (currentAuthor == null) {
            return;
        }
        try(var out = Files.newBufferedWriter(authorPath)) {
            out.write(currentAuthor.name() + "\n");
            out.write(currentAuthor.email() + "\n");
        }
    }

    /**
     * Load ignored patterns from the .minigitignore file.
     *
     * @throws IOException If reading the ignored file fails.
     */
    private void loadIgnored() throws IOException {
        if (Files.exists(ignoredPath)) {
            try (var ignoredStream = Files.newBufferedReader(ignoredPath)) {
                ignoredStream.lines().filter(s -> !s.isEmpty()).forEach(ignored::add);
            }
        }
        ignored.add(repoInternalPath.normalize() + "/");
    }
}
