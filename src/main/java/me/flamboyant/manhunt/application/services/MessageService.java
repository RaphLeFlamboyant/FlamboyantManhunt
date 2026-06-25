package me.flamboyant.manhunt.application.services;

import org.bukkit.entity.Player;

/**
 * Service for sending formatted messages to players and server.
 * Replaces ChatHelper with framework-independent abstraction.
 */
public interface MessageService {
    /**
     * Send a message to a player with color code translation.
     * @param player target player (must not be null)
     * @param message message with '&' color codes (must not be null)
     */
    void sendMessage(Player player, String message);

    /**
     * Broadcast a message to all online players.
     * @param message message with '&' color codes (must not be null)
     */
    void broadcastMessage(String message);

    /**
     * Send a formatted message to a player.
     * @param player target player (must not be null)
     * @param format format string (must not be null)
     * @param args format arguments
     */
    void sendFormattedMessage(Player player, String format, Object... args);
}
