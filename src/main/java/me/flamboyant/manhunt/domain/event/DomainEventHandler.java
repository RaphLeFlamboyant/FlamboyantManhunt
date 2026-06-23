package me.flamboyant.manhunt.domain.event;

/**
 * Handles a specific type of domain event.
 * Implementations should be idempotent where possible and handle exceptions gracefully.
 *
 * @param <T> The type of event this handler processes
 */
@FunctionalInterface
public interface DomainEventHandler<T extends DomainEvent> {
    /**
     * Handle a domain event.
     *
     * @param event The event to handle
     */
    void handle(T event);
}
