package me.flamboyant.manhunt.domain.services;

import me.flamboyant.manhunt.domain.game.GameSession;
import org.bukkit.entity.Player;

/**
 * Domain port for retrieving game sessions.
 * Implemented by application layer GameSessionManager.
 */
public interface SessionRepository {
    GameSession getActiveSessionForPlayer(Player player);
}
