package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

import java.util.UUID;

/**
 * Base class for all domain events in the Manhunt game.
 * Domain events represent significant business moments that have occurred.
 */
public abstract class DomainEvent {
    private final String eventId;
    private final long occurredAtMillis;
    private final GameSessionId sessionId;

    /**
     * Creates a new domain event.
     *
     * @param sessionId The ID of the game session that produced this event
     * @throws IllegalArgumentException if sessionId is null
     */
    protected DomainEvent(GameSessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        this.eventId = UUID.randomUUID().toString();
        this.occurredAtMillis = System.currentTimeMillis();
        this.sessionId = sessionId;
    }

    /**
     * @return Unique identifier for this event instance
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return Timestamp (milliseconds since epoch) when this event occurred
     */
    public long getOccurredAtMillis() {
        return occurredAtMillis;
    }

    /**
     * @return The game session that produced this event
     */
    public GameSessionId getSessionId() {
        return sessionId;
    }
}
