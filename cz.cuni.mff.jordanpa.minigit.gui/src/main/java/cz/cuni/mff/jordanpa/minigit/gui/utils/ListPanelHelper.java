package cz.cuni.mff.jordanpa.minigit.gui.utils;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.function.Consumer;

/**
 * Panel with a title, a selectable list of items, and a row of buttons below.
 *
 * <p>
 *     Shared by all panels. For files, use FileListPanelHelper.
 * </p>
 *
 * @param <T> The type of the listed items.
 */
public abstract class ListPanelHelper<T> extends VBox {

    /**
     * The list with the items. Subclasses can tweak its selection mode or mouse clicks.
     */
    protected final ListView<T> listView = new ListView<>();

    private final Label title;

    /**
     * Create the panel.
     *
     * @param title The title shown above.
     * @param buttons The buttons shown below the list.
     */
    protected ListPanelHelper(String title, Button... buttons) {
        super(5);
        this.title = new Label(title);
        setPadding(new Insets(5));

        setUpListView();
        getChildren().addAll(this.title, listView, buttonRow(buttons));
    }

    /**
     * Get the list text of an item.
     *
     * @param item The shown item.
     * @return The text of its line in the list.
     */
    protected abstract String itemText(T item);

    /**
     * Get the CSS style of an item.
     *
     * @param item The shown item.
     * @return The style of its line in the list, or empty string.
     */
    protected String itemStyle(T item) {
        return "";
    }

    /**
     * Deselect all items in the list.
     */
    public void clearSelection() {
        listView.getSelectionModel().clearSelection();
    }

    /**
     * Set a callback for when the user clicks an item in the list.
     *
     * @param onItemClicked Called with the clicked item.
     */
    public void setOnItemClicked(Consumer<T> onItemClicked) {
        listView.getSelectionModel().selectedItemProperty()
                .addListener((_, _, newValue) -> {
            if (newValue != null) {
                onItemClicked.accept(newValue);
            }
        });
    }

    /**
     * Change the title above the list.
     *
     * @param text The new title.
     */
    protected void setTitle(String text) {
        title.setText(text);
    }

    private void setUpListView() {
        listView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : itemText(item));
                setStyle(empty ? "" : itemStyle(item));
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
    }

    private static HBox buttonRow(Button[] buttons) {
        HBox row = new HBox(5, buttons);
        if (buttons.length == 0) {
            Button spacerFakeButton = new Button("Bubu");
            spacerFakeButton.setVisible(false);
            row.getChildren().add(spacerFakeButton);
        }
        for (Node button : row.getChildren()) {
            ((Button) button).setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        return row;
    }
}
