package me.flamboyant.manhunt.application.services;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.GameStartedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

public class GameLifecycleServiceTest {

    private GameSessionManager mockSessionManager;
    private DomainEventPublisher mockPublisher;
    private EventHandlerRegistrationService mockEventHandlerService;
    private GameLifecycleService service;

    @Before
    public void setUp() {
        mockSessionManager = mock(GameSessionManager.class);
        mockPublisher = mock(DomainEventPublisher.class);
        mockEventHandlerService = mock(EventHandlerRegistrationService.class);
        service = new GameLifecycleService(mockSessionManager, mockPublisher, mockEventHandlerService);
    }

    @Test
    public void createSession_shouldCreateAndPublishEvent() {
        // Arrange
        GameSession mockSession = mock(GameSession.class);
        GameSessionId sessionId = GameSessionId.generate();
        when(mockSessionManager.createSession()).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn(sessionId);

        Player mockPlayer = mock(Player.class);
        List<Player> players = Arrays.asList(mockPlayer);

        // Act
        GameSessionId result = service.createSession(players);

        // Assert
        assertNotNull(result);
        assertEquals(sessionId, result);
        verify(mockSessionManager).createSession();

        ArgumentCaptor<GameSessionCreatedEvent> eventCaptor =
            ArgumentCaptor.forClass(GameSessionCreatedEvent.class);
        verify(mockPublisher).publish(eventCaptor.capture());

        GameSessionCreatedEvent event = eventCaptor.getValue();
        assertEquals(sessionId, event.getSessionId());
        assertEquals(1, event.getPlayers().size());
    }

    @Test
    public void startSession_shouldStartAndPublishEvent() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Player mockPlayer = mock(Player.class);
        java.util.Set<Player> players = new java.util.HashSet<>();
        players.add(mockPlayer);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getPlayers()).thenReturn(players);

        // Act
        service.startSession(sessionId, 10, false);

        // Assert
        verify(mockSessionManager).getSession(sessionId);

        ArgumentCaptor<GameStartedEvent> eventCaptor =
            ArgumentCaptor.forClass(GameStartedEvent.class);
        verify(mockPublisher).publish(eventCaptor.capture());

        GameStartedEvent event = eventCaptor.getValue();
        assertEquals(sessionId, event.getSessionId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void startSession_shouldThrowIfSessionNotFound() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        when(mockSessionManager.getSession(sessionId)).thenReturn(null);

        // Act
        service.startSession(sessionId, 10, false);
    }

    @Test
    public void endSession_shouldEndAndRemoveSession() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        HandlerRegistration registration = mock(HandlerRegistration.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getHandlerRegistration()).thenReturn(registration);

        EndGameCommand command = new EndGameCommand(sessionId, "Test end");

        // Act
        service.endSession(command);

        // Assert
        verify(mockEventHandlerService).unregisterHandlers(registration);
        verify(mockSession).notifyGameEnded(null, "Test end");
        verify(mockSessionManager).removeSession(sessionId);
    }

    @Test
    public void endSession_shouldHandleNonExistentSession() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        when(mockSessionManager.getSession(sessionId)).thenReturn(null);

        EndGameCommand command = new EndGameCommand(sessionId, "Test end");

        // Act
        service.endSession(command);

        // Assert - should not throw, just no-op
        verify(mockSessionManager, never()).removeSession(any());
    }

    @Test
    public void testEndSessionUnregistersHandlers() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        HandlerRegistration registration = mock(HandlerRegistration.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getHandlerRegistration()).thenReturn(registration);

        // Act
        service.endSession(sessionId);

        // Assert
        verify(mockEventHandlerService).unregisterHandlers(registration);
        verify(mockSession).notifyGameEnded(null, "Game ended");
        verify(mockSessionManager).removeSession(sessionId);
    }

    @Test
    public void testEndSessionWithNullRegistrationDoesNotThrow() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getHandlerRegistration()).thenReturn(null);

        // Act
        service.endSession(sessionId);

        // Assert - should not throw NPE
        verify(mockEventHandlerService, never()).unregisterHandlers(any());
        verify(mockSession).notifyGameEnded(null, "Game ended");
        verify(mockSessionManager).removeSession(sessionId);
    }

    @Test
    public void testEndSessionHandlesUnregisterException() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        HandlerRegistration registration = mock(HandlerRegistration.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getHandlerRegistration()).thenReturn(registration);
        doThrow(new RuntimeException("Unregister failed")).when(mockEventHandlerService).unregisterHandlers(registration);

        // Act
        service.endSession(sessionId);

        // Assert - should log error but continue cleanup
        verify(mockEventHandlerService).unregisterHandlers(registration);
        verify(mockSession).notifyGameEnded(null, "Game ended");
        verify(mockSessionManager).removeSession(sessionId);
    }
}
