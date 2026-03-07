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

public final class Repository implements MinigitObjectLoader {

    public enum FileStatusType {
        SAME,
        MODIFIED,
        DELETED,
        NEW
    }

    public enum MergeStatus {
        CONFLICT,
        APPLIED,
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

    public boolean isMerging() {
        return mergingCommits != null;
    }

    public MergeStatus startMerging(MergingCommits mergingCommits) throws IOException {
        if (isMerging()) {
            IO.println("Cannot start merging. There is already a merging in progress. Use stop-merge first, or apply-merge.");
            return MergeStatus.INVALID;
        }
        this.mergingCommits = mergingCommits;
        return merge();
    }

    public boolean mergeFromIndex(String message) throws IOException {
        if (!isMerging()) {
            IO.println("Cannot merge. There is no merging in progress.");
            return false;
        }
        MergingCommits merging = mergingCommits;
        String intoCommitHash = merging.intoCommit();
        String fromCommitHash = merging.fromCommit();
        Head intoHead = merging.intoHead();
        if (!loadFromInternal(intoHead.data()).miniGitSha1().equals(intoCommitHash)) {
            IO.println("Warning: merging into a commit that is not the current head. This is not recommended.");
        }
        List<Tree> newTrees = Tree.buildTree(stagedIndex, mainRepoPath);
        Commit result = new Commit(newTrees.getLast().miniGitSha1(), intoCommitHash, fromCommitHash, getCurrentAuthor(), message, Date.from(Instant.now()));
        updateHeadToCommit(intoHead, result);
        newTrees.forEach(this::storeInternally);
        storeInternally(result);
        stopMerge();
        return true;
    }

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

    public void stopMerge() {
        this.mergingCommits = null;
    }

    private MergeStatus merge() throws IOException{
        IO.println("Will merge from " + mergingCommits.fromCommit());
        IO.println("To " + mergingCommits.intoHead());
        MiniGitObject oursObject = loadFromInternal(mergingCommits.intoCommit());
        MiniGitObject theirsObject = loadFromInternal(mergingCommits.fromCommit());
        if (!(oursObject instanceof Commit oursCommit) || !(theirsObject instanceof Commit theirsCommit)) {
            IO.println("Cannot merge. Commits are not of type commit.");
            stopMerge();
            return MergeStatus.INVALID;
        }

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
                IO.println("Regular merge.");
                stagedIndex = MR.newStagedIndex();
                MR.mergeIndexBlobsToSave().values().forEach(this::storeInternally);
                if (MR.conflicts().isEmpty()) {
                    return MergeStatus.APPLIED;
                }
                else {
                    IO.println("Conflicts detected. MiniGit asks you to resolve them:");
                    for (var conflict : MR.conflicts()) {
                        IO.println(conflict.toString());
                    }
                    IO.println("Resolve conflicts, stage them and then type 'merge-apply' to apply the merge.");
                    return MergeStatus.CONFLICT;
                }
            }
        }
        IO.println("Merging successful. But not implemented yet. So conflict you get.");
        return MergeStatus.CONFLICT;
    }

    public MergingCommits getMergingCommits() {
        return mergingCommits;
    }

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

    private Path mainRepoAbsPath() { return mainRepoPath.toAbsolutePath().normalize(); }
    private Path cwdAbsPath()  { return Path.of("").toAbsolutePath().normalize(); }
    private Path safeAbsFromCwdRelative(Path p) throws IOException {
        Path abs = cwdAbsPath().resolve(p).normalize();
        if (!abs.startsWith(mainRepoAbsPath())) {
            throw new IOException("Path escapes repository: " + p);
        }
        return abs;
    }

    private boolean isInRepo(Path p) {
        Path abs = cwdAbsPath().resolve(p).normalize();
        return abs.startsWith(mainRepoAbsPath());
    }

    private Path getPathRelativeToRepo(Path cwdRelative) throws IOException {
        Path abs = safeAbsFromCwdRelative(cwdRelative);
        return mainRepoAbsPath().relativize(abs).normalize();
    }

    private Head head;

    public Head getHead(){
        return head;
    }

    /**
     * Map path -> data. In memory are relative paths
     */
    private HashMap<Path, String> stagedIndex;

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

    public Path getRootPath() {
        return mainRepoPath;
    }

    public Map<String, String> getBranches() {
        return Map.copyOf(branches);
    }

    public void deleteBranch(String branchName) {
        branches.remove(branchName);
    }

    public Map<String, String> getTags() {
        return Map.copyOf(tags);
    }

    public void deleteTag(String tagName) {
        tags.remove(tagName);
    }

    public static Repository load(Path loadFrom) throws IOException {
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

    public void storeInternally(MiniGitObject obj) {
        if (!objects.containsKey(obj.miniGitSha1())) {
            objects.put(obj.miniGitSha1(), obj);
        }
    }

    public MiniGitObject loadFromInternal(String hashOrName) throws IOException {
        String hash;

        if (branches.containsKey(hashOrName)) {
            hash = branches.get(hashOrName);
        }
        else {
            hash = tags.getOrDefault(hashOrName, hashOrName);
        }

        if (!objects.containsKey(hash)) {
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

    public void save() throws IOException {
        if (!Files.exists(objectsPath)) {
            Files.createDirectory(objectsPath);
        }
        for (Map.Entry<String, MiniGitObject> obj : objects.entrySet()) {
            MiniGitObject.saveObjectBasedOnHash(objectsPath, obj.getKey(), obj.getValue());
        }
        saveIndex();
        saveHead();
        saveRef();
        saveCurrentAuthor();
        saveMerging();
    }

    public boolean addToIndex(Path file) {
        // Ignore files not belonging to the repository - useful when working with multiple repositories.
        if (!isInRepo(file)) {
            return false;
        }
        try {
        Path abs = safeAbsFromCwdRelative(file.normalize());
        Path normalized = cwdAbsPath().relativize(abs).normalize();
            if (!Files.exists(abs) || FileHelper.isExcluded(normalized, getIgnored())) {
                stagedIndex.remove(normalized);
                return true;
            }
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

    private void loadHead() throws IOException {
        head = Head.loadHead(headPath);
    }

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

    public Map<Path, String> getTrackedFiles(){
        return Map.copyOf(stagedIndex);
    }

    public void checkoutTree(Tree treeToRestore) throws IOException {
        List<FileStatus> statusesStaged = getWorkingToIndexStatus();
        List<FileStatus> statusesCommited = getStagedToLastCommitStatus();
        if (statusesStaged.stream().anyMatch(status -> status.status() != FileStatusType.SAME && status.status() != FileStatusType.NEW)) {
            throw new IOException("Cannot restore tree. There are unstaged uncommitted changes.");
        }
        if (statusesCommited.stream().anyMatch(status -> status.status() != FileStatusType.SAME && status.status() != FileStatusType.NEW)) {
            throw new IOException("Cannot restore tree. There are staged uncommitted changes.");
        }
        Tree CurrentRoot = Tree.buildTree(stagedIndex, mainRepoPath).getLast();
        if (loadFromInternal(CurrentRoot.miniGitSha1()) == null) {
            throw new IOException("Cannot restore tree. Current root tree is not in repository. Use tree command to build it.");
        }
        if (CurrentRoot.miniGitSha1().equals(treeToRestore.miniGitSha1())) {
            IO.println("Tree is already checked out.");
            return;
        }
        for (FileStatus status : statusesStaged.stream().filter(s -> s.status() == FileStatusType.SAME).toList()) {
            if (status.status() == FileStatusType.SAME) {
                IO.println("Deleting tracked file " + status.path());
                Files.delete(safeAbsFromCwdRelative(status.path()));
            }
        }
        stagedIndex = treeToRestore.getIndex(this);
        IO.println("Restoring tree...");
        treeToRestore.forcedCheckoutTree(this);
    }

    public List<FileStatus> getStagedToLastCommitStatus() throws IOException {
        HashMap<Path, String> commitIndex = head.getCommitIndex(this);
        return getFileStatusesFromComparison(stagedIndex, commitIndex);
    }

    public List<FileStatus> getWorkingToIndexStatus() throws IOException {
        HashMap<Path, String> workingDirIndex = getIndexOfWorkingDirectory();
        return getFileStatusesFromComparison(workingDirIndex, stagedIndex);
    }

    public void setHeadToCommit(Commit commit) {
        head = new Head(Head.Type.COMMIT, commit.miniGitSha1());
    }

    public void setHeadToBranch(String checkoutTo) {
        head = new Head(Head.Type.BRANCH, checkoutTo);
    }

    public boolean isBranch(String name) {
        return branches.containsKey(name);
    }

    public HashMap<Path, String> getIndexOfWorkingDirectory() throws IOException {
        List<Path> paths = FileHelper.getAllFiles(mainRepoPath, getIgnored());
        HashMap<Path, String> workingDirIndex = new HashMap<>();
        for(Path path : paths) {
            String hash = new Blob(path).miniGitSha1();
            workingDirIndex.put(path.normalize(), hash);
        }
        return workingDirIndex;
    }

    public void unstageToLastCommit() throws IOException {
        stagedIndex = head.getCommitIndex(this);
    }

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

    private void saveCurrentAuthor() throws IOException {
        if (currentAuthor == null) {
            return;
        }
        try(var out = Files.newBufferedWriter(authorPath)) {
            out.write(currentAuthor.name() + "\n");
            out.write(currentAuthor.email() + "\n");
        }
    }

    public void setCurrentAuthor(Author author) throws IOException {
        currentAuthor = author;
    }

    public Author getCurrentAuthor() {
        return currentAuthor;
    }

    public void setBranch(String arg, String hash){
        if (branches.containsKey(arg)) {
            IO.println("Branch " + arg + " moved from: " + branches.get(arg) + " to " + hash);
        }
        branches.put(arg, hash);
    }

    public void setTag(String arg, String data) {
        tags.put(arg, data);
    }

    private final List<String> ignored = new ArrayList<>();

    public List<String> getIgnored() throws IOException {
        return ignored;
    }

    private void loadIgnored() throws IOException {
        if (Files.exists(ignoredPath)) {
            try (var ignoredStream = Files.newBufferedReader(ignoredPath)) {
                ignoredStream.lines().filter(s -> !s.isEmpty()).forEach(ignored::add);
            }
        }
        ignored.add(repoInternalPath.normalize() + "/");
    }

    public boolean workingTreeDirty() throws IOException {
        List<Repository.FileStatus> statusesWorking = getWorkingToIndexStatus();
        List<Repository.FileStatus> statusesCommited = getStagedToLastCommitStatus();
        return !statusesWorking.stream().allMatch(status -> status.status() == FileStatusType.SAME || status.status() == FileStatusType.NEW)
                || !statusesCommited.stream().allMatch(status -> status.status() == FileStatusType.SAME);
    }

    public String getHashFromRef(String ref) {
        if (branches.containsKey(ref)) {
            return branches.get(ref);
        }
        else return tags.getOrDefault(ref, null);
    }
}
