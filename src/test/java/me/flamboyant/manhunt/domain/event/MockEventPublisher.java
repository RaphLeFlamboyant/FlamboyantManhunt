package me.flamboyant.manhunt.domain.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mock event publisher for testing.
 * Captures published events and allows verification in tests.
 */
public class MockEventPublisher implements DomainEventPublisher {
    private final List<DomainEvent> publishedEvents = new ArrayList<>();
    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        publishedEvents.add(event);

        // Also call handlers for integration tests
        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (DomainEventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // In tests, let exceptions propagate
                    throw new RuntimeException("Handler failed in test", e);
                }
            }
        }
    }

    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }

    // Test helper methods

    public List<DomainEvent> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }

    public <T extends DomainEvent> List<T> getEventsOfType(Class<T> eventType) {
        return publishedEvents.stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .collect(Collectors.toList());
    }

    public boolean hasEventOfType(Class<? extends DomainEvent> eventType) {
        return publishedEvents.stream().anyMatch(eventType::isInstance);
    }

    public int getEventCount() {
        return publishedEvents.size();
    }

    public void clearPublishedEvents() {
        publishedEvents.clear();
    }
}
