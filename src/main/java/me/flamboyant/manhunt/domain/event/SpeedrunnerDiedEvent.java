package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

/**
 * Published when a speedrunner takes fatal damage and dies.
 * This is a critical business event that triggers win condition evaluation.
 */
public class SpeedrunnerDiedEvent extends DomainEvent {
    private final Player player;
    private final int remainingSpeedrunners;

    public SpeedrunnerDiedEvent(GameSessionId sessionId, Player player, int remainingSpeedrunners) {
        super(sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
        this.remainingSpeedrunners = remainingSpeedrunners;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRemainingSpeedrunners() {
        return remainingSpeedrunners;
    }
}
