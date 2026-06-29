package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;

/**
 * Published when any win condition evaluates to true.
 * Indicates that the game should end with the specified outcome.
 */
public class WinConditionMetEvent extends DomainEvent {
    private final WinOutcome outcome;

    public WinConditionMetEvent(GameSessionId sessionId, WinOutcome outcome) {
        super(sessionId);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        this.outcome = outcome;
    }

    public WinOutcome getOutcome() {
        return outcome;
    }
}
