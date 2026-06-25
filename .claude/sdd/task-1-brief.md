# Task 1: Core Event Infrastructure - Base Classes

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/DomainEvent.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/DomainEventPublisher.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/DomainEventHandler.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/event/DomainEventTest.java`

**Interfaces:**
- Consumes: `GameSessionId` from `me.flamboyant.manhunt.domain.game.GameSessionId`
- Produces:
  - `DomainEvent` - abstract base class with `getEventId()`, `getOccurredAtMillis()`, `getSessionId()`
  - `DomainEventPublisher` - interface with `subscribe()`, `publish()`, `unsubscribeAll()`
  - `DomainEventHandler<T>` - functional interface with `handle(T event)`

---

**Step 1: Write failing test for DomainEvent base class**

Create `src/test/java/me/flamboyant/manhunt/domain/event/DomainEventTest.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DomainEventTest {
    
    // Concrete test event for testing abstract base
    private static class TestEvent extends DomainEvent {
        public TestEvent(GameSessionId sessionId) {
            super(sessionId);
        }
    }
    
    @Test
    public void testEventHasUniqueId() {
        GameSessionId sessionId = GameSessionId.generate();
        TestEvent event1 = new TestEvent(sessionId);
        TestEvent event2 = new TestEvent(sessionId);
        
        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }
    
    @Test
    public void testEventHasTimestamp() {
        GameSessionId sessionId = GameSessionId.generate();
        long before = System.currentTimeMillis();
        TestEvent event = new TestEvent(sessionId);
        long after = System.currentTimeMillis();
        
        assertTrue(event.getOccurredAtMillis() >= before);
        assertTrue(event.getOccurredAtMillis() <= after);
    }
    
    @Test
    public void testEventStoresSessionId() {
        GameSessionId sessionId = GameSessionId.generate();
        TestEvent event = new TestEvent(sessionId);
        
        assertEquals(sessionId, event.getSessionId());
    }
    
    @Test
    public void testEventRejectsNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TestEvent(null);
        });
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DomainEventTest`

Expected output:
```
[ERROR] Compilation failure: DomainEvent cannot be resolved to a type
```

**Step 3: Create DomainEvent abstract class**

Create `src/main/java/me/flamboyant/manhunt/domain/event/DomainEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

import java.util.UUID;

/**
 * Base class for all domain events in the Manhunt game.
 * Domain events represent significant business moments that have occurred.
 */
public abstract class DomainEvent {
    private final String eventId;
    private final long occurredAtMillis;
    private final GameSessionId sessionId;
    
    /**
     * Creates a new domain event.
     * 
     * @param sessionId The ID of the game session that produced this event
     * @throws IllegalArgumentException if sessionId is null
     */
    protected DomainEvent(GameSessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        this.eventId = UUID.randomUUID().toString();
        this.occurredAtMillis = System.currentTimeMillis();
        this.sessionId = sessionId;
    }
    
    /**
     * @return Unique identifier for this event instance
     */
    public String getEventId() {
        return eventId;
    }
    
    /**
     * @return Timestamp (milliseconds since epoch) when this event occurred
     */
    public long getOccurredAtMillis() {
        return occurredAtMillis;
    }
    
    /**
     * @return The game session that produced this event
     */
    public GameSessionId getSessionId() {
        return sessionId;
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=DomainEventTest`

Expected output:
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

**Step 5: Create DomainEventPublisher interface**

Create `src/main/java/me/flamboyant/manhunt/domain/event/DomainEventPublisher.java`:

```java
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
```

**Step 6: Create DomainEventHandler functional interface**

Create `src/main/java/me/flamboyant/manhunt/domain/event/DomainEventHandler.java`:

```java
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
```

**Step 7: Commit core infrastructure**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/event/DomainEvent.java
git add src/main/java/me/flamboyant/manhunt/domain/event/DomainEventPublisher.java
git add src/main/java/me/flamboyant/manhunt/domain/event/DomainEventHandler.java
git add src/test/java/me/flamboyant/manhunt/domain/event/DomainEventTest.java
git commit -m "feat(domain): add domain event infrastructure base classes

- Add DomainEvent abstract class with eventId, timestamp, sessionId
- Add DomainEventPublisher interface for event bus
- Add DomainEventHandler functional interface
- Add comprehensive tests for base event class"
```
