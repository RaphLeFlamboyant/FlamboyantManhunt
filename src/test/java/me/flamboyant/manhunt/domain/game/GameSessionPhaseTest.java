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
}
