package cz.cuni.mff.jordanpa.minigit.structures;

import cz.cuni.mff.jordanpa.minigit.misc.Author;

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

    private final Path repoPath;
    private final Path objectsPath;
    private final Path indexPath;
    private final Path headPath;
    private final Path authorPath; // Not in remote!
    private final Path refPath;
    private Head head;

    public Head getHead(){
        return head;
    }

    /**
     * Map path -> data.
     */
    private HashMap<Path, String> stagedIndex;

    public Repository(Path path) {
        this.repoPath = path;
        this.objectsPath = path.resolve("objects");
        this.indexPath = path.resolve("index");
        this.headPath = path.resolve("HEAD");
        this.authorPath = path.resolve("author");
        this.refPath = path.resolve("refs");
    }

    public Map<String, String> getBranches() {
        return Map.copyOf(branches);
    }

    public Map<String, String> getTags() {
        return Map.copyOf(tags);
    }

    public Map<String, String> getAllRefs() {
        HashMap<String, String> refs = new HashMap<>();
        refs.putAll(Map.copyOf(branches));
        refs.putAll(Map.copyOf(tags));
        return refs;
    }

    public static Repository load(Path loadFrom) throws IOException {
        if (!Files.exists(loadFrom)) {
            throw new IOException("Repository does not exist.");
        }
        Repository repo = new Repository(loadFrom);
        repo.loadHead();
        repo.loadRef();
        repo.loadIndex();
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
    }

    public void addToIndex(Path... files) {
        for (Path file : files) {
            try {
                if (!Files.exists(file)) {
                    stagedIndex.remove(file);
                    continue;
                }
                Blob blob = new Blob(file);
                stagedIndex.put(file, blob.miniGitSha1());
                storeInternally(blob);
            }
            catch(IOException e) {
                IO.println(e);
                IO.println("Error adding file " + file + " to index. Continuing...");
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
                stagedIndex.put(path, hash);
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
                out.write(entry.getValue() + " " + entry.getKey().normalize() + "\n");
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
        Tree CurrentRoot = Tree.buildTree(stagedIndex).getLast();
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
                Files.delete(status.path());
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
                if (Files.exists(entry.getKey())) {
                    IO.println("File " + entry.getKey() + " already exists. Cannot overwrite!");
                    return;
                }
                IO.println("Restoring file " + entry.getKey() + "...");
                try {
                    blobToRestore.writeContentsTo(entry.getKey());
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

    private List<Path> getPaths(Path path) {
        ArrayList<Path> paths = new ArrayList<>();
        try(var dirStream = Files.newDirectoryStream(path)) {
            for (Path entry : dirStream) {
                if (entry.getFileName().toString().equals(".minigit")) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    paths.addAll(getPaths(entry));
                }
                else {
                    paths.add(entry.normalize());
                }
            }
        }
        catch(IOException e) {
            IO.println(e);
        }
        return paths;
    }

    public void setHeadToCommit(Commit commit) {
        head = new Head(Head.Type.COMMIT, commit.miniGitSha1());
    }

    public void setHeadToBranch(String checkoutTo) {
        head = new Head(Head.Type.BRANCH, checkoutTo);
    }

    HashMap<Path, String> getIndexOfTree(Tree tree) throws IOException {
        return getIndexOfTreeInternal(tree, Path.of("./"));
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
                treeIndex.put(pathSoFar.resolve(name).normalize(), value.hash());

            }
        }
        return treeIndex;
    }

    public boolean isBranch(String name) {
        return branches.containsKey(name);
    }

    public HashMap<Path, String> getIndexOfWorkingDirectory() throws IOException {
        List<Path> paths = getPaths(Path.of("./"));
        HashMap<Path, String> workingDirIndex = new HashMap<>();
        for(Path path : paths) {
            String hash = new Blob(path).miniGitSha1();
            workingDirIndex.put(path, hash);
        }
        return workingDirIndex;
    }

    public void unstageToLastCommit() throws IOException {
        stagedIndex = getCommitIndexFromHead();
    }

    public Author loadCurrentAuthor() throws IOException {
        if (!Files.exists(authorPath)) {
            return null;
        }
        try(var in = Files.newBufferedReader(authorPath)) {
            String name = in.readLine();
            String email = in.readLine();
            return new Author(name, email);
        }
    }

    public void setCurrentAuthor(Author author) throws IOException {
        try(var out = Files.newBufferedWriter(authorPath)) {
            out.write(author.name() + "\n");
            out.write(author.email() + "\n");
        }
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
}
