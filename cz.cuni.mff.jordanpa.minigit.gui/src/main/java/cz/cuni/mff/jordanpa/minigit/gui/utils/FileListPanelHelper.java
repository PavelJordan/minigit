package cz.cuni.mff.jordanpa.minigit.gui.utils;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel with a title, list of files which are selectable, and a row of buttons below.
 *
 * <p>
 *     Shared by unstaged and staged panels. Files have type of change next to them - [M]/[D]/[N]/[S].
 *     The panel also provides "All"/"None" selection buttons, but the panel-specific action
 *     buttons must be passed via constructor.
 * </p>
 */
public class FileListPanelHelper extends VBox {

    private final ListView<Repository.FileStatus> listView = new ListView<>();

    /**
     * Create the panel.
     *
     * @param title The title shown above.
     * @param actionButtons The panel-specific buttons shown in front of "All"/"None" buttons.
     */
    public FileListPanelHelper(String title, Button... actionButtons) {
        super(5);
        setPadding(new Insets(5));

        setUpListView();
        HBox rowOfButtons = setUpButtons(actionButtons);

        getChildren().addAll(new Label(title), listView, rowOfButtons);
    }

    private HBox setUpButtons(Button[] actionButtons) {
        Button all = new Button("All");
        all.setOnAction(_ -> listView.getSelectionModel().selectAll());
        Button none = new Button("None");
        none.setOnAction(_ -> clearSelection());

        HBox row = new HBox(5);
        row.getChildren().addAll(actionButtons);
        row.getChildren().addAll(all, none);
        for (Node button : row.getChildren()) {
            ((Button) button).setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        return row;
    }

    private void setUpListView() {
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Repository.FileStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : marker(item.status()) + " " + item.path());
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
    }

    /**
     * Replace the shown files, keeping the selection of files that are still present.
     *
     * @param files The new file statuses to show.
     */
    public void setFiles(List<Repository.FileStatus> files) {
        Set<Path> previouslySelected = new HashSet<>(selectedPaths());
        listView.getItems().setAll(files);
        listView.getSelectionModel().clearSelection();
        for (int i = 0; i < files.size(); i++) {
            if (previouslySelected.contains(files.get(i).path())) {
                listView.getSelectionModel().select(i);
            }
        }
    }

    /**
     * Deselect all files in the list.
     *
     * <p>
     *     Used by the main window to deselect this panel when the user selects a file in another one.
     * </p>
     */
    public void clearSelection() {
        listView.getSelectionModel().clearSelection();
    }

    /**
     * Get the paths of the currently selected files.
     *
     * @return The selected CWD-relative paths.
     */
    public List<Path> selectedPaths() {
        return listView.getSelectionModel().getSelectedItems()
                .stream().map(Repository.FileStatus::path).toList();
    }

    /**
     * Set a callback for when the user clicks a file in the list.
     *
     * <p>
     *     Used by the main window to show a diff of the clicked file in its diff panel.
     * </p>
     *
     * @param onFileClicked Called with the clicked file status.
     */
    public void setOnFileClicked(Consumer<Repository.FileStatus> onFileClicked) {
        listView.getSelectionModel().selectedItemProperty()
                .addListener((_, _, newValue) -> {
            if (newValue != null) {
                onFileClicked.accept(newValue);
            }
        });
    }

    /**
     * Get state marker of a change type.
     *
     * @param status The change type.
     * @return The marker shown before the file path.
     */
    private static String marker(Repository.FileStatusType status) {
        return switch (status) {
            case MODIFIED -> "[M]";
            case DELETED -> "[D]";
            case NEW -> "[N]";
            case SAME -> "[S]";
        };
    }
}
