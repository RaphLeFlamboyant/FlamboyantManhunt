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
