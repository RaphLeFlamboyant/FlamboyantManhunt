package me.flamboyant.manhunt.domain.services;

import org.bukkit.entity.Player;

/**
 * Domain port for sending messages to players.
 * Implemented by application layer MessageService.
 */
public interface MessagingPort {
    void sendMessage(Player player, String message);
    void broadcastMessage(String message);
}
