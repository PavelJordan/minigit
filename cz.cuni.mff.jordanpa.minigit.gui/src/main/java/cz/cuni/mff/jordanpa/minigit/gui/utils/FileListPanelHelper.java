package cz.cuni.mff.jordanpa.minigit.gui.utils;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import javafx.scene.control.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Panel with a title, list of files which are selectable, and a row of buttons below.
 *
 * <p>
 *     Shared by unstaged and staged panels. Files have type of change next to them - [M]/[D]/[N]/[S].
 *     The panel also provides "All"/"None" selection buttons, but the panel-specific action
 *     buttons must be passed via constructor.
 * </p>
 */
public class FileListPanelHelper extends ListPanelHelper<Repository.FileStatus> {

    /**
     * Create the panel.
     *
     * @param title The title shown above.
     * @param actionButtons The panel-specific buttons shown in front of "All"/"None" buttons.
     */
    public FileListPanelHelper(String title, Button... actionButtons) {
        Button all = new Button("All");
        Button none = new Button("None");
        super(title, Stream.concat(Arrays.stream(actionButtons), Stream.of(all, none)).toArray(Button[]::new));

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        all.setOnAction(_ -> listView.getSelectionModel().selectAll());
        none.setOnAction(_ -> clearSelection());
    }

    @Override
    protected String itemText(Repository.FileStatus item) {
        return marker(item.status()) + " " + item.path();
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
     * Get the paths of the currently selected files.
     *
     * @return The selected CWD-relative paths.
     */
    public List<Path> selectedPaths() {
        return listView.getSelectionModel().getSelectedItems()
                .stream().map(Repository.FileStatus::path).toList();
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
