package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;
import cz.cuni.mff.jordanpa.minigit.misc.Merger;
import cz.cuni.mff.jordanpa.minigit.structures.*;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Bar with the merge in progress and its conflicts.
 *
 * <p>
 *     Hidden when no merge is in progress. During a merge it shows what is being merged
 *     and the conflicted files.
 * </p>
 */
public final class ConflictBar extends HBox {

    private final MiniGitApi api;
    private final Runnable refresh;
    private final Consumer<Path> onFileClicked;

    /**
     * Conflicted files the user has not staged yet
     */
    private List<Path> unresolved = List.of();

    /**
     * Create the bar.
     *
     * @param api The MiniGit API.
     * @param refresh Called after a successful operation to reload the status into panels.
     * @param onFileClicked The path of a clicked conflicted file.
     */
    public ConflictBar(MiniGitApi api, Runnable refresh, Consumer<Path> onFileClicked) {
        super(5);
        this.api = api;
        this.refresh = refresh;
        this.onFileClicked = onFileClicked;
        setAlignment(Pos.CENTER_LEFT);
        setVisible(false);
        setManaged(false);
    }

    /**
     * Show the merge in progress from the status, or hide the bar if no merge is in progress.
     *
     * @param status The repository status.
     */
    public void update(StatusResult status) {
        setVisible(status.isMerging());
        setManaged(status.isMerging());
        if (!status.isMerging()) {
            getChildren().clear();
            return;
        }

        // A conflicted file still in the unstaged changes was not resolved (= staged) yet
        Set<Path> unstaged = status.unstaged().stream()
                .map(Repository.FileStatus::path).collect(Collectors.toSet());
        unresolved = status.conflicts().stream()
                .map(Merger.Conflict::path).filter(unstaged::contains).toList();

        getChildren().setAll(new Label("Merging " + status.merging().fromCommit().substring(0, 7)
                + " into " + headText(status.merging().intoHead())
                + (status.conflicts().isEmpty() ? "" : " ~ conflicts:")));
        for (Merger.Conflict conflict : status.conflicts()) {
            Hyperlink file = new Hyperlink(conflict.path().toString());
            file.setTooltip(new Tooltip(conflict.message()));
            file.setOnAction(_ -> onFileClicked.accept(conflict.path()));
            getChildren().add(file);
        }

        Button apply = new Button("Merge apply");
        Button stop = new Button("Merge stop");
        apply.setOnAction(_ -> applyMerge());
        stop.setOnAction(_ -> stopMerge());
        getChildren().addAll(apply, stop);
    }

    /**
     * Create the merge commit from the staged files, asking for the commit message.
     * If some conflicted files are not staged yet, ask for confirmation first.
     */
    private void applyMerge() {
        if (!unresolved.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "These conflicted files are not staged yet:\n" + pathList(unresolved)
                            + "\nThe merge commit will contain their versions from before the merge. Apply anyway?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Merge apply");
        dialog.setHeaderText("Create the merge commit from the staged files");
        dialog.setContentText("Message:");
        dialog.showAndWait().filter(message -> !message.isBlank()).ifPresent(message ->
                MiniGitBackgroundWorker.run(() -> api.mergeApply(message), refresh));
    }

    /**
     * Stop the merge in progress, asking for confirmation.
     */
    private void stopMerge() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Stop the merge? The working directory keeps its current contents.",
                ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            MiniGitBackgroundWorker.run(api::mergeStop, refresh);
        }
    }

    /**
     * Get pretty description of the head being merged into.
     *
     * @param head The head the merge started on.
     * @return The branch name, or the short commit hash when detached.
     */
    private static String headText(Head head) {
        return head.type() == Head.Type.BRANCH ? "branch " + head.data() : "commit " + head.data().substring(0, 7);
    }

    /**
     * Get the paths as a multi-line list for a dialog.
     *
     * @param paths The paths to list.
     * @return One path per line.
     */
    private static String pathList(List<Path> paths) {
        return paths.stream().map(Path::toString).collect(Collectors.joining("\n"));
    }
}
