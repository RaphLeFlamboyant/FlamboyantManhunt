package me.flamboyant.manhunt.domain.services;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Domain port for Bukkit event registration.
 * Implemented by application layer EventRegistrationService.
 */
public interface EventRegistrationPort {
    void registerEvents(Listener listener, Plugin plugin);
    void unregisterEvents(Listener listener);
}
