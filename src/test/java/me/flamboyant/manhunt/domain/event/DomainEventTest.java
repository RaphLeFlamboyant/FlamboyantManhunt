package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test(expected = IllegalArgumentException.class)
    public void testEventRejectsNullSessionId() {
        new TestEvent(null);
    }
}
