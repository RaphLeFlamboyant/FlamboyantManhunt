package me.flamboyant.manhunt.application.services;

import org.bukkit.event.Listener;

/**
 * Service for registering and unregistering Bukkit event listeners.
 * Replaces direct PluginManager access.
 */
public interface EventRegistrationService extends me.flamboyant.manhunt.domain.services.EventRegistrationPort {
    /**
     * Register a Bukkit event listener.
     * @param listener listener to register (must not be null)
     * @param plugin plugin instance (must not be null)
     */
    void registerEvents(Listener listener, org.bukkit.plugin.Plugin plugin);

    /**
     * Unregister a Bukkit event listener.
     * @param listener listener to unregister (must not be null)
     */
    void unregisterEvents(Listener listener);

    /**
     * Register a Bukkit event listener using default plugin.
     * @param listener listener to register (must not be null)
     */
    default void registerListener(Listener listener) {
        // This method is kept for backward compatibility
        // Implementation will handle the plugin instance
        throw new UnsupportedOperationException("Use registerEvents(Listener, Plugin) instead");
    }

    /**
     * Unregister a Bukkit event listener.
     * @param listener listener to unregister (must not be null)
     */
    default void unregisterListener(Listener listener) {
        unregisterEvents(listener);
    }
}
