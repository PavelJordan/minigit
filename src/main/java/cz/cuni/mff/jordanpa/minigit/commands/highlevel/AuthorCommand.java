package cz.cuni.mff.jordanpa.minigit.commands.highlevel;

import cz.cuni.mff.jordanpa.minigit.commands.Command;
import cz.cuni.mff.jordanpa.minigit.misc.Author;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Path;

public final class AuthorCommand implements Command {

    @Override
    public String name() {
        return "author";
    }

    @Override
    public String shortHelp() {
        return help();
    }

    @Override
    public String help() {
        return "Set current author for commits. Provide name and email.";
    }

    @Override
    public int execute(String[] args) {
        if (args.length != 2) {
            IO.println("Incorrect number of arguments. Provide exactly 2 arguments: name email.");
            return 1;
        }
        try {
            Repository repo = Repository.load(Path.of(".minigit"));
            repo.setCurrentAuthor(new Author(args[0], args[1]));
            repo.save();
        } catch (IOException e) {
            IO.println(e);
            return 1;
        }
        return 0;
    }
}
