package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.game.GameSessionId;

public class EndGameCommand {
    private final GameSessionId sessionId;
    private final String reason;

    public EndGameCommand(GameSessionId sessionId, String reason) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }

        this.sessionId = sessionId;
        this.reason = reason != null ? reason : "Game ended";
    }

    public GameSessionId getSessionId() {
        return sessionId;
    }

    public String getReason() {
        return reason;
    }
}
