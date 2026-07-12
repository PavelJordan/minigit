package cz.cuni.mff.jordanpa.minigit.gui.utils;

import cz.cuni.mff.jordanpa.minigit.api.MiniGitApiException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Designed so lengthy MiniGit API calls can be easily ran in background with finish hook.
 *
 * <p>
 *     Each call runs in its own virtual thread. On error a dialog is shown.
 * </p>
 */
public final class MiniGitBackgroundWorker {

    /**
     * Private constructor to prevent instantiation.
     */
    private MiniGitBackgroundWorker() {}

    private final static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Run an API call in the background which returns something. Shows user an error if something fails.
     *
     * @param work The API call to run.
     * @param onSuccess Called with the result when the call succeeds.
     * @param <T> The type of the result.
     */
    public static <T> void run(ApiThrowingSupplier<T> work, Consumer<T> onSuccess){
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return work.supply();
                    } catch (MiniGitApiException exception) {
                        throw new CompletionException(exception);
                    }
                }, executor).whenCompleteAsync((result, exception) -> {
                    if (exception != null) {
                        showError((MiniGitApiException) exception.getCause());
                    } else {
                        onSuccess.accept(result);
                    }
                }, Platform::runLater);
    }

    /**
     * Run an API call in the background which does not return anything. Shows user an error if something fails.
     *
     * @param work The API call to run.
     * @param onSuccess Called when the call succeeds.
     */
    public static void run(ApiThrowingRunnable work, Runnable onSuccess) {
        run(() -> {
            work.run();
            return null;
        }, _ -> onSuccess.run());
    }

    /**
     * Runnable, which may throw a MiniGitAPI exception with a pretty message.
     */
    @FunctionalInterface
    public interface ApiThrowingRunnable {
        void run() throws MiniGitApiException;
    }

    /**
     * Supplier, which may throw a MiniGitAPI exception with a pretty message.
     */
    @FunctionalInterface
    public interface ApiThrowingSupplier<T> {
        T supply() throws MiniGitApiException;
    }

    /**
     * Show an error dialog to the user for the given exception.
     *
     * @param exception The exception with pretty message to show.
     */
    private static void showError(MiniGitApiException exception) {
        new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK).showAndWait();
    }
}
