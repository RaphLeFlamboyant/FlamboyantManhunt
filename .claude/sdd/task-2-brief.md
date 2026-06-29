# Task 2: In-Memory Event Publisher Implementation

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisher.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisherTest.java`

**Interfaces:**
- Consumes: `DomainEvent`, `DomainEventPublisher`, `DomainEventHandler` from Task 1
- Produces: `InMemoryEventPublisher` class implementing `DomainEventPublisher`

---

**Step 1: Write failing test for event publisher**

Create `src/test/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisherTest.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class InMemoryEventPublisherTest {
    
    private InMemoryEventPublisher publisher;
    private GameSessionId sessionId;
    
    // Test event class
    private static class TestEvent extends DomainEvent {
        private final String message;
        
        public TestEvent(GameSessionId sessionId, String message) {
            super(sessionId);
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    @Before
    public void setUp() {
        publisher = new InMemoryEventPublisher();
        sessionId = GameSessionId.generate();
    }
    
    @Test
    public void testPublishEventCallsSubscribedHandler() {
        List<TestEvent> receivedEvents = new ArrayList<>();
        
        publisher.subscribe(TestEvent.class, receivedEvents::add);
        
        TestEvent event = new TestEvent(sessionId, "test");
        publisher.publish(event);
        
        assertEquals(1, receivedEvents.size());
        assertEquals("test", receivedEvents.get(0).getMessage());
    }
    
    @Test
    public void testPublishEventCallsMultipleHandlers() {
        List<String> handlerCalls = new ArrayList<>();
        
        publisher.subscribe(TestEvent.class, e -> handlerCalls.add("handler1"));
        publisher.subscribe(TestEvent.class, e -> handlerCalls.add("handler2"));
        
        TestEvent event = new TestEvent(sessionId, "test");
        publisher.publish(event);
        
        assertEquals(2, handlerCalls.size());
        assertEquals("handler1", handlerCalls.get(0));
        assertEquals("handler2", handlerCalls.get(1));
    }
    
    @Test
    public void testPublishEventDoesNotCallUnsubscribedHandlers() {
        List<TestEvent> receivedEvents = new ArrayList<>();
        
        publisher.subscribe(TestEvent.class, receivedEvents::add);
        publisher.unsubscribeAll();
        
        TestEvent event = new TestEvent(sessionId, "test");
        publisher.publish(event);
        
        assertEquals(0, receivedEvents.size());
    }
    
    @Test
    public void testHandlerExceptionDoesNotStopOtherHandlers() {
        List<String> handlerCalls = new ArrayList<>();
        
        publisher.subscribe(TestEvent.class, e -> {
            handlerCalls.add("handler1");
            throw new RuntimeException("Handler1 failed");
        });
        publisher.subscribe(TestEvent.class, e -> handlerCalls.add("handler2"));
        
        TestEvent event = new TestEvent(sessionId, "test");
        publisher.publish(event);
        
        assertEquals(2, handlerCalls.size());
        assertEquals("handler1", handlerCalls.get(0));
        assertEquals("handler2", handlerCalls.get(1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSubscribeRejectsNullEventType() {
        publisher.subscribe(null, e -> {});
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSubscribeRejectsNullHandler() {
        publisher.subscribe(TestEvent.class, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testPublishRejectsNullEvent() {
        publisher.publish(null);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=InMemoryEventPublisherTest`

Expected output:
```
[ERROR] Compilation failure: InMemoryEventPublisher cannot be resolved to a type
```

**Step 3: Implement InMemoryEventPublisher**

Create `src/main/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisher.java`:

```java
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
```

**Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=InMemoryEventPublisherTest`

Expected output:
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

**Step 5: Commit event publisher implementation**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisher.java
git add src/test/java/me/flamboyant/manhunt/domain/event/InMemoryEventPublisherTest.java
git commit -m "feat(domain): implement in-memory event publisher

- Add InMemoryEventPublisher with synchronous event delivery
- Handle exceptions in handlers without stopping other handlers
- Add comprehensive tests for subscribe/publish/unsubscribe
- Add null validation and error logging"
```
