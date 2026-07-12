package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.MiniGitApi;
import cz.cuni.mff.jordanpa.minigit.gui.utils.FileListPanelHelper;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;

import javafx.scene.control.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Panel with the unstaged changes.
 */
public final class UnstagedPanel extends FileListPanelHelper {

    /**
     * Create the panel.
     *
     * @param api The MiniGit API.
     * @param refresh Called after a successful operation to reload the status into panels.
     */
    public UnstagedPanel(MiniGitApi api, Runnable refresh) {
        Button stage = new Button("Stage");
        Button reset = new Button("Reset");
        super("Unstaged", stage, reset);

        setUpStageAction(api, refresh, stage);
        setUpResetAction(api, refresh, reset);
    }

    private static void setUpResetAction(MiniGitApi api, Runnable refresh, Button reset) {
        reset.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Discard ALL unstaged changes of tracked files and overwrite them with the staged versions?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                MiniGitBackgroundWorker.run(api::discardUnstaged, refresh);
            }
        });
    }

    private void setUpStageAction(MiniGitApi api, Runnable refresh, Button stage) {
        stage.setOnAction(event -> {
            List<Path> selection = selectedPaths();
            if (!selection.isEmpty()) {
                MiniGitBackgroundWorker.run(() -> api.stage(selection), refresh);
            }
        });
    }
}
