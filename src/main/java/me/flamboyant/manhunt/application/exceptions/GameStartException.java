package me.flamboyant.manhunt.application.exceptions;

public class GameStartException extends Exception {

    public GameStartException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameStartException(String message) {
        super(message);
    }
}
