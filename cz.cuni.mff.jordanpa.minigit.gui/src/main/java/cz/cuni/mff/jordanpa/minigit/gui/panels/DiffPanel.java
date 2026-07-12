package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

/**
 * Panel with the diff of the clicked file.
 *
 * <p>
 *     For a file clicked in the unstaged panel, changes are the working directory vs index.
 *     For a file clicked in the staged panel, changes are the index vs last commit.
 * </p>
 * <p>
 *     The whole file is shown. Added lines are green, deleted lines are red.
 * </p>
 */
public final class DiffPanel extends VBox {

    private final MiniGitApi api;
    private final Label title = new Label("Diff");
    private final ListView<DiffLine> listView = new ListView<>();

    /**
     * Create the panel.
     *
     * @param api The MiniGit API.
     */
    public DiffPanel(MiniGitApi api) {
        super(5);
        this.api = api;
        setPadding(new Insets(5));

        setUpListView();

        // Invisible button so the list bottom is aligned with the neighboring panels
        Button spacerFakeButton = new Button("Bubu");
        spacerFakeButton.setVisible(false);

        getChildren().addAll(title, listView, new HBox(5, spacerFakeButton));
    }

    private void setUpListView() {
        listView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(DiffLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(marker(item.type()) + item.line());
                    setStyle(color(item.type()));
                }
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
    }

    /**
     * Show the diff of an unstaged file.
     *
     * @param file The clicked file status.
     */
    public void showUnstaged(Repository.FileStatus file) {
        show("unstaged " + file.path(), () -> api.diffWorkingVsIndex(file.path()));
    }

    /**
     * Show the diff of a staged file.
     *
     * @param file The clicked file status.
     */
    public void showStaged(Repository.FileStatus file) {
        show("staged " + file.path(), () -> api.diffIndexVsHead(file.path()));
    }

    /**
     * Clear the panel.
     */
    public void clear() {
        title.setText("Diff");
        listView.getItems().clear();
    }

    /**
     * Compute a diff in the background and show it.
     *
     * @param shownFile Description of the diffed file for the title.
     * @param diff The API diff call to run.
     */
    private void show(String shownFile, MiniGitBackgroundWorker.ApiThrowingSupplier<List<DiffLine>> diff) {
        MiniGitBackgroundWorker.run(diff, (List<DiffLine> lines) -> {
            title.setText("Diff ~ " + shownFile);
            listView.getItems().setAll(lines);
        });
    }

    /**
     * Get marker for a diff line based on its type.
     *
     * @param type of diff line.
     * @return The marker (empty, minus or plus)
     */
    private static String marker(DiffLine.Type type) {
        return switch (type) {
            case SAME -> "  ";
            case DELETED -> "- ";
            case ADDED -> "+ ";
        };
    }

    /**
     * Get style of a diff line based on its type.
     *
     * @param type of diff line.
     * @return CSS with the line color, or empty string.
     */
    private static String color(DiffLine.Type type) {
        return switch (type) {
            case SAME -> "";
            case DELETED -> "-fx-text-fill: #c02020;";
            case ADDED -> "-fx-text-fill: #208020;";
        };
    }
}
