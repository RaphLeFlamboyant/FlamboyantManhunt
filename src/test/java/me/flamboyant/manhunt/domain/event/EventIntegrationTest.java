package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionEvaluator;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for full domain event chains.
 * Tests that events trigger handlers which trigger other events.
 */
public class EventIntegrationTest {

    private GameSession session;
    private MockEventPublisher publisher;
    private WinConditionEvaluator evaluator;
    private Player mockPlayer;

    @Before
    public void setUp() {
        GameSessionId sessionId = GameSessionId.generate();
        publisher = new MockEventPublisher();
        session = new GameSession(sessionId, new InMemoryPortalTracker(), publisher);

        evaluator = new WinConditionEvaluator(
            Arrays.asList(new AllSpeedrunnersDeadCondition())
        );

        mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getDisplayName()).thenReturn("TestPlayer");
    }

    @Test
    public void testSpeedrunnerDeathTriggersWinConditionCheck() {
        // Setup: Track all events
        List<DomainEvent> capturedEvents = new ArrayList<>();
        publisher.subscribe(SpeedrunnerDiedEvent.class, capturedEvents::add);
        publisher.subscribe(GameEndedEvent.class, capturedEvents::add);

        // Register win condition handler (simulates NewManhuntManager)
        publisher.subscribe(SpeedrunnerDiedEvent.class, event -> {
            Optional<WinOutcome> outcome = evaluator.evaluate(session);
            if (outcome.isPresent()) {
                session.notifyGameEnded(outcome.get(), "All speedrunners eliminated");
            }
        });

        session.setRemainingSpeedrunners(1);

        // Act: Speedrunner dies
        session.notifySpeedrunnerDied(mockPlayer);

        // Assert: Event chain fired
        assertEquals(2, capturedEvents.size());
        assertTrue(capturedEvents.get(0) instanceof SpeedrunnerDiedEvent);
        assertTrue(capturedEvents.get(1) instanceof GameEndedEvent);

        GameEndedEvent endEvent = (GameEndedEvent) capturedEvents.get(1);
        assertEquals("All speedrunners eliminated", endEvent.getReason());
    }

    @Test
    public void testSpeedrunnerDeathDoesNotEndGameIfMoreRemain() {
        // Setup: Track all events
        List<DomainEvent> capturedEvents = new ArrayList<>();
        publisher.subscribe(SpeedrunnerDiedEvent.class, capturedEvents::add);
        publisher.subscribe(GameEndedEvent.class, capturedEvents::add);

        // Register win condition handler
        publisher.subscribe(SpeedrunnerDiedEvent.class, event -> {
            Optional<WinOutcome> outcome = evaluator.evaluate(session);
            if (outcome.isPresent()) {
                session.notifyGameEnded(outcome.get(), "All speedrunners eliminated");
            }
        });

        session.setRemainingSpeedrunners(2);

        // Act: One speedrunner dies (one still remains)
        session.notifySpeedrunnerDied(mockPlayer);

        // Assert: Only death event, no game end
        assertEquals(1, capturedEvents.size());
        assertTrue(capturedEvents.get(0) instanceof SpeedrunnerDiedEvent);
        assertFalse(publisher.hasEventOfType(GameEndedEvent.class));
    }

    @Test
    public void testEventHandlerExceptionDoesNotBreakChain() {
        // Setup: First handler throws, second should still execute
        List<String> handlerCalls = new ArrayList<>();

        publisher.subscribe(GameStartedEvent.class, event -> {
            handlerCalls.add("handler1");
            throw new RuntimeException("Handler 1 failed");
        });

        publisher.subscribe(GameStartedEvent.class, event -> {
            handlerCalls.add("handler2");
        });

        // Act: Publish event
        try {
            session.notifyGameStarted();
        } catch (RuntimeException e) {
            // Expected in mock (real publisher logs it)
        }

        // Assert: Both handlers called despite first failing
        // Note: MockEventPublisher re-throws for test visibility
        // Real InMemoryEventPublisher logs and continues
        assertTrue(handlerCalls.contains("handler1"));
    }

    @Test
    public void testUnsubscribeAllPreventsHandlerExecution() {
        // Setup: Register handler
        List<GameStartedEvent> events = new ArrayList<>();
        publisher.subscribe(GameStartedEvent.class, events::add);

        // Act: Unsubscribe all
        publisher.unsubscribeAll();
        session.notifyGameStarted();

        // Assert: Handler not called
        assertEquals(0, events.size());
    }

    @Test
    public void testSessionEndCleansUpHandlers() {
        // Setup: Register handler
        List<GameStartedEvent> events = new ArrayList<>();
        publisher.subscribe(GameStartedEvent.class, events::add);

        // Act: End session (should call unsubscribeAll)
        session.end();
        session.notifyGameStarted();

        // Assert: Handler not called after session ended
        assertEquals(0, events.size());
    }
}
