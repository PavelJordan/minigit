package cz.cuni.mff.jordanpa.minigit.commands;

import cz.cuni.mff.jordanpa.minigit.structures.Blob;
import cz.cuni.mff.jordanpa.minigit.structures.MiniGitObject;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class RestoreCommand implements Command{

    @Override
    public String name() {
        return "restore";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Return the repository to index state (remove unstaged changes).";
    }

    @Override
    public int execute(String[] args) {
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            Set<Path> trackedFiles = repo.getTrackedFiles();
            for (Path path : trackedFiles) {
                Blob currentBlob = new Blob(path);
                MiniGitObject trackedObject = repo.loadFromInternal(repo.trackedFileHash(path));
                if (trackedObject instanceof Blob trackedBlob) {
                    if (!currentBlob.miniGitSha1().equals(trackedBlob.miniGitSha1())) {
                        trackedBlob.writeContentsTo(path);
                    }
                }
                else {
                    IO.println("Error: tracked file is not a blob. Repository is corrupted.");
                    return 1;
                }
            }
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
