package cz.cuni.mff.jordanpa.minigit.gui.panels;

import cz.cuni.mff.jordanpa.minigit.api.*;
import cz.cuni.mff.jordanpa.minigit.gui.utils.ListPanelHelper;
import cz.cuni.mff.jordanpa.minigit.gui.utils.MiniGitBackgroundWorker;
import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.util.List;

/**
 * Panel with the diff of the clicked file.
 *
 * <p>
 *     For a file clicked in the unstaged panel, changes are the working directory vs index.
 *     For a file clicked in the staged panel, changes are the index vs last commit.
 *     For a commit clicked in the tree panel, changes are the commit vs its previous commit,
 *     with the changed files separated by header lines.
 * </p>
 * <p>
 *     The whole file is shown. Added lines are green, deleted lines are red.
 * </p>
 */
public final class DiffPanel extends ListPanelHelper<DiffLine> {

    private final MiniGitApi api;

    /**
     * Create the panel.
     *
     * @param api The MiniGit API.
     */
    public DiffPanel(MiniGitApi api) {
        super("Diff");
        this.api = api;
    }

    @Override
    protected String itemText(DiffLine item) {
        return marker(item.type()) + item.line();
    }

    @Override
    protected String itemStyle(DiffLine item) {
        return color(item.type());
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
     * Show the diff of a commit against its previous commit.
     *
     * @param commit The clicked commit.
     */
    public void showCommit(CommitInfo commit) {
        show("commit " + commit.hash().substring(0, 7), () -> api.diffCommitVsParent(commit.hash()));
    }

    /**
     * Clear the panel.
     */
    public void clear() {
        setTitle("Diff");
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
            setTitle("Diff ~ " + shownFile);
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
            case SAME, HEADER -> "  ";
            case DELETED -> "- ";
            case ADDED -> "+ ";
        };
    }

    /**
     * Get style of a diff line based on its type.
     *
     * @param type of diff line.
     * @return CSS with the line color (bold for header), or empty string.
     */
    private static String color(DiffLine.Type type) {
        return switch (type) {
            case SAME -> "";
            case DELETED -> "-fx-text-fill: #c02020;";
            case ADDED -> "-fx-text-fill: #208020;";
            case HEADER -> "-fx-font-weight: bold;";
        };
    }
}
