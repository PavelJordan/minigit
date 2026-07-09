package cz.cuni.mff.jordanpa.minigit.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    /**
     * Constructor for the application. Called from JavaFX - don't call it yourself!
     */
    public MiniGitGuiApp() {}

    private final ListView<String> unstagedList = new ListView<>();
    private final ListView<String> stagedList = new ListView<>();

    private final TextArea diffView = new TextArea();

    // StackPane because it is simple to swap its contents between commit tree and commit details
    private final StackPane treePane = new StackPane(new Label("Commit tree soon"));

    private final Label conflictRow = new Label("Conflicts soon");

    @Override
    public void start(Stage stage) {
        diffView.setEditable(false);

        unstagedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        stagedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        SplitPane panels = new SplitPane(
                panelWithButtons("Unstaged", unstagedList,
                        new Button("Stage"), new Button("Reset"),
                        new Button("All"), new Button("None")),
                panelWithButtons("Staged", stagedList,
                        new Button("Unstage"), new Button("Commit"),
                        new Button("All"), new Button("None")),
                panelWithButtons("Diff", diffView),
                panelWithButtons("Tree", treePane));

        panels.setOrientation(Orientation.HORIZONTAL);
        panels.setDividerPositions(0.25, 0.5, 0.75);

        BorderPane root = new BorderPane(panels);
        root.setBottom(conflictRow);
        BorderPane.setMargin(conflictRow, new Insets(5));

        stage.setTitle("MiniGit Gui");
        stage.setScene(new Scene(root, 1200, 700));
        stage.show();
    }

    /**
     * Wraps a component with a title label above it.
     *
     * @param title title shown above the content
     * @param content the panel content
     * @return the wrapped panel
     */
    private static VBox titled(String title, javafx.scene.Node content) {
        VBox box = new VBox(5, new Label(title), content);
        box.setPadding(new Insets(5));
        VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
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
    private static VBox panelWithButtons(String title, javafx.scene.Node content, Button... buttons) {
        VBox box = titled(title, content);
        HBox row = new HBox(5, buttons);
        if (buttons.length == 0) {
            Button spacer = new Button("spacer");
            spacer.setVisible(false);
            row.getChildren().add(spacer);
        }
        for (javafx.scene.Node button : row.getChildren()) {
            ((Button) button).setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);
        }
        box.getChildren().add(row);
        return box;
    }
}
