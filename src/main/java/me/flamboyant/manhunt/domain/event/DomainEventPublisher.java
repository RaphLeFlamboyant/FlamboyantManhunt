package me.flamboyant.manhunt.domain.event;

/**
 * Publishes domain events to registered handlers.
 * Events are delivered synchronously in the order they are published.
 */
public interface DomainEventPublisher {
    /**
     * Subscribe a handler to a specific event type.
     * The handler will be called synchronously whenever events of this type are published.
     *
     * @param eventType The class of event to subscribe to
     * @param handler The handler to invoke when events occur
     * @param <T> The event type
     */
    <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler);

    /**
     * Publish an event to all registered handlers.
     * Handlers are called synchronously in registration order.
     * If a handler throws an exception, it is logged and the next handler is called.
     *
     * @param event The event to publish
     */
    void publish(DomainEvent event);

    /**
     * Unsubscribe all handlers.
     * Called when the game session ends to prevent memory leaks.
     */
    void unsubscribeAll();
}
