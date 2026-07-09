/**
 * JavaFX graphical interface for MiniGit. Used with minigit-gui command.
 */
module cz.cuni.mff.jordanpa.minigit.gui {
    requires cz.cuni.mff.jordanpa.minigit;
    requires javafx.controls;
    exports cz.cuni.mff.jordanpa.minigit.gui to javafx.graphics;
}
