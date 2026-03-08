# User manual for MiniGit

For the user manual, I will assume that you are familiar with the basics of git. For that reason,
I will focus on explaining what is different from git, and what is new. However, to sum up:


 - `minigit help` shows the help for all commands
 - `minigit help <command>` shows the help for a specific command
 - `minigit init` initializes a repository
 - `minigit author <name> <email>` sets the author of the repository

Now you can use `minigit status` to see what is changed, and what is staged, and `minigit log` to see serialized history.
You then create files, and use `minigit add` to stage them.
You can then use `minigit commit` to commit the staged files.
You can then use `minigit branch` to create branches, and `minigit checkout` to switch to branches (+follow them) or commits.
You can then use `minigit merge` to merge branches or commits into the current commit/branch, and apply with `minigit merge-apply`.
You can then use `minigit tag` to create tags, `minigit restore` to remove unstaged changes, and `minigit restore-staged` to restore staged changes.

## What is the same/similar worth mentioning?

 - You use `.minigitignore` to ignore files. The ignored files will not be able to be staged, and will appear as "deleted files" if they were committed before.
 - You use `minigit checkout` to switch to branches and follow them, or checkout commits, resulting in detached HEAD. Tags are also supported.
 - You use `minigit merge` to merge branches or commits/tags into the current commit/branch. You can then use `minigit merge-apply` to apply the merge.
   If you are on a branch, it will advance it after `merge-apply` with multiple parents. In either case, it will move head to the result.

## What is different?

 - When you ignore a file, the repository will want to remove it in the next commit if it was committed before.
   To do that, you use `minigit add-ignored`, as you cannot `add` ignored files.
 - After using the `minigit merge` command, you need to stage/unstage the files that were changed, based on what you want to be the result,
   and only then use `minigit merge-apply`, instead of `minigit commit`, even if there are no conflicts.
   That is so you can go over the result before creating the merge commit. You can even restore things from different branches,
   checkout somewhere else, and only then apply the merge.
 - After using `minigit merge`, you can use `minigit merge-stop` to stop the merge. The only exception is fast-forward merges.
 - You use `minigit restore-staged` to restore staged files, and `minigit restore` to restore unstaged files.
 - You use `minigit tag-remove` and `minigit branch-delete` to delete branches and tags.
 - You move branches by creating a branch with the same name on a different commit.
 - You can use `minigit refs` to see all branches and tags.
 - You set the author in repos with `minigit author <name> <email>`

## What is new?

 - You can use `minigit show <branch/tag/commit>` to see what changed in that specified commit.
 - You can use `minigit blob <path>` to save contents of a file to database (it is then called a blob)
 - You can use `minigit tree` to save contents of the currently staged files to database (it is then called a tree)
 - You can use `minigit tree-checkout` to load a tree into the working directory. You can browse commits without checking-out this way.
 - You can use `minigit inspect <hash/branch/tag>` to see the contents of some object in database. Can be blob, tree, or commit.
 - You can use `minigit blob-diff <blob1> <blob2>` to see the differences between two blobs given by their hashes.

### Projects

You can use `minigit projects-init` to initialize a project directory. In here, you can create multiple repositories,
and use `minigit project-add <repo_root>` to add them (the repositories have to be in sub-directories).

You can use `minigit projects` to see the list of repositories and `minigit project-remove` to remove the repositories from the list.

When you then use some `minigit` command in this directory, it will apply them to all the repositories in the directory that are added to the list.
This works for:

 - add (adds only the files that belong to the respective repositories)
 - add-ignored
 - author (adds author to all repositories)
 - commit (Creates a commit in all repositories with the same message)
 - restore (restore unstaged changes in all repositories)
 - restore-staged (restore staged changes in all repositories)
 - status (shows statuses of all repositories)
 - tag (creates a tag in all repositories on their current commit)
 - tag-remove (deletes a tag in all repositories)
 - refs (shows all branches and tags in all repositories)

## What is not supported?

 - There is no fetch/push/pull/clone.
 - There is no cherry-picking or rebasing
 - No `diff` between commits, even though it is possible to implement quickly
 - No `diff --staged` (a big one, I'm sorry)
 - No `log --graph`
 - No grep
 - No reset
 - No `mv`
 - No bisecting

All of this can be implemented later, if needed.
