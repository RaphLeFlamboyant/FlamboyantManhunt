package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

/**
 * Published when the Ender Dragon is killed.
 * This is a critical business event that triggers win condition evaluation.
 */
public class DragonKilledEvent extends DomainEvent {
    private final Player killer; // May be null if environmental kill

    public DragonKilledEvent(GameSessionId sessionId, Player killer) {
        super(sessionId);
        this.killer = killer; // Nullable
    }

    public Player getKiller() {
        return killer;
    }

    public boolean hasKiller() {
        return killer != null;
    }
}
