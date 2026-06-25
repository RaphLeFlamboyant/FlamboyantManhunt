package me.flamboyant.manhunt.application.services;

import org.bukkit.event.Listener;

/**
 * Service for registering and unregistering Bukkit event listeners.
 * Replaces direct PluginManager access.
 */
public interface EventRegistrationService {
    /**
     * Register a Bukkit event listener.
     * @param listener listener to register (must not be null)
     */
    void registerListener(Listener listener);

    /**
     * Unregister a Bukkit event listener.
     * @param listener listener to unregister (must not be null)
     */
    void unregisterListener(Listener listener);
}
