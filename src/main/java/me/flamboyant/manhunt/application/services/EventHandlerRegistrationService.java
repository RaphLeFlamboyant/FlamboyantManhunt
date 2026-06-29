package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.flamboyant.manhunt.domain.lifecycle.HandlerRegistration;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.HandlersRegisteredEvent;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

@Singleton
public class EventHandlerRegistrationService {
    private final Plugin plugin;
    private final DomainEventPublisher eventPublisher;

    @Inject
    public EventHandlerRegistrationService(@Named("plugin") Plugin plugin,
                                           DomainEventPublisher eventPublisher) {
        this.plugin = plugin;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers Bukkit event handlers for a game session.
     * Returns registration handle for cleanup.
     * Publishes: HandlersRegisteredEvent
     *
     * @param sessionId Session ID
     * @param listeners Listeners to register
     * @return Handler registration for cleanup
     */
    public HandlerRegistration registerHandlers(GameSessionId sessionId, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        eventPublisher.publish(new HandlersRegisteredEvent(sessionId));

        return new HandlerRegistration(sessionId, Arrays.asList(listeners));
    }

    /**
     * Unregisters handlers.
     * Idempotent - safe to call multiple times.
     *
     * @param registration Handler registration to unregister
     */
    public void unregisterHandlers(HandlerRegistration registration) {
        if (registration == null) {
            return;
        }

        for (Listener listener : registration.getListeners()) {
            HandlerList.unregisterAll(listener);
        }
    }
}
