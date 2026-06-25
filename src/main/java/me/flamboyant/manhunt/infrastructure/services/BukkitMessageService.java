package me.flamboyant.manhunt.infrastructure.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Singleton;

/**
 * Bukkit implementation of MessageService.
 * Uses only Bukkit APIs for message formatting and delivery.
 */
@Singleton
public class BukkitMessageService implements MessageService {
    private final Server server;

    @Inject
    public BukkitMessageService(Server server) {
        this.server = server;
    }

    @Override
    public void sendMessage(Player player, String message) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning("Attempted to send message to offline player: " + player.getName());
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void broadcastMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        server.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void sendFormattedMessage(Player player, String format, Object... args) {
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        sendMessage(player, String.format(format, args));
    }
}
