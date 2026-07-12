package cz.cuni.mff.jordanpa.minigit.gui;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.panels.StagedPanel;
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
 *     and a row listing files with merge conflicts.
 * </p>
 */
public class MiniGitGuiApp extends Application {

    private MiniGitApi api;
    private Stage stage;

    private UnstagedPanel unstagedPanel;
    private StagedPanel stagedPanel;
    private final TextArea diffView = new TextArea();
    private final Label conflictRow = new Label("Conflicts soon");

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

        // TODO Reload status when files change (some listener to file system? Or simply on-focus? Or button?)

        stage.setTitle("MiniGit Gui");
        stage.setScene(new Scene(root, 1200, 700));
        stage.show();
        refresh();
    }

    private BorderPane createScreen() {
        unstagedPanel = new UnstagedPanel(api, this::refresh);
        stagedPanel = new StagedPanel(api, this::refresh);

        // Show diff of file if some was clicked
        unstagedPanel.setOnFileClicked(file -> diffView.setText("Diff of unstaged " + file.path()));
        stagedPanel.setOnFileClicked(file -> diffView.setText("Diff of staged " + file.path()));
        diffView.setEditable(false);

        SplitPane panels = new SplitPane(unstagedPanel, stagedPanel, panelWithButtons("Diff", diffView),
                panelWithButtons("Tree", new Label("Commit tree will be here")));

        panels.setOrientation(Orientation.HORIZONTAL);
        panels.setDividerPositions(0.25, 0.5, 0.75);

        BorderPane root = new BorderPane(panels);
        root.setBottom(conflictRow);
        BorderPane.setMargin(conflictRow, new Insets(5));

        return root;
    }

    /**
     * Reload the repository status in the background and load it into panels.
     */
    private void refresh() {
        MiniGitBackgroundWorker.run(api::status, (StatusResult status) -> {
            unstagedPanel.setFiles(status.unstaged());
            stagedPanel.setFiles(status.staged());
            stage.setTitle("MiniGit Gui ~ " + headText(status));
        });
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

    /**
     * Wraps a component with a title label above it.
     *
     * @param title title shown above the content
     * @param content the panel content
     * @return the wrapped panel
     */
    private static VBox titled(String title, Node content) {
        VBox box = new VBox(5, new Label(title), content);
        box.setPadding(new Insets(5));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    /**
     * Wraps a component with a title above and a row of buttons below.
     *
     * @param title title shown above the content
     * @param content the panel content
     * @param buttons buttons shown below the content
     * @return the wrapped panel
     */
    private static VBox panelWithButtons(String title, Node content, Button... buttons) {
        VBox box = titled(title, content);
        HBox row = new HBox(5, buttons);
        if (buttons.length == 0) {
            Button spacer = new Button("spacer");
            spacer.setVisible(false);
            row.getChildren().add(spacer);
        }
        for (Node button : row.getChildren()) {
            ((Button) button).setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        box.getChildren().add(row);
        return box;
    }
}
