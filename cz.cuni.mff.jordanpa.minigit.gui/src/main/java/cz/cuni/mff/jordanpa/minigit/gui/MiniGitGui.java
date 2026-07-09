package cz.cuni.mff.jordanpa.minigit.gui;

import javafx.application.Application;

/**
 * Entry point of the minigit-gui command - opens the graphical interface for the repository in the current folder.
 * If there is no repository, it prompts the user to create one.
 */
public final class MiniGitGui {

    /**
     * Private constructor to prevent instantiation.
     */
    private MiniGitGui() {}

    static void main(String[] args) {
        Application.launch(MiniGitGuiApp.class, args);
    }
}
