package me.flamboyant.manhunt.infrastructure.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.exceptions.EventRegistrationException;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import org.bukkit.Server;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import javax.inject.Singleton;

/**
 * Bukkit implementation of EventRegistrationService.
 * Manages event listener lifecycle.
 */
@Singleton
public class BukkitEventRegistrationService implements EventRegistrationService {
    private final Server server;
    private final Plugin plugin;

    @Inject
    public BukkitEventRegistrationService(Server server, Plugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void registerEvents(Listener listener, Plugin pluginInstance) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        if (pluginInstance == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        try {
            server.getPluginManager().registerEvents(listener, pluginInstance);
        } catch (Exception e) {
            throw new EventRegistrationException(
                "Failed to register listener: " + listener.getClass().getName(), e);
        }
    }

    @Override
    public void unregisterEvents(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        HandlerList.unregisterAll(listener);
    }

    @Override
    public void registerListener(Listener listener) {
        registerEvents(listener, plugin);
    }

    @Override
    public void unregisterListener(Listener listener) {
        unregisterEvents(listener);
    }
}
