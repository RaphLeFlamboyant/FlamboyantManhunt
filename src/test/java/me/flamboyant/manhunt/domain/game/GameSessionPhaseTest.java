package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        // Transition to ACTIVE manually (we'll implement transitionTo later)
        // For now, use reflection to set the phase
        try {
            java.lang.reflect.Field field = GameSession.class.getDeclaredField("currentPhase");
            field.setAccessible(true);
            field.set(session, GamePhase.ACTIVE);
        } catch (Exception e) {
            fail("Failed to set phase via reflection: " + e.getMessage());
        }

        // From ACTIVE, cannot transition anywhere
        assertFalse(session.canTransitionTo(GamePhase.PREPARATION));
        assertFalse(session.canTransitionTo(GamePhase.ACTIVE));
    }
}
