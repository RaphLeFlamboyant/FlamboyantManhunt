package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Published when a game session starts and all roles have been assigned.
 */
public class GameStartedEvent extends DomainEvent {
    private final Set<Player> players;
    private final int totalSpeedrunners;

    public GameStartedEvent(GameSessionId sessionId, Set<Player> players, int totalSpeedrunners) {
        super(sessionId);
        if (players == null) {
            throw new IllegalArgumentException("Players cannot be null");
        }
        this.players = Collections.unmodifiableSet(new HashSet<>(players)); // Defensive copy
        this.totalSpeedrunners = totalSpeedrunners;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public int getTotalSpeedrunners() {
        return totalSpeedrunners;
    }
}
