package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.utils.ListPanelHelper;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;

import javafx.scene.control.*;
import java.util.*;

/**
 * Panel with the commit history of the repository.
 *
 * <p>
 *     Each commit is one line - short hash, branches/tags pointing to it, and the message.
 *     Double-clicking a commit shows its details. The "Checkout" button moves to a
 *     branch/tag/selected commit picked from a list, the "Make branch" button creates a branch at HEAD.
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
        Button makeBranch = new Button("Make branch");
        super("Tree", checkout, makeBranch);

        setUpCheckoutAction(api, refresh, checkout);
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
     * One checkout target the user can pick in the checkout dialog.
     *
     * @param label What the user sees in the list.
     * @param ref The branch name, tag name, or commit hash to pass to the API.
     */
    private record CheckoutTarget(String label, String ref) {
        @Override
        public String toString() {
            return label;
        }
    }

    private void setUpCheckoutAction(MiniGitApi api, Runnable refresh, Button checkout) {
        checkout.setOnAction(_ -> MiniGitBackgroundWorker.run(api::refs, (List<String> refs) -> {
            List<CheckoutTarget> targets = new ArrayList<>(refs.stream()
                    .map(ref -> new CheckoutTarget(ref, ref)).toList());
            CommitInfo selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                targets.add(new CheckoutTarget(oneLine(selected), selected.hash()));
            }

            ChoiceDialog<CheckoutTarget> dialog = new ChoiceDialog<>(null, targets);
            dialog.setTitle("Checkout");
            dialog.setHeaderText("Checkout a branch, tag, or the selected commit");
            dialog.setContentText("Target:");
            dialog.showAndWait().ifPresent(target ->
                    MiniGitBackgroundWorker.run(() -> api.checkout(target.ref()), refresh));
        }));
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
