package cz.cuni.mff.jordanpa.minigit.gui;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.panels.ConflictBar;
import cz.cuni.mff.jordanpa.minigit.gui.panels.DiffPanel;
import cz.cuni.mff.jordanpa.minigit.gui.panels.StagedPanel;
import cz.cuni.mff.jordanpa.minigit.gui.panels.TreePanel;
import cz.cuni.mff.jordanpa.minigit.gui.panels.UnstagedPanel;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.nio.file.Path;

/**
 * Main window of the MiniGit gui in JavaFX.
 *
 * <p>
 *     Layout is: unstaged files | staged files | diff of the clicked file | commit tree.
 *     Below the file panels there are stage/reset + unstage/commit buttons,
 *     and a bar with the merge in progress and its conflicted files.
 * </p>
 */
public class MiniGitGuiApp extends Application {

    private MiniGitApi api;
    private Stage stage;

    private UnstagedPanel unstagedPanel;
    private StagedPanel stagedPanel;
    private DiffPanel diffPanel;
    private TreePanel treePanel;
    private ConflictBar conflictBar;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Open the repository in the current directory, or prompt the user to create one
        try {
            api = MiniGitApi.open(Path.of(""));
        } catch (MiniGitApiException e) {
            new Alert(Alert.AlertType.ERROR,
                    e.getMessage() + "\nCreate your repository with 'minigit init' in the terminal here first.",
                    ButtonType.OK).showAndWait();
            Platform.exit();
            return;
        }

        BorderPane root = createScreen();

        stage.setTitle("MiniGit Gui");
        stage.setScene(new Scene(root, 1200, 700));
        stage.show();
        refresh();
    }

    private BorderPane createScreen() {
        unstagedPanel = new UnstagedPanel(api, this::refresh);
        stagedPanel = new StagedPanel(api, this::refresh);
        diffPanel = new DiffPanel(api);
        treePanel = new TreePanel(api, this::refresh);
        conflictBar = new ConflictBar(api, this::refresh, file -> {
            unstagedPanel.clearSelection();
            stagedPanel.clearSelection();
            treePanel.clearSelection();
            diffPanel.showConflict(file);
        });

        // Show diff of file/commit if some was clicked, and deselect the other panels
        unstagedPanel.setOnItemClicked(file -> {
            stagedPanel.clearSelection();
            treePanel.clearSelection();
            diffPanel.showUnstaged(file);
        });
        stagedPanel.setOnItemClicked(file -> {
            unstagedPanel.clearSelection();
            treePanel.clearSelection();
            diffPanel.showStaged(file);
        });
        treePanel.setOnItemClicked(commit -> {
            unstagedPanel.clearSelection();
            stagedPanel.clearSelection();
            diffPanel.showCommit(commit);
        });

        SplitPane panels = new SplitPane(unstagedPanel, stagedPanel, diffPanel, treePanel);

        panels.setOrientation(Orientation.HORIZONTAL);
        panels.setDividerPositions(0.25, 0.5, 0.75);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(_ -> refresh());

        HBox bottomRow = new HBox(5, refreshButton, conflictBar);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setPadding(new Insets(5));

        BorderPane root = new BorderPane(panels);
        root.setBottom(bottomRow);

        return root;
    }

    /**
     * Reload the repository status in the background and load it into panels.
     */
    private void refresh() {
        MiniGitBackgroundWorker.run(api::status, (StatusResult status) -> {
            diffPanel.clear();
            unstagedPanel.setFiles(status.unstaged());
            stagedPanel.setFiles(status.staged());
            stagedPanel.setMerging(status.isMerging());
            conflictBar.update(status);
            stage.setTitle("MiniGit Gui ~ " + headText(status));
        });
        MiniGitBackgroundWorker.run(api::log, treePanel::setCommits);
    }

    /**
     * Get pretty description of HEAD.
     *
     * @param status The repository status.
     * @return Description of commit/branch where HEAD points to.
     */
    private static String headText(StatusResult status) {
        return switch (status.head().type()) {
            case BRANCH -> "on branch " + status.head().data();
            case COMMIT -> "detached at " + status.headCommitHash().substring(0, 7);
            case UNSET -> "no commits yet";
        };
    }
}
