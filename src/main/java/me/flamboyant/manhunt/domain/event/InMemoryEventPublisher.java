package me.flamboyant.manhunt.domain.event;

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
            // Session already ended, handlers cleared - silently ignore
            // (framework-agnostic implementation - no logging dependency)
            return;
        }

        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (DomainEventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // Silently catch and continue to next handler
                    // (framework-agnostic implementation - no logging dependency)
                }
            }
        }
    }

    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }
}
