package me.flamboyant.manhunt.domain.event;

import me.flamboyant.utils.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of domain event publisher.
 * Events are delivered synchronously to all registered handlers.
 * Thread-safe for single-threaded Bukkit environment.
 */
public class InMemoryEventPublisher implements DomainEventPublisher {
    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (handlers.isEmpty()) {
            // Session already ended, handlers cleared - log warning
            if (Common.plugin != null) {
                Common.plugin.getLogger().warning(
                    "Event published after session ended: " + event.getClass().getSimpleName()
                );
            }
            return;
        }

        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (DomainEventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // Log but continue to next handler
                    if (Common.plugin != null) {
                        Common.plugin.getLogger().severe(
                            "Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage()
                        );
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }
}
