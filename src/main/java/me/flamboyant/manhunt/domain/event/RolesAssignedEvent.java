package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

public class RolesAssignedEvent extends DomainEvent {

    public RolesAssignedEvent(GameSessionId sessionId) {
        super(sessionId);
    }
}
