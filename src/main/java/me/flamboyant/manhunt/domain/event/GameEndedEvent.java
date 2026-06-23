package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;

/**
 * Published when a game session ends, either due to win condition being met
 * or manual termination.
 */
public class GameEndedEvent extends DomainEvent {
    private final WinOutcome outcome;
    private final String reason;

    public GameEndedEvent(GameSessionId sessionId, WinOutcome outcome, String reason) {
        super(sessionId);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reason cannot be null");
        }
        this.outcome = outcome;
        this.reason = reason;
    }

    public WinOutcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }
}
