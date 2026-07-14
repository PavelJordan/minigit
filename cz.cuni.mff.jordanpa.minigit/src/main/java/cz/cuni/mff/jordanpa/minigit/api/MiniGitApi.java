package cz.cuni.mff.jordanpa.minigit.api;

import cz.cuni.mff.jordanpa.minigit.misc.*;
import cz.cuni.mff.jordanpa.minigit.structures.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Facade with MiniGit operations needed by GUI.
 *
 * <p>
 *     Each method is one CLI command, with the difference that it takes typed parameters, and returns typed result
 *     instead of printing to stdout. If an error happens, {@link MiniGitApiException} gets thrown with a
 *     pretty message for the user.
 * </p>
 * <p>
 *     The API uses single repository - it does not use project manager repositories.
 * </p>
 * <p>
 *     The description of the methods is short. If you want to know, how MiniGit behaves, look into
 *     Javadocs of CLI commands.
 * </p>
 * <p>
 *     Read package-info.java docs: AI helped me with matching the API commands to the CLI commands
 * </p>
 */
public final class MiniGitApi {

    private static final String FIRST_BRANCH_NAME = "master";

    /**
     * Path to the .minigit directory of the repository this API uses.
     */
    private final Path minigitDir;

    /**
     * Private constructor so the user has to use proper init/open methods.
     *
     * @param minigitDir The .minigit directory of the repository.
     */
    private MiniGitApi(Path minigitDir) {
        this.minigitDir = minigitDir;
    }

    /**
     * Open an existing repository.
     *
     * @param repoRoot The root directory of the repository with .minigit folder inside. Parent directories are searched too.
     * @return The API for that repository.
     * @throws MiniGitApiException If no repository was found.
     */
    public static MiniGitApi open(Path repoRoot) throws MiniGitApiException {
        Path minigitDir = repoRoot.resolve(".minigit");
        try {
            Repository.load(minigitDir);
        } catch (IOException e) {
            throw new MiniGitApiException("No MiniGit repository found at " + repoRoot.toAbsolutePath().normalize(), e);
        }
        return new MiniGitApi(minigitDir);
    }

    /**
     * Get api-friendly status of current repository.
     *
     * @return HEAD, staged changes, unstaged changes, and the merge in progress with its conflicts (if any).
     * @throws MiniGitApiException If repository cannot be read.
     */
    public StatusResult status() throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            List<Repository.FileStatus> unstaged = repo.getWorkingToIndexStatus().stream()
                    .filter(s -> s.status() != Repository.FileStatusType.SAME)
                    .toList();
            List<Repository.FileStatus> staged = repo.getStagedToLastCommitStatus().stream()
                    .filter(s -> s.status() != Repository.FileStatusType.SAME)
                    .toList();
            return new StatusResult(repo.getHead(), repo.getHeadCommitHash(), staged, unstaged, repo.getMergingCommits(), repo.getMergeConflicts());
        } catch (IOException e) {
            throw new MiniGitApiException("Error reading repository status: " + e.getMessage(), e);
        }
    }

    /**
     * Stage files.
     *
     * @param patterns The CWD-relative paths (patterns) to stage.
     * @throws MiniGitApiException If reading files or saving index fails.
     */
    public void stage(Collection<Path> patterns) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            for (Path pattern : patterns) {
                for (Path file : FileHelper.getAllFiles(pattern, repo.getIgnored())) {
                    repo.addToIndex(file);
                }
            }
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error staging files: " + e.getMessage(), e);
        }
    }

    /**
     * Overwrite unstaged changes with index.
     *
     * @throws MiniGitApiException If repository is corrupted or writing the files fails.
     */
    public void discardUnstaged() throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            for (Map.Entry<Path, String> entry : repo.getTrackedFiles().entrySet()) {

                // Test whether the staged file is the same as the current file
                if (Files.exists(entry.getKey()) && new Blob(entry.getKey()).miniGitSha1().equals(entry.getValue())) {
                    continue;
                }

                // If not, overwrite the file with the staged version
                MiniGitObject trackedObject = repo.loadFromInternal(entry.getValue());
                if (trackedObject instanceof Blob trackedBlob) {
                    trackedBlob.writeContentsTo(entry.getKey());
                } else {
                    throw new MiniGitApiException("Tracked file is not a blob. Repository is corrupted.");
                }
            }
        } catch (IOException e) {
            throw new MiniGitApiException("Error restoring files: " + e.getMessage(), e);
        }
    }

    /**
     * Restore index to the last commit.
     *
     * @throws MiniGitApiException If reading the commit or saving the index fails.
     */
    public void unstageAll() throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            repo.unstageToLastCommit();
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error unstaging changes: " + e.getMessage(), e);
        }
    }

    /**
     * Commit staged files.
     *
     * @param message The commit message.
     * @return The hash of the new commit.
     * @throws MiniGitApiException If no author is set, there is nothing to commit, or saving fails.
     */
    public String commit(String message) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            Author author = repo.getCurrentAuthor();
            if (author == null) {
                throw new MiniGitApiException("No author set. Use 'minigit author <name> <email>' in the terminal.");
            }

            // Check if there is anything to commit
            if (repo.getStagedToLastCommitStatus().stream()
                    .allMatch(status -> status.status() == Repository.FileStatusType.SAME)) {
                throw new MiniGitApiException("There is nothing to commit.");
            }

            // Build the tree to save from the staged files
            List<Tree> trees = Tree.buildTree(repo.getTrackedFiles(), repo.getRootPath());
            Tree rootTree = trees.getLast();

            // Create the commit with the current HEAD commit as the parent (or no parent for the first commit)
            Head head = repo.getHead();
            String parent = switch (head.type()) {
                case COMMIT -> head.data();
                case BRANCH -> repo.getBranches().get(head.data());
                case UNSET -> null;
            };
            Commit commit = new Commit(rootTree.miniGitSha1(), parent, author, message, Date.from(Instant.now()));

            // Store the data
            repo.storeInternally(commit);
            trees.forEach(repo::storeInternally);

            // Update HEAD
            switch (head.type()) {
                case BRANCH -> {
                    repo.setBranch(head.data(), commit.miniGitSha1());
                    repo.setHeadToBranch(head.data());
                }
                case UNSET -> {
                    repo.setBranch(FIRST_BRANCH_NAME, commit.miniGitSha1());
                    repo.setHeadToBranch(FIRST_BRANCH_NAME);
                }
                case COMMIT -> repo.setHeadToCommit(commit);
            }

            repo.save();
            return commit.miniGitSha1();
        } catch (IOException e) {
            throw new MiniGitApiException("Error committing: " + e.getMessage(), e);
        }
    }

    /**
     * Get commit history reachable from HEAD, newest first.
     *
     * <p>
     *     Each commit is returned only once. With the parent hashes, the caller can rebuild
     *     the whole commit graph. When the HEAD is detached, commits reachable from the
     *     branches are included too, so the newer commits of the left branch stay visible.
     * </p>
     *
     * @return List of commits sorted by date (newest first), or empty list if there are no commits yet.
     * @throws MiniGitApiException If the commit history is corrupted or cannot be read.
     */
    public List<CommitInfo> log() throws MiniGitApiException {
        Repository repo = loadRepo();
        String headCommitHash = repo.getHeadCommitHash();
        List<CommitInfo> result = new ArrayList<>();
        if (headCommitHash == null) {
            return result;
        }
        try {
            Deque<String> toVisit = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            toVisit.push(headCommitHash);
            if (repo.getHead().type() == Head.Type.COMMIT) {
                repo.getBranches().values().forEach(toVisit::push);
            }
            while (!toVisit.isEmpty()) {
                String hash = toVisit.pop();
                if (!visited.add(hash)) {
                    continue;
                }
                if (!(repo.loadFromInternal(hash) instanceof Commit commit)) {
                    throw new MiniGitApiException("Commit history is corrupted: " + hash + " is not a commit.");
                }
                result.add(new CommitInfo(hash, List.of(commit.getParents()), commit.getAuthor(), commit.getDate(),
                        commit.getMessage(), refsPointingTo(repo.getBranches(), hash),
                        refsPointingTo(repo.getTags(), hash), hash.equals(headCommitHash)));
                for (String parent : commit.getParents()) {
                    toVisit.push(parent);
                }
            }
        } catch (IOException e) {
            throw new MiniGitApiException("Error reading commit history: " + e.getMessage(), e);
        }
        // Stable sort: commits sharing a date keep the DFS order, which visits children before parents
        result.sort(Comparator.comparing(CommitInfo::date).reversed());
        return result;
    }

    /**
     * Diff a file between the index and the working directory.
     *
     * @param file The CWD-relative path of the file.
     * @return The whole-file using diff lines.
     * @throws MiniGitApiException If reading the file or the repository fails.
     */
    public List<DiffLine> diffWorkingVsIndex(Path file) throws MiniGitApiException {
        Repository repo = loadRepo();
        Path key = file.normalize();
        try {
            Blob oldBlob = loadBlobOrEmpty(repo, repo.getTrackedFiles().get(key));
            Blob newBlob = Files.exists(key) ? new Blob(key) : new Blob(new byte[0]);
            return diffLines(oldBlob, newBlob);
        } catch (IOException e) {
            throw new MiniGitApiException("Error diffing file " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Diff a file between the last commit and the index.
     *
     * @param file The CWD-relative path of the file.
     * @return The whole-file using diff lines.
     * @throws MiniGitApiException If reading the repository fails.
     */
    public List<DiffLine> diffIndexVsHead(Path file) throws MiniGitApiException {
        Repository repo = loadRepo();
        Path key = file.normalize();
        try {
            Blob oldBlob = loadBlobOrEmpty(repo, repo.getHead().getCommitIndex(repo).get(key));
            Blob newBlob = loadBlobOrEmpty(repo, repo.getTrackedFiles().get(key));
            return diffLines(oldBlob, newBlob);
        } catch (IOException e) {
            throw new MiniGitApiException("Error diffing file " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Diff all files of a commit against its previous commit.
     *
     * <p>
     *     Only changed files are included, each preceded by a HEADER line with its path.
     *     The first commit is diffed against nothing, a merge commit against its first parent.
     * </p>
     *
     * @param hash The hash of the commit.
     * @return The whole-file using diff lines of all changed files.
     * @throws MiniGitApiException If the commit does not exist or reading the repository fails.
     */
    public List<DiffLine> diffCommitVsParent(String hash) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            Commit commit = loadCommit(repo, hash);
            Map<Path, String> newIndex = commitIndex(repo, commit);
            Map<Path, String> oldIndex = commit.getParents().length == 0
                    ? Map.of() : commitIndex(repo, loadCommit(repo, commit.getParents()[0]));

            Set<Path> files = new TreeSet<>(oldIndex.keySet());
            files.addAll(newIndex.keySet());

            List<DiffLine> lines = new ArrayList<>();
            for (Path file : files) {
                String oldHash = oldIndex.get(file);
                String newHash = newIndex.get(file);
                if (Objects.equals(oldHash, newHash)) {
                    continue;
                }
                lines.add(new DiffLine(DiffLine.Type.HEADER, file.toString()));
                lines.addAll(diffLines(loadBlobOrEmpty(repo, oldHash), loadBlobOrEmpty(repo, newHash)));
            }
            return lines;
        } catch (IOException e) {
            throw new MiniGitApiException("Error diffing commit " + hash + ": " + e.getMessage(), e);
        }
    }

    /**
     * Move HEAD and the working directory to a branch/tag/commit.
     *
     * <p>
     *     Checking out a branch starts following it. Checking out a tag or a commit hash detaches the HEAD.
     * </p>
     *
     * @param ref The branch name, tag name, or commit hash to check out.
     * @throws MiniGitApiException If the working tree is dirty, the ref does not name a commit, or checkout fails.
     */
    public void checkout(String ref) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            if (repo.workingTreeDirty()) {
                throw new MiniGitApiException("Cannot checkout - working tree is not clean.");
            }
            Commit commit = loadCommit(repo, ref);
            if (!(repo.loadFromInternal(commit.getTreeHash()) instanceof Tree tree)) {
                throw new MiniGitApiException("Commit tree is missing. Repository is corrupted.");
            }
            repo.checkoutTree(tree);
            if (repo.isBranch(ref)) {
                repo.setHeadToBranch(ref);
            } else {
                repo.setHeadToCommit(commit);
            }
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error checking out: " + e.getMessage(), e);
        }
    }

    /**
     * Create a branch pointing to the current HEAD commit. HEAD keeps its position.
     *
     * @param name The name of the new branch. An existing branch with that name is moved.
     * @throws MiniGitApiException If there are no commits yet or saving fails.
     */
    public void makeBranch(String name) throws MiniGitApiException {
        Repository repo = loadRepo();
        String headCommitHash = repo.getHeadCommitHash();
        if (headCommitHash == null) {
            throw new MiniGitApiException("Cannot create a branch. There are no commits yet.");
        }
        try {
            repo.setBranch(name, headCommitHash);
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error creating branch: " + e.getMessage(), e);
        }
    }

    /**
     * Get the names of all branches and tags.
     *
     * @return Sorted list of the branch and tag names, usable as checkout targets.
     * @throws MiniGitApiException If the repository cannot be read.
     */
    public List<String> refs() throws MiniGitApiException {
        Repository repo = loadRepo();
        return Stream.concat(repo.getBranches().keySet().stream(), repo.getTags().keySet().stream())
                .sorted().toList();
    }

    /**
     * Merge the specified branch/commit/tag into the current HEAD.
     *
     * @param ref The branch name, tag name, or commit hash to merge from.
     * @return The merge status. Read CLI docs for explanation.
     * @throws MiniGitApiException If the working tree is dirty, HEAD is unset, a merge is already in progress,
     * the ref does not name a commit, or the merge fails with exception.
     */
    public Repository.MergeStatus merge(String ref) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            if (repo.workingTreeDirty()) {
                throw new MiniGitApiException("Cannot merge - working tree is not clean.");
            }
            Head head = repo.getHead();
            if (head.type() == Head.Type.UNSET) {
                throw new MiniGitApiException("Cannot merge yet. No HEAD.");
            }
            if (repo.isMerging()) {
                throw new MiniGitApiException("Cannot merge - a merge is already in progress.");
            }
            if (!(repo.loadFromInternal(ref) instanceof Commit mergeFromCommit)
                    || !(repo.loadFromInternal(head.data()) instanceof Commit mergeIntoCommit)) {
                throw new MiniGitApiException("Cannot merge - " + ref + " does not name a commit.");
            }
            Repository.MergeStatus mergeStatus = repo.startMerging(
                    new MergingCommits(mergeFromCommit.miniGitSha1(), head, mergeIntoCommit.miniGitSha1()));
            if (mergeStatus != Repository.MergeStatus.INVALID) {
                repo.save();
            }
            return mergeStatus;
        } catch (IOException e) {
            throw new MiniGitApiException("Error merging: " + e.getMessage(), e);
        }
    }

    /**
     * Apply the merge in progress.
     *
     * @param message The commit message of the merge commit.
     * @throws MiniGitApiException If no merge is in progress or the merge commit cannot be created.
     */
    public void mergeApply(String message) throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            if (!repo.mergeFromIndex(message)) {
                throw new MiniGitApiException("Merge failed. Is a merge in progress?");
            }
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error applying merge: " + e.getMessage(), e);
        }
    }

    /**
     * Stop the merge in progress.
     *
     * @throws MiniGitApiException If saving the repository fails.
     */
    public void mergeStop() throws MiniGitApiException {
        Repository repo = loadRepo();
        try {
            repo.stopMerge();
            repo.save();
        } catch (IOException e) {
            throw new MiniGitApiException("Error stopping merge: " + e.getMessage(), e);
        }
    }

    /**
     * Load fresh repository state from the disk.
     *
     * @return The loaded repository.
     * @throws MiniGitApiException If the repository cannot be loaded.
     */
    private Repository loadRepo() throws MiniGitApiException {
        try {
            return Repository.load(minigitDir);
        } catch (IOException e) {
            throw new MiniGitApiException("Cannot load repository: " + e.getMessage(), e);
        }
    }

    /**
     * Load the blob with the specified hash from the repository.
     *
     * @param repo The repository to load from.
     * @param hash The hash of the blob.
     * @return The loaded blob.
     * @throws MiniGitApiException If the object does not exist or is not a blob.
     * @throws IOException If loading the object fails.
     */
    private static Blob loadBlob(Repository repo, String hash) throws MiniGitApiException, IOException {
        if (repo.loadFromInternal(hash) instanceof Blob blob) {
            return blob;
        }
        throw new MiniGitApiException("Object " + hash + " is not a blob. Repository is corrupted.");
    }

    /**
     * Load the blob with the specified hash, or an empty blob for a null hash.
     *
     * <p>
     *     Diffs use the empty blob as the missing side of a created/deleted file.
     * </p>
     *
     * @param repo The repository to load from.
     * @param hash The hash of the blob, or null.
     * @return The loaded blob, or an empty blob.
     * @throws MiniGitApiException If the object does not exist or is not a blob.
     * @throws IOException If loading the object fails.
     */
    private static Blob loadBlobOrEmpty(Repository repo, String hash) throws MiniGitApiException, IOException {
        return hash == null ? new Blob(new byte[0]) : loadBlob(repo, hash);
    }

    /**
     * Load the commit with the specified hash from the repository.
     *
     * @param repo The repository to load from.
     * @param hash The hash of the commit.
     * @return The loaded commit.
     * @throws MiniGitApiException If the object does not exist or is not a commit.
     * @throws IOException If loading the object fails.
     */
    private static Commit loadCommit(Repository repo, String hash) throws MiniGitApiException, IOException {
        if (repo.loadFromInternal(hash) instanceof Commit commit) {
            return commit;
        }
        throw new MiniGitApiException("Object " + hash + " is not a commit.");
    }

    /**
     * Get the index of a commit.
     *
     * @param repo The repository to load from.
     * @param commit The commit whose index to build.
     * @return Map "path -> blob hash" of the files in the commit.
     * @throws MiniGitApiException If the commit tree is missing.
     * @throws IOException If loading the tree fails.
     */
    private static Map<Path, String> commitIndex(Repository repo, Commit commit) throws MiniGitApiException, IOException {
        if (repo.loadFromInternal(commit.getTreeHash()) instanceof Tree tree) {
            return tree.getIndex(repo);
        }
        throw new MiniGitApiException("Commit tree is missing. Repository is corrupted.");
    }

    /**
     * Diff two blobs and convert the result into diff lines of the entire file.
     *
     * @param oldBlob The old version of the file.
     * @param newBlob The new version of the file.
     * @return The whole-file using diff lines.
     * @throws IOException If reading the blob contents fails.
     */
    private static List<DiffLine> diffLines(Blob oldBlob, Blob newBlob) throws IOException {
        List<MiniGitDiff.DiffResult> results = MiniGitDiff.diff(oldBlob, newBlob);
        List<String> oldLines = oldBlob.readAllLines();
        List<String> newLines = newBlob.readAllLines();
        List<DiffLine> lines = new ArrayList<>();
        long newPosition = 0;
        for (MiniGitDiff.DiffResult result : results) {

            // Unchanged lines before this replacement
            for (; newPosition < result.replaceWithFrom(); newPosition++) {
                lines.add(new DiffLine(DiffLine.Type.SAME, newLines.get((int) newPosition)));
            }
            // Replaced old lines
            for (long oldPosition = result.replaceFrom(); oldPosition < result.replaceTo(); oldPosition++) {
                lines.add(new DiffLine(DiffLine.Type.DELETED, oldLines.get((int) oldPosition)));
            }
            // Replacing new lines
            for (; newPosition < result.replaceWithTo(); newPosition++) {
                lines.add(new DiffLine(DiffLine.Type.ADDED, newLines.get((int) newPosition)));
            }
        }

        // Unchanged lines after the last replacement
        for (; newPosition < newLines.size(); newPosition++) {
            lines.add(new DiffLine(DiffLine.Type.SAME, newLines.get((int) newPosition)));
        }
        return lines;
    }

    /**
     * Get the branches or tags names that point to the specified commit.
     *
     * @param refs Map "name -> commit hash" to search in.
     * @param hash The commit hash to look for.
     * @return Sorted list of the names pointing to the commit.
     */
    private static List<String> refsPointingTo(Map<String, String> refs, String hash) {
        return refs.entrySet().stream().filter(entry -> entry.getValue().equals(hash))
                .map(Map.Entry::getKey).sorted().toList();
    }
}
