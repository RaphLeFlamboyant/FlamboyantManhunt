package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.application.exceptions.CompensationStatus;
import me.flamboyant.manhunt.domain.game.GameSessionId;

public class GameStartFailedEvent extends DomainEvent {
    private final Throwable cause;
    private final CompensationStatus compensationStatus;

    public GameStartFailedEvent(GameSessionId sessionId, Throwable cause,
                               CompensationStatus status) {
        super(sessionId);
        this.cause = cause;
        this.compensationStatus = status;
    }

    public Throwable getCause() {
        return cause;
    }

    public CompensationStatus getCompensationStatus() {
        return compensationStatus;
    }
}
