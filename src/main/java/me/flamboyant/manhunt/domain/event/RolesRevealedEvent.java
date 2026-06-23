package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

/**
 * Published when surprise mode ends and roles are revealed to players.
 */
public class RolesRevealedEvent extends DomainEvent {
    public RolesRevealedEvent(GameSessionId sessionId) {
        super(sessionId);
    }
}
