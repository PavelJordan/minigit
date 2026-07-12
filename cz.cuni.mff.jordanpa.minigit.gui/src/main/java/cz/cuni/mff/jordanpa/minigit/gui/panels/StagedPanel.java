package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.MiniGitApi;
import cz.cuni.mff.jordanpa.minigit.gui.utils.FileListPanelHelper;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;

import javafx.scene.control.*;

/**
 * Panel with staged changes.
 */
public final class StagedPanel extends FileListPanelHelper {

    /**
     * Create the panel.
     *
     * @param api The MiniGit API.
     * @param refresh Called after a successful operation to reload the status into panels.
     */
    public StagedPanel(MiniGitApi api, Runnable refresh) {
        Button unstage = new Button("Unstage");
        Button commit = new Button("Commit");
        super("Staged", unstage, commit);

        setUpUnstageAction(api, refresh, unstage);
        setUpCommitAction(api, refresh, commit);
    }

    private static void setUpCommitAction(MiniGitApi api, Runnable refresh, Button commit) {
        commit.setOnAction(_ -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Commit");
            dialog.setHeaderText("Commit the staged files");
            dialog.setContentText("Message:");
            dialog.showAndWait().filter(message -> !message.isBlank()).ifPresent(message ->
                    MiniGitBackgroundWorker.run(() -> api.commit(message), (String hash) -> {
                        new Alert(Alert.AlertType.INFORMATION, "Created commit " + hash, ButtonType.OK).showAndWait();
                        refresh.run();
                    }));
        });
    }

    private static void setUpUnstageAction(MiniGitApi api, Runnable refresh, Button unstage) {
        unstage.setOnAction(_ -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Unstage ALL staged changes?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                MiniGitBackgroundWorker.run(api::unstageAll, refresh);
            }
        });
    }
}
