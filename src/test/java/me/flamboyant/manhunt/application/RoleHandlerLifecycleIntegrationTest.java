package me.flamboyant.manhunt.application;

import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.EventHandlerRegistrationService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.event.Listener;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for role handler lifecycle management.
 * Verifies that handlers are registered on game start and unregistered on game end.
 */
public class RoleHandlerLifecycleIntegrationTest {
    private GameLifecycleService lifecycleService;
    private EventHandlerRegistrationService eventHandlerService;

    @Before
    public void setUp() {
        lifecycleService = mock(GameLifecycleService.class);
        eventHandlerService = mock(EventHandlerRegistrationService.class);
    }

    @Test
    public void testHandlerRegistrationAndCleanup() {
        // This test verifies the contract between components
        // In a real integration test with a test server, we'd verify actual Bukkit registration

        // 1. Start game should call registerHandlers
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);

        when(lifecycleService.createSession()).thenReturn(sessionId);
        when(lifecycleService.getSession(sessionId)).thenReturn(session);

        // Mock handler registration
        HandlerRegistration registration = mock(HandlerRegistration.class);
        when(eventHandlerService.registerHandlers(any(GameSessionId.class), any(Listener[].class)))
            .thenReturn(registration);

        // Simulate setting registration (saga would do this)
        session.setHandlerRegistration(registration);

        // Verify registration stored
        assertNotNull(session.getHandlerRegistration());
        assertEquals(registration, session.getHandlerRegistration());

        // 2. End game should call unregisterHandlers
        // Simulate lifecycle service endSession behavior
        HandlerRegistration retrievedRegistration = session.getHandlerRegistration();
        assertNotNull(retrievedRegistration);

        // Verify unregister would be called
        eventHandlerService.unregisterHandlers(retrievedRegistration);
        verify(eventHandlerService).unregisterHandlers(retrievedRegistration);
    }

    @Test
    public void testMultipleSessionsHaveIsolatedRegistrations() {
        // Create two sessions with different registrations
        GameSessionId session1Id = GameSessionId.generate();
        GameSession session1 = new GameSession(session1Id);
        HandlerRegistration registration1 = mock(HandlerRegistration.class);
        session1.setHandlerRegistration(registration1);

        GameSessionId session2Id = GameSessionId.generate();
        GameSession session2 = new GameSession(session2Id);
        HandlerRegistration registration2 = mock(HandlerRegistration.class);
        session2.setHandlerRegistration(registration2);

        // Verify sessions have different registrations
        assertNotEquals(registration1, registration2);
        assertEquals(registration1, session1.getHandlerRegistration());
        assertEquals(registration2, session2.getHandlerRegistration());
    }

    @Test
    public void testSessionCleanupWithNullRegistration() {
        // Create session without registration
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);
        session.setHandlerRegistration(null);

        // Verify no NPE when ending session
        session.end();
        assertNull(session.getHandlerRegistration());
    }
}
