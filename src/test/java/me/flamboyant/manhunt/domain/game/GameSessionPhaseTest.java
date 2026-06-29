package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.event.PhaseChangedEvent;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameSessionPhaseTest {

    private GameSession session;
    private InMemoryEventPublisher publisher;
    private GameSessionId sessionId;

    @BeforeEach
    public void setUp() {
        sessionId = GameSessionId.generate();
        publisher = new InMemoryEventPublisher();
        session = new GameSession(sessionId, new InMemoryPortalTracker(), publisher);
    }

    @Test
    public void testSessionStartsInPreparationPhase() {
        assertEquals(GamePhase.PREPARATION, session.getCurrentPhase());
    }

    @Test
    public void testCanTransitionToChecksValidTransitions() {
        // From PREPARATION, can transition to ACTIVE
        assertTrue(session.canTransitionTo(GamePhase.ACTIVE));

        // From PREPARATION, cannot transition to PREPARATION (same state)
        assertFalse(session.canTransitionTo(GamePhase.PREPARATION));

        // Transition to ACTIVE
        session.transitionTo(GamePhase.ACTIVE);

        // From ACTIVE, cannot transition anywhere
        assertFalse(session.canTransitionTo(GamePhase.PREPARATION));
        assertFalse(session.canTransitionTo(GamePhase.ACTIVE));
    }

    @Test
    public void testValidTransitionFromPreparationToActive() {
        session.transitionTo(GamePhase.ACTIVE);

        assertEquals(GamePhase.ACTIVE, session.getCurrentPhase());
    }

    @Test
    public void testInvalidTransitionFromActiveToPreparation() {
        session.transitionTo(GamePhase.ACTIVE);

        assertThrows(IllegalStateException.class, () -> {
            session.transitionTo(GamePhase.PREPARATION);
        });
    }

    @Test
    public void testCannotTransitionToSamePhase() {
        assertThrows(IllegalStateException.class, () -> {
            session.transitionTo(GamePhase.PREPARATION);
        });
    }

    @Test
    public void testTransitionToNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            session.transitionTo(null);
        });
    }

    @Test
    public void testPhaseChangedEventPublished() {
        List<PhaseChangedEvent> events = new ArrayList<>();
        publisher.subscribe(PhaseChangedEvent.class, events::add);

        session.transitionTo(GamePhase.ACTIVE);

        assertEquals(1, events.size());
        PhaseChangedEvent event = events.get(0);
        assertEquals(GamePhase.PREPARATION, event.getOldPhase());
        assertEquals(GamePhase.ACTIVE, event.getNewPhase());
        assertEquals(sessionId, event.getSessionId());
    }
}
