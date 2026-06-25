package me.flamboyant.manhunt.application.exceptions;

/**
 * Exception thrown when event registration fails.
 */
public class EventRegistrationException extends RuntimeException {
    public EventRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
