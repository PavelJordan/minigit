package cz.cuni.mff.jordanpa.minigit.api;

/**
 * Errors that can happen when using MiniGitApi, with pretty messages for the user
 */
public class MiniGitApiException extends Exception {

    /**
     * Create an exception with a user-friendly message.
     *
     * @param message The message to show to the user.
     */
    public MiniGitApiException(String message) {
        super(message);
    }

    /**
     * Create an exception with a user-friendly message and the cause.
     *
     * @param message The message to show to the user.
     * @param cause The exception that caused this error.
     */
    public MiniGitApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
