package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

public class HandlersRegisteredEvent extends DomainEvent {

    public HandlersRegisteredEvent(GameSessionId sessionId) {
        super(sessionId);
    }
}
