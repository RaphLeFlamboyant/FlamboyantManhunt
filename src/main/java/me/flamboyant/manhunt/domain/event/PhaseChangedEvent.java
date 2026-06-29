package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GamePhase;
import me.flamboyant.manhunt.domain.game.GameSessionId;

/**
 * Published when the game session transitions from one phase to another.
 * Allows handlers to react to phase changes and trigger phase-specific behavior.
 */
public class PhaseChangedEvent extends DomainEvent {
    private final GamePhase oldPhase;
    private final GamePhase newPhase;

    public PhaseChangedEvent(GameSessionId sessionId, GamePhase oldPhase, GamePhase newPhase) {
        super(sessionId);
        if (oldPhase == null) {
            throw new IllegalArgumentException("Old phase cannot be null");
        }
        if (newPhase == null) {
            throw new IllegalArgumentException("New phase cannot be null");
        }
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }

    public GamePhase getOldPhase() {
        return oldPhase;
    }

    public GamePhase getNewPhase() {
        return newPhase;
    }

    @Override
    public String toString() {
        return String.format("PhaseChangedEvent[session=%s, %s -> %s]",
            getSessionId(), oldPhase, newPhase);
    }
}
