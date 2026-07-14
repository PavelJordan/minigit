package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.utils.ListPanelHelper;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import javafx.scene.control.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel with the commit history of the repository.
 *
 * <p>
 *     Each commit is one line - short hash, branches/tags pointing to it, and the message.
 *     Double-clicking a commit shows its details. The "Checkout" button moves to a
 *     branch/tag/selected commit picked from a list, the "Merge" button merges the target
 *     into HEAD and the "Make branch" button creates a branch at HEAD.
 * </p>
 */
public final class TreePanel extends ListPanelHelper<CommitInfo> {

    /**
     * Create the panel.
     *
     * @param api The MiniGit API.
     * @param refresh Called after a successful operation to reload the status into panels.
     */
    public TreePanel(MiniGitApi api, Runnable refresh) {
        Button checkout = new Button("Checkout");
        Button merge = new Button("Merge");
        Button makeBranch = new Button("Branch");
        super("Tree", checkout, merge, makeBranch);

        setUpCheckoutAction(api, refresh, checkout);
        setUpMergeAction(api, refresh, merge);
        setUpMakeBranchAction(api, refresh, makeBranch);
        setUpDetailsOnDoubleClick();
    }

    @Override
    protected String itemText(CommitInfo commit) {
        return oneLine(commit);
    }

    @Override
    protected String itemStyle(CommitInfo commit) {
        return commit.isHead() ? "-fx-font-weight: bold;" : "";
    }

    /**
     * Replace the shown commits.
     *
     * @param commits The commits to show, newest first.
     */
    public void setCommits(List<CommitInfo> commits) {
        listView.getItems().setAll(commits);
    }

    /**
     * One target the user can pick in the checkout/merge dialog.
     *
     * @param label What the user sees in the list.
     * @param ref The branch name, tag name, or commit hash to pass to the API.
     */
    private record RefTarget(String label, String ref) {
        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Let the user pick a branch, tag, or the currently selected commit.
     *
     * @param api The MiniGit API.
     * @param title The dialog title.
     * @param header The dialog header text.
     * @param onPicked Called with the picked target.
     */
    private void pickTarget(MiniGitApi api, String title, String header, Consumer<RefTarget> onPicked) {
        MiniGitBackgroundWorker.run(api::refs, (List<String> refs) -> {
            List<RefTarget> targets = new ArrayList<>(refs.stream()
                    .map(ref -> new RefTarget(ref, ref)).toList());
            CommitInfo selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                targets.add(new RefTarget(oneLine(selected), selected.hash()));
            }

            ChoiceDialog<RefTarget> dialog = new ChoiceDialog<>(null, targets);
            dialog.setTitle(title);
            dialog.setHeaderText(header);
            dialog.setContentText("Target:");
            dialog.showAndWait().ifPresent(onPicked);
        });
    }

    private void setUpCheckoutAction(MiniGitApi api, Runnable refresh, Button checkout) {
        checkout.setOnAction(_ -> pickTarget(api, "Checkout", "Checkout a branch, tag, or the selected commit",
                target -> MiniGitBackgroundWorker.run(() -> api.checkout(target.ref()), refresh)));
    }

    /**
     * The result of starting a merge.
     *
     * @param status The merge status returned by the API.
     * @param stillMerging Whether the merge still waits for "Merge apply". Look into CLI on how this works in detail.
     */
    private record MergeOutcome(Repository.MergeStatus status, boolean stillMerging) { }

    private void setUpMergeAction(MiniGitApi api, Runnable refresh, Button merge) {
        merge.setOnAction(_ -> pickTarget(api, "Merge", "Merge a branch, tag, or the selected commit into HEAD",
                target -> MiniGitBackgroundWorker.run(
                        () -> new MergeOutcome(api.merge(target.ref()), api.status().isMerging()),
                        (MergeOutcome outcome) -> {
                            showMergeResult(outcome);
                            refresh.run();
                        })));
    }

    /**
     * Show a dialog with the result of a started merge.
     *
     * @param outcome The result of the merge.
     */
    private static void showMergeResult(MergeOutcome outcome) {
        switch (outcome.status()) {
            case INVALID -> new Alert(Alert.AlertType.ERROR, "This merge is impossible.",
                    ButtonType.OK).showAndWait();
            case CONFLICT -> new Alert(Alert.AlertType.INFORMATION,
                    "Conflicts detected. Resolve the files in the bottom bar, stage them, then press Merge apply.",
                    ButtonType.OK).showAndWait();
            case APPLIED -> new Alert(Alert.AlertType.INFORMATION,
                    outcome.stillMerging()
                            ? "Merged cleanly. Review the staged changes and press Merge apply to create the merge commit."
                            : "Merge successful.",
                    ButtonType.OK).showAndWait();
        }
    }

    private void setUpMakeBranchAction(MiniGitApi api, Runnable refresh, Button makeBranch) {
        makeBranch.setOnAction(_ -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Make branch");
            dialog.setHeaderText("Create a branch pointing to HEAD");
            dialog.setContentText("Name:");
            dialog.showAndWait().filter(name -> !name.isBlank()).ifPresent(name ->
                    MiniGitBackgroundWorker.run(() -> api.makeBranch(name), refresh));
        });
    }

    private void setUpDetailsOnDoubleClick() {
        listView.setOnMouseClicked(event -> {
            CommitInfo commit = listView.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && commit != null) {
                showCommitDetails(commit);
            }
        });
    }

    /**
     * Get one-line description of a commit for the list.
     *
     * @param commit The commit to describe.
     * @return Short hash, refs pointing to the commit, and the first line of the message.
     */
    private static String oneLine(CommitInfo commit) {
        return commit.hash().substring(0, 7) + decorateWithInfo(commit) + " "
                + commit.message().lines().findFirst().orElse("");
    }

    /**
     * Get the "(HEAD, branch, tag: name)" decoration of a commit.
     *
     * @param commit The commit to decorate.
     * @return The decoration, or empty string if no refs point to the commit.
     */
    private static String decorateWithInfo(CommitInfo commit) {
        List<String> refs = new ArrayList<>();
        if (commit.isHead()) {
            refs.add("HEAD");
        }
        refs.addAll(commit.branches());
        commit.tags().forEach(tag -> refs.add("tag: " + tag));
        return refs.isEmpty() ? "" : " (" + String.join(", ", refs) + ")";
    }

    /**
     * Show a dialog with the details of a double-clicked commit.
     *
     * @param commit The commit to show.
     */
    private static void showCommitDetails(CommitInfo commit) {
        String details = "Author: " + commit.author().name() + " <" + commit.author().email() + ">"
                + "\nDate: " + commit.date() + "\nParents: " + (commit.parents().isEmpty() ? "-" : String.join(", ", commit.parents()))
                + "\n\n" + commit.message();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, details, ButtonType.OK);
        alert.setHeaderText("Commit " + commit.hash());
        alert.showAndWait();
    }
}
