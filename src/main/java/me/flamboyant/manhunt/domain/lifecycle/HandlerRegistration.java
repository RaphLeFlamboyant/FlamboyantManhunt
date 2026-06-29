package me.flamboyant.manhunt.domain.lifecycle;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain value object representing a collection of Bukkit event listeners
 * registered for a specific game session. Used to track and cleanup handlers.
 */
public class HandlerRegistration {
    private final GameSessionId sessionId;
    private final List<Listener> listeners;

    public HandlerRegistration(GameSessionId sessionId, List<Listener> listeners) {
        this.sessionId = sessionId;
        this.listeners = Collections.unmodifiableList(new ArrayList<>(listeners));
    }

    public GameSessionId getSessionId() {
        return sessionId;
    }

    public List<Listener> getListeners() {
        return listeners;
    }
}
