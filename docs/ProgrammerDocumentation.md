# Programmer Documentation

Welcome to the programmer documentation! Let me walk you through the interfaces the code uses, and then the main classes
that implement them, and how.

## Command interface

The commands are found in the `cz.cuni.mff.jordanpa.minigit.commands` package. They are loaded through 
`ServiceLoader`, so later you can add your own commands easily, or even modify the code so that the plugins are loaded
from jars.

To add a new command, you need to implement the `Command` interface.

```java
public interface Command {
    String name();
    String shortHelp();
    String help();
    String usage();
    int execute(String[] args);
}
```

Then, register it in `resources/META-INF/services/cz.cuni.mff.jordanpa.minigit.commands.Command`.

Upon start of the program, `main()` will load all the commands and find the one that matches the first argument.
The rest of the arguments are passed to that command.

## Structures

The structures are found in the `cz.cuni.mff.jordanpa.minigit.structures` package. They are the main data structures
of the program. Let's walk through them.

### MiniGitObject

Before we start, let's understand, what is an object.

Object can be either a blob, a tree, or a commit. They are all immutable, and have their own SHA-1 hash.
They are stored in the .minigit/objects directory.

You may now wonder, how to retrieve the blobs/trees/commits by their hashes? Well, in the objects directory,
the first two characters of their hash is the sub-directory name, and the rest of the hash is the file name.

To know, which object is which, we need to know the header of the object. The header is the first few bytes of the file.

```java
public abstract sealed class MiniGitObject implements Sha1Hashable permits Blob, Tree, Commit {
    protected static final byte[] BLOB_HEADER =   "[[blob]]..".getBytes();
    protected static final byte[] TREE_HEADER =   "[[tree]]..".getBytes();
    protected static final byte[] COMMIT_HEADER = "[[commit]]".getBytes();
    protected static final int HEADER_SIZES = BLOB_HEADER.length;

    public static MiniGitObject getObjectBasedOnHash(Path objectsPath, String hash) throws IOException;

    static void saveObjectBasedOnHash(Path objectsPath, MiniGitObject obj) throws IOException;

    abstract void write(Path path) throws IOException;
    public abstract String getDescription();

    protected String getSha1FromBytes(byte[] bytes);

    protected void writeBytes(byte[] bytes, Path path) throws IOException;
}
```

Based on the knowledge how MiniGitObjects work, the function of these methods should be pretty straightforward.

### Blobs

What are blobs? They are an internal representation of the files in the repository. They are a type of object.
They are addressable by their SHA-1 hash computed from their content and the `BLOB` header.

As all objects, they are immutable. Unless you do some sort of garbage collection, they will never be deleted,
so once something is staged, it will "never" be deleted.

Internally, they look like this:

```java

public final class Blob extends MiniGitObject implements TreeContent {
    private final byte[] content;

    public Blob(byte[] content);
    public Blob(Path path) throws IOException;
    
    @Override
    public String miniGitSha1();

    @Override
    void write(Path path) throws IOException;

    @Override
    public String getDescription() {
        return "Object:\nblob\nContent:\n" + new String(content);
    }

    public void writeContentsTo(Path path) throws IOException;

    public BufferedInputStream getContentReader();

    private void writeToStream(OutputStream out) throws IOException;

    public List<String> readAllLines() throws IOException;
}
```

As you can see, they don't know their name, only their bytes. You can construct them from a file or from a byte array.
You can then write them to a file as an object so that when you load them, you recognize them as blobs or write only their contents for the user.

Note: `readAllLines()` works even for files that are not text files. This is because UTF-8 is used for encoding and
it is VERY challenging to know if a file is text or binary. It will not crash, so it doesn't hurt.

Whether the methods are `public` or `internal` depends on the use case - do we want to expose them to the commands, or
just the repository/other object classes?

### Trees

Blobs are useless without name. Trees are objects, that contain blobs with their name, and other sub-trees, with their name.
They are supposed to mirror the directories in the repository. Again, they are immutable and have their own SHA-1 hash.

They do not hold the instances of their contents - only their names and SHA-1 hashes.

Internally, trees look like this:

```java
public final class Tree extends MiniGitObject implements TreeContent {
    public enum TreeEntryType { BLOB, TREE }
    public record TreeEntry(String hash, TreeEntryType type) {}
    private final SortedMap<String, TreeEntry> contents = new TreeMap<>();
    
    Tree(byte[] byteArray, boolean isWithHeader);
    Tree(Map<String, TreeEntry> contents);
    
    public Map<String, TreeEntry> getContents();
    public static LinkedList<Tree> buildTree(Map<Path, String> filesToBuild, Path root);
    
    @Override
    public String miniGitSha1();

    @Override
    void write(Path path) throws IOException;

    @Override
    public String getDescription();

    private void writeToStream(OutputStream out) throws IOException;

    public HashMap<Path, String> getIndex(MinigitObjectLoader loader) throws IOException;

    public void showTreeDiff(Tree treeToCompare, MinigitObjectLoader loader) throws IOException;

    public void showTreeDiff(MinigitObjectLoader loader) throws IOException;

    public void forcedCheckoutTree(MinigitObjectLoader loader) throws IOException;
}
```

`TreeEntryType` is what tells us, whether an entry in a tree is a blob or a sub-tree.
`TreeEntry` is a simple record that holds the hash of the object and its type.
Finally, the `contents` are pairs of the names and TreeEntries of the objects in a tree.

There are 3 interesting methods:

 - `buildTree()`, which builds a tree structure from a map of paths and their blob hashes,
returning all the trees that were needed to build the root tree. The root tree is the last tree in the list.

 - `getIndex()`, which returns a map of paths and their blob-hashes that are contained in the tree structure,
   starting from this tree as the root.

 - `forcedCheckoutTree()`, which uses the index retrieved from `getIndex()` to figure out its structure,
   and then writes the contents of the tree to the file system.

There are also some other methods, but those are for printing purposes to the user.

One very important note - you might have noticed that you specify the `root` parameter in `buildTree()`.
That is because, when saving the tree into the file system, we write its contents relative to that root. However, 
at runtime, the files are loaded relative to the current working directory. This simplifies the code,
but complicates saving and loading the trees between file system and runtime memory.

Also, `MinigitObjectLoader`, what is it?

### MinigitObjectLoader

MinigitObjectLoader is an abstraction above a repository itself. It can retrieve objects from the repository,
and knows its root path.

```java
public interface MinigitObjectLoader {
    MiniGitObject loadFromInternal(String hash) throws IOException;
    Path getRootPath();
    String getHashFromRef(String ref);
}
```

This makes it easier to test the code, and forces us to write better code in the tree class.

You might have noticed that the `getHashFromRef(String ref)`. This method can be used to retrieve the hash of branch/tag,
as that what "references" are called - they are just named pointers to commits. They are also the only
mutable thing in the repository.

The difference between tags and branches is that tags should usually stay on the same commit,
while branches move around.

### Head

HEAD is quite simple. After initialization of the repository, it is unset. When you do the first commit,
it is set to the commit that was just created. You can use checkout commits to move between commits
in the "detached" state.

If you ever create a branch and check out to it, the HEAD is set to follow that branch. That means it will move
that branch anytime you create a commit, not only the HEAD.

One nuance—you by default check out the “master” branch when you create a new commit.

```java
public record Head(Type type, String data) {
    public enum Type {
        BRANCH,
        COMMIT,
        UNSET
    }
    public static Head loadHead(Path path) throws IOException;
    public void write(Path path) throws IOException;
    public HashMap<Path, String> getCommitIndex(MinigitObjectLoader loader) throws IOException;
}
```

It should be saved into the .minigit/HEAD file. The `getCommitIndex(MinigitObjectLoader loader)` method
returns a map of paths and their blob hashes that are contained in the commit pointed to by the HEAD.

### Index

Index should be the list of files that are staged for commit together with their hashes. However, throughout the code,
I use the term "index" to mean any list of files (with their hashes)—index of commits, CWD index, staged index...
For that reason, "staged index" is a more appropriate term for "index," but not always is this specified.

### Commit

Before we go into the details of "repository" we need to understand what is a commit.

Commit is a tree, with a message and a timestamp. It is immutable, and has its own SHA-1 hash. It often has
one, or two parents, depending on whether it is a merge commit or not. It can also have zero parents, if it
is the first commit in the repository.

```java
public final class Commit extends MiniGitObject {
    private final String Snapshot;
    private final String[] parents;
    private final String message;
    private final Author author;
    private final Date date;
    private String sha1;
    public final String AUTHOR_HEADER = "AUTHOR";
    
    public Commit(String snapshot, String parent, Author author, String message, Date date);

    @Override
    public boolean equals(Object o);
    
    public Commit(String snapshot, String parentInto, String parentFrom, Author author, String message, Date date);

    public String [] getParents();

    Commit(byte[] byteArray);

    @Override
    void write(Path path) throws IOException;

    @Override
    public String getDescription();

    public String getAnnotatedDescription(Map<String, String> branchNameToHash, Map<String, String> tagNameToHash, Head head, String indent);
    
    @Override
    public String miniGitSha1();

    private byte[] getFileBytes();

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    public String getTreeHash();
}
```

There should be nothing difficult to understand here, as the ideas repeat from the previous structures.
If you want to create the first commit, you use the single-parent constructor but provide null for the parent.

There is also this helper record for when you want to merge two commits. Head is useful to know
whether to warn the user if they changed the head in the meantime, and whether to move a branch.

```java
public record MergingCommits(String fromCommit, Head intoHead, String intoCommit) { }
```

### Repository

Now the largest part—the repository itself. I will not go into detail here, but you can deduce the behavior from the names of the methods below.
The methods are sorted so that you can read through them in the order they are usually called or encountered.

If you want to know their behavior, it's time to look up Javadocs, as I mentioned earlier, as you already have enough knowledge
of the technology used.

The commands now just use all the methods provided. Again, run-time paths are relative to CWD,
paths in the files saved are relative to the repository root.

Note that you use the keyword "new" when creating something new, and "load" when loading something from the file system.
I tried to keep this consistent throughout the codebase.

If you don't call the "save" method, the changes you made will not be saved. The repository also checks
that when you do something with files, you work in the repository's root directory or its subdirectories.

Most of the data is loaded on-demand (i.e. lazy loading). You will notice this mostly because the files you work with in the
repository can be quite huge, whereas minigit memory usage will be much smaller. So, most data-members have their
"ensure loaded"/"load" methods. These are all private, so you will not see them here.

```java
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

    private final Path mainRepoPath;
    private Head head;
    private HashMap<Path, String> stagedIndex;
    private final HashMap<String, String> branches = new HashMap<>();
    private final HashMap<String, String> tags = new HashMap<>();
    private Author currentAuthor = null;
    private MergingCommits mergingCommits = null;
    
    public Repository(Path path);
    public static Repository load(Path loadFrom) throws IOException;
    public void save() throws IOException;
    public Path getRootPath();
    
    public void storeInternally(MiniGitObject obj);
    public MiniGitObject loadFromInternal(String hashOrName) throws IOException;
    
    public boolean addToIndex(Path file);
    public Map<Path, String> getTrackedFiles();
    public void unstageToLastCommit() throws IOException;
    public HashMap<Path, String> getIndexOfWorkingDirectory() throws IOException;
    
    public List<FileStatus> getStagedToLastCommitStatus() throws IOException;
    public List<FileStatus> getWorkingToIndexStatus() throws IOException;
    public boolean workingTreeDirty() throws IOException;
    
    public Head getHead();
    public String getHeadCommitHash();
    public void setHeadToCommit(Commit commit);
    public void setHeadToBranch(String checkoutTo);
    public void checkoutTree(Tree treeToRestore) throws IOException;
    
    public Map<String, String> getBranches();
    public void setBranch(String arg, String hash);
    public void deleteBranch(String branchName);
    public boolean isBranch(String name);
    public Map<String, String> getTags();
    public void setTag(String arg, String data);
    public void deleteTag(String tagName);
    public String getHashFromRef(String ref);
    
    public Author getCurrentAuthor();
    public void setCurrentAuthor(Author author) throws IOException;

    public List<String> getIgnored() throws IOException;

    public boolean isMerging();
    public MergingCommits getMergingCommits();
    public MergeStatus startMerging(MergingCommits mergingCommits) throws IOException;
    public boolean mergeFromIndex(String message) throws IOException;
    public void stopMerge();
}
```

### Command example

As an example, let's look at the 'add' command, as this command is medium-complexity.

Because this command can be used on multiple repositories at once through the project manager,
we do a for loop over all the repositories.

We then do another for loop over all the files the user wants to add, ignoring the files that are ignored by that repository.

We then add the files to the index of those repositories (i.e., stage them). Notice we can use relative paths,
as the paths at run-time are relative to the current working directory.

After that, we have to save the repository; otherwise the changes won't be saved.

```java
public final class AddCommand implements Command {
    @Override
    public String name() {
        return "add";
    }

    @Override
    public String shortHelp() {
        return "Stage files for commit/merge-apply.";
    }

    @Override
    public String help() {
        return "Add/update files into the staging area (index) in repo or all repos in project manager. You can provide list of files to add as arguments.";
    }

    @Override
    public String usage() {
        return "minigit add <file> [<file> ...]";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            IO.println("Incorrect number of arguments. Provide at least 1 file to add.");
            return 1;
        }
        try {
            List<Repository> reposHere = ProjectManager.loadSingleRepoOrReposFromManager(Path.of("./"));
            for (Repository repo : reposHere) {
                IO.println("Updating " + repo.getRootPath() + " ...");
                List<String> toIgnore = repo.getIgnored();
                for (String pattern : args) {
                    for (Path file: FileHelper.getAllFiles(Path.of(pattern), toIgnore)) {
                        boolean successful = repo.addToIndex(file);
                        if (successful) {
                            IO.println("Staging " + file + "...");
                        }
                    }
                }
                repo.save();
                IO.println("Done.");
            }
        }
        catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
```

That's it! Go over the rest of the commands, and you will understand how everything works. If you ever get confused,
look into the Javadocs of the MiniGit classes.
