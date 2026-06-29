package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;

public class EndGameCommand {
    private final GameSessionId sessionId;
    private final String reason;
    private final WinOutcome winOutcome;

    public EndGameCommand(GameSessionId sessionId, String reason) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }

        this.sessionId = sessionId;
        this.reason = reason != null ? reason : "Game ended";
        this.winOutcome = null;
    }

    private EndGameCommand(GameSessionId sessionId, WinOutcome winOutcome) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (winOutcome == null) {
            throw new IllegalArgumentException("Win outcome cannot be null");
        }

        this.sessionId = sessionId;
        this.winOutcome = winOutcome;
        this.reason = winOutcome.getDescription();
    }

    public static EndGameCommand of(GameSessionId sessionId, WinOutcome winOutcome) {
        return new EndGameCommand(sessionId, winOutcome);
    }

    public GameSessionId getSessionId() {
        return sessionId;
    }

    public String getReason() {
        return reason;
    }

    public WinOutcome getWinOutcome() {
        return winOutcome;
    }
}
