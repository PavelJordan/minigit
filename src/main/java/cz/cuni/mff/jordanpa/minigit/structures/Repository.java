package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.misc.FileHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Repository {

    public enum FileStatusType {
        SAME,
        MODIFIED,
        DELETED,
        NEW
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
    private Path mainRepoAbsPath() { return mainRepoPath.toAbsolutePath().normalize(); }
    private Path cwdAbsPath()  { return Path.of("").toAbsolutePath().normalize(); }
    private Path safeAbsFromCwdRelative(Path p) throws IOException {
        Path abs = cwdAbsPath().resolve(p).normalize();
        if (!abs.startsWith(mainRepoAbsPath())) {
            throw new IOException("Path escapes repository: " + p);
        }
        return abs;
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
        this.ignoredPath = path.resolve("../.minigitignore").normalize();
    }

    public Path getRepoDirectory() {
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
    }

    public void addToIndex(Path... files) {
        for (Path file : files) {
            Path normalized = file.normalize();
            try {
                if (!Files.exists(normalized) || FileHelper.isExcluded(normalized, getIgnored())) {
                    stagedIndex.remove(normalized);
                    continue;
                }
                Blob blob = new Blob(normalized);
                stagedIndex.put(normalized, blob.miniGitSha1());
                storeInternally(blob);
            }
            catch(IOException e) {
                IO.println(e);
                IO.println("Error adding file " + normalized + " to index. Continuing...");
            }
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
        stagedIndex = getIndexOfTree(treeToRestore);
        IO.println("Restoring tree...");
        internalForcedCheckoutTree();
    }

    private void internalForcedCheckoutTree() throws IOException {
        for (HashMap.Entry<Path, String> entry : stagedIndex.entrySet()) {
            MiniGitObject possibleBlobToRestore = loadFromInternal(entry.getValue());
            if (possibleBlobToRestore instanceof Blob blobToRestore) {
                Path absTarget = safeAbsFromCwdRelative(entry.getKey());
                if (Files.exists(absTarget)) {
                    IO.println("File " + absTarget + " already exists. Cannot overwrite!");
                    return;
                }
                IO.println("Restoring file " + absTarget + "...");
                try {
                    blobToRestore.writeContentsTo(absTarget);
                } catch (IOException e) {
                    IO.println(e);
                }
            }
            else {
                IO.println("Object with specified data is not a blob, even though it should. Repository is corrupted.");
            }
        }
    }

    public List<FileStatus> getStagedToLastCommitStatus() throws IOException {
        HashMap<Path, String> commitIndex = getCommitIndexFromHead();
        return getFileStatusesFromComparison(stagedIndex, commitIndex);
    }

    private HashMap<Path, String> getCommitIndexFromHead() throws IOException {
        String hash;
        if (head.type() == Head.Type.BRANCH) {
             hash = branches.get(head.data());
        }
        else if (head.type() == Head.Type.COMMIT) {
             hash = head.data();
        }
        else {
            return new HashMap<>();
        }
        MiniGitObject maybeCommit = loadFromInternal(hash);
        if (maybeCommit instanceof Commit commit) {
            MiniGitObject maybeTree = loadFromInternal(commit.getTreeHash());
            if (maybeTree instanceof Tree tree) {
                return getIndexOfTree(tree);
            }
            else {
                throw new IOException("Cannot restore tree. Tree is not in repository.");
            }
        }
        else {
            throw new IOException("Cannot restore head.");
        }
    }

    public List<FileStatus> getWorkingToIndexStatus() throws IOException {
        HashMap<Path, String> workingDirIndex = getIndexOfWorkingDirectory();
        return getFileStatusesFromComparison(workingDirIndex, stagedIndex);
    }

    private List<FileStatus> getFileStatusesFromComparison(HashMap<Path, String> currentIndex, HashMap<Path, String> indexToCompare) {
        List<FileStatus> statuses = new ArrayList<>();
        for (Path path : currentIndex.keySet()) {
            String workingDirectoryHash = currentIndex.get(path);
            FileStatusType againstCommit;
            if (indexToCompare.containsKey(path)) {
                if (indexToCompare.get(path).equals(workingDirectoryHash)) {
                    againstCommit = FileStatusType.SAME;
                }
                else {
                    againstCommit = FileStatusType.MODIFIED;
                }
            }
            else {
                againstCommit = FileStatusType.NEW;
            }
            statuses.add(new FileStatus(path, againstCommit));
        }
        var deletedAgainstStaged = indexToCompare.keySet().stream().filter(path -> !currentIndex.containsKey(path));
        deletedAgainstStaged.forEach(path -> statuses.add(new FileStatus(path, FileStatusType.DELETED)));
        return statuses;
    }

    public void setHeadToCommit(Commit commit) {
        head = new Head(Head.Type.COMMIT, commit.miniGitSha1());
    }

    public void setHeadToBranch(String checkoutTo) {
        head = new Head(Head.Type.BRANCH, checkoutTo);
    }

    HashMap<Path, String> getIndexOfTree(Tree tree) throws IOException {
        return new HashMap<>(getIndexOfTreeInternal(tree, mainRepoPath));
    }

    private HashMap<Path, String> getIndexOfTreeInternal(Tree tree, Path pathSoFar) throws IOException {
        HashMap<Path, String> treeIndex = new HashMap<>();
        for (Map.Entry<String, Tree.TreeEntry> entry : tree.getContents().entrySet()) {
            String name = entry.getKey();
            Tree.TreeEntry value = entry.getValue();
            if (value.type() == Tree.TreeEntryType.TREE) {
                MiniGitObject possiblySubtreeToRestore = loadFromInternal(value.hash());
                if (possiblySubtreeToRestore instanceof Tree subtreeToRestore) {
                    treeIndex.putAll(getIndexOfTreeInternal(subtreeToRestore, pathSoFar.resolve(name)));
                } else {
                    IO.println("Object with specified data is not a tree, even though it should. Repository is corrupted.");
                }
            } else if (value.type() == Tree.TreeEntryType.BLOB) {
                Path resolvedPath = FileHelper.getRelativePathToDirectory(pathSoFar.resolve(name), Path.of("./"));
                treeIndex.put(resolvedPath, value.hash());

            }
        }
        return treeIndex;
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
        stagedIndex = getCommitIndexFromHead();
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

    public void showTreeDiff(Tree baseTree, Tree treeToCompare) throws IOException{
        IO.println("The tree " + treeToCompare.miniGitSha1() + " has changed from " + baseTree.miniGitSha1() + " in the following way:");
        HashMap<Path, String> baseTreeIndex = getIndexOfTree(baseTree);
        HashMap<Path, String> treeToCompareIndex = getIndexOfTree(treeToCompare);
        getFileStatusesFromComparison(treeToCompareIndex, baseTreeIndex).forEach(status -> {
            if (status.status() != FileStatusType.SAME) {
                IO.println(status.path() + " (" + status.status() + ")");
            }
        });
    }

    public void showTreeDiff(Tree commitTree) throws IOException {
        IO.println("The tree " + commitTree.miniGitSha1() + " added:");
        getIndexOfTree(commitTree).forEach((path, hash) -> {
            IO.println(path + " (" + hash + ")");
        });
    }

    public void setBranch(String arg, String hash){
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
        ignored.add(repoInternalPath.normalize().toString() + "/");
    }
}
