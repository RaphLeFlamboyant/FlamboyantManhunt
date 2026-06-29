package me.flamboyant.manhunt.application.sagas;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.lifecycle.HandlerRegistration;
import me.flamboyant.manhunt.application.commands.AssignRolesCommand;
import me.flamboyant.manhunt.application.commands.DistributeRolesCommand;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.domain.lifecycle.CompensationStatus;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.services.EventHandlerRegistrationService;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.application.services.RoleAssignmentService;
import me.flamboyant.manhunt.application.services.RoleDistributionService;
import me.flamboyant.manhunt.domain.event.DomainEventHandler;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.GameStartFailedEvent;
import me.flamboyant.manhunt.domain.event.HandlersRegisteredEvent;
import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesAssignedEvent;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for StartGameSaga.
 * Uses real event publisher with mocked services to test saga coordination.
 */
public class StartGameSagaTest {

    private GameLifecycleService mockLifecycle;
    private RoleDistributionService mockDistribution;
    private RoleAssignmentService mockAssignment;
    private EventHandlerRegistrationService mockHandlers;
    private MessageService mockMessageService;
    private GameSessionManager mockSessionManager;
    private TestEventPublisher testPublisher;
    private StartGameSaga saga;

    private Player mockPlayer1;
    private Player mockPlayer2;

    @Before
    public void setUp() {
        mockLifecycle = mock(GameLifecycleService.class);
        mockDistribution = mock(RoleDistributionService.class);
        mockAssignment = mock(RoleAssignmentService.class);
        mockHandlers = mock(EventHandlerRegistrationService.class);
        mockMessageService = mock(MessageService.class);
        mockSessionManager = mock(GameSessionManager.class);

        // Use test event publisher for integration tests
        testPublisher = new TestEventPublisher();

        saga = new StartGameSaga(mockLifecycle, mockDistribution, mockAssignment,
                                 mockHandlers, mockMessageService, mockSessionManager, testPublisher);

        // Register saga event handlers
        testPublisher.subscribe(GameSessionCreatedEvent.class, saga::onSessionCreated);
        testPublisher.subscribe(RolesDistributedEvent.class, saga::onRolesDistributed);
        testPublisher.subscribe(RolesAssignedEvent.class, saga::onRolesAssigned);
        testPublisher.subscribe(HandlersRegisteredEvent.class, saga::onHandlersRegistered);

        // Create mock players
        mockPlayer1 = mock(Player.class);
        mockPlayer2 = mock(Player.class);
        when(mockPlayer1.getDisplayName()).thenReturn("Player1");
        when(mockPlayer2.getDisplayName()).thenReturn("Player2");
    }

    @Test
    public void start_shouldCreateSessionAndReturnId() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        when(mockLifecycle.createSession(any())).thenReturn(sessionId);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        GameSessionId result = saga.start(command);

        // Assert
        assertEquals(sessionId, result);
        verify(mockLifecycle).createSession(command.getPlayers());
        assertTrue(saga.hasActiveWorkflow(sessionId));
    }

    @Test
    public void onSessionCreated_shouldDistributeRoles() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        assignments.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        saga.start(command);

        // Simulate event published by GameLifecycleService
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));

        // Assert
        verify(mockDistribution).distributeRoles(any(DistributeRolesCommand.class));
    }

    @Test
    public void onRolesDistributed_shouldAssignRoles() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        assignments.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));

        // Simulate event published by RoleDistributionService
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));

        // Assert
        verify(mockAssignment).assignRoles(any(AssignRolesCommand.class));
    }

    @Test
    public void onRolesAssigned_shouldRegisterHandlers() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        assignments.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        HandlerRegistration registration = new HandlerRegistration(sessionId, Collections.emptyList());

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(eq(sessionId), any())).thenReturn(registration);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));

        // Simulate event published by RoleAssignmentService
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Assert
        verify(mockHandlers).registerHandlers(eq(sessionId), any());
    }

    @Test
    public void onHandlersRegistered_shouldStartSession() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        assignments.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        HandlerRegistration registration = new HandlerRegistration(sessionId, Collections.emptyList());

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(eq(sessionId), any())).thenReturn(registration);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .surpriseSpeedrunner(false)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Simulate event published by EventHandlerRegistrationService
        testPublisher.publish(new HandlersRegisteredEvent(sessionId));

        // Assert
        verify(mockLifecycle).startSession(sessionId, 10, false);
        // Workflow should be removed after completion
        assertFalse(saga.hasActiveWorkflow(sessionId));
    }

    @Test
    public void fullWorkflow_shouldExecuteInOrder() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        assignments.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        HandlerRegistration registration = new HandlerRegistration(sessionId, Collections.emptyList());

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(eq(sessionId), any())).thenReturn(registration);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .surpriseSpeedrunner(false)
            .build();

        // Act - simulate full event flow
        GameSessionId result = saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));
        testPublisher.publish(new RolesAssignedEvent(sessionId));
        testPublisher.publish(new HandlersRegisteredEvent(sessionId));

        // Assert - verify execution order
        InOrder inOrder = inOrder(mockLifecycle, mockDistribution, mockAssignment, mockHandlers);
        inOrder.verify(mockLifecycle).createSession(command.getPlayers());
        inOrder.verify(mockDistribution).distributeRoles(any(DistributeRolesCommand.class));
        inOrder.verify(mockAssignment).assignRoles(any(AssignRolesCommand.class));
        inOrder.verify(mockHandlers).registerHandlers(eq(sessionId), any());
        inOrder.verify(mockLifecycle).startSession(eq(sessionId), eq(10), eq(false));

        assertEquals(sessionId, result);
        assertEquals(0, saga.getActiveWorkflowCount());
    }

    @Test(expected = GameStartException.class)
    public void start_shouldThrowOnCreateSessionFailure() throws GameStartException {
        // Arrange
        when(mockLifecycle.createSession(any())).thenThrow(new RuntimeException("Creation failed"));

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
    }

    @Test
    public void compensation_shouldRollbackOnDistributionFailure() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockDistribution.distributeRoles(any()))
            .thenThrow(new RuntimeException("Distribution failed"));

        // Subscribe to failure event
        AtomicReference<GameStartFailedEvent> failureEvent = new AtomicReference<>();
        testPublisher.subscribe(GameStartFailedEvent.class, failureEvent::set);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));

        // Assert - compensation executed
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);
        assertFalse(saga.hasActiveWorkflow(sessionId));

        // Verify failure event published
        assertNotNull(failureEvent.get());
        assertEquals(sessionId, failureEvent.get().getSessionId());
    }

    @Test
    public void compensation_shouldRollbackOnAssignmentFailure() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        doThrow(new RuntimeException("Assignment failed"))
            .when(mockAssignment).assignRoles(any());

        // Subscribe to failure event
        AtomicReference<GameStartFailedEvent> failureEvent = new AtomicReference<>();
        testPublisher.subscribe(GameStartFailedEvent.class, failureEvent::set);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));

        // Assert
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);

        assertNotNull(failureEvent.get());
        assertEquals(sessionId, failureEvent.get().getSessionId());
    }

    @Test
    public void compensation_shouldRollbackOnHandlerRegistrationFailure() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(any(), any()))
            .thenThrow(new RuntimeException("Handler registration failed"));

        // Subscribe to failure event
        AtomicReference<GameStartFailedEvent> failureEvent = new AtomicReference<>();
        testPublisher.subscribe(GameStartFailedEvent.class, failureEvent::set);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Assert
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);

        assertNotNull(failureEvent.get());
        assertNotNull(failureEvent.get().getCause());
    }

    @Test
    public void compensation_shouldRollbackHandlersOnStartSessionFailure() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        Listener mockListener = mock(Listener.class);
        HandlerRegistration registration = new HandlerRegistration(sessionId, Arrays.asList(mockListener));

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(eq(sessionId), any())).thenReturn(registration);
        doThrow(new RuntimeException("Start session failed"))
            .when(mockLifecycle).startSession(any(), anyInt(), anyBoolean());

        // Subscribe to failure event
        AtomicReference<GameStartFailedEvent> failureEvent = new AtomicReference<>();
        testPublisher.subscribe(GameStartFailedEvent.class, failureEvent::set);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));
        testPublisher.publish(new RolesAssignedEvent(sessionId));
        testPublisher.publish(new HandlersRegisteredEvent(sessionId));

        // Assert - handlers should be unregistered during compensation
        verify(mockHandlers).unregisterHandlers(registration);
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);

        assertNotNull(failureEvent.get());
        CompensationStatus status = failureEvent.get().getCompensationStatus();
        assertTrue(status.isHandlersUnregistered());
        assertTrue(status.isRolesCleared());
        assertTrue(status.isSessionDeleted());
        assertTrue(status.isFullyCompensated());
    }

    @Test
    public void onSessionCreated_shouldIgnoreUnrelatedEvents() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSessionId unrelatedId = GameSessionId.generate();

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        // Send event for unrelated session
        testPublisher.publish(new GameSessionCreatedEvent(unrelatedId, command.getPlayers()));

        // Assert - distribution should not be called for unrelated event
        verify(mockDistribution, never()).distributeRoles(any());
    }

    @Test
    public void listenerProvider_shouldProvideListenersForRegistration() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        Listener mockListener = mock(Listener.class);
        HandlerRegistration registration = new HandlerRegistration(sessionId, Arrays.asList(mockListener));

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockHandlers.registerHandlers(eq(sessionId), eq(mockListener))).thenReturn(registration);

        // Set listener provider
        saga.setListenerProvider(id -> new Listener[] { mockListener });

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, assignments));
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Assert
        verify(mockHandlers).registerHandlers(sessionId, mockListener);
    }

    @Test
    public void getActiveWorkflowCount_shouldTrackWorkflows() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        when(mockLifecycle.createSession(any())).thenReturn(sessionId);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1))
            .speedrunnerCount(1)
            .build();

        // Act & Assert
        assertEquals(0, saga.getActiveWorkflowCount());

        saga.start(command);

        assertEquals(1, saga.getActiveWorkflowCount());
        assertTrue(saga.hasActiveWorkflow(sessionId));
    }

    @Test
    public void testStartGameRegistersRoleHandlers() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Map<Player, ManhuntRoleIdentifier> roleMap = new HashMap<>();
        roleMap.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        roleMap.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        // Create mock roles that implement Listener
        AManhuntRole mockRole1 = mock(AManhuntRole.class, withSettings().extraInterfaces(Listener.class));
        AManhuntRole mockRole2 = mock(AManhuntRole.class, withSettings().extraInterfaces(Listener.class));

        Map<Player, AManhuntRole> roleObjects = new HashMap<>();
        roleObjects.put(mockPlayer1, mockRole1);
        roleObjects.put(mockPlayer2, mockRole2);

        HandlerRegistration mockRegistration = mock(HandlerRegistration.class);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getAllRoles()).thenReturn(roleObjects);
        when(mockDistribution.distributeRoles(any())).thenReturn(roleMap);
        when(mockHandlers.registerHandlers(any(GameSessionId.class), any(Listener[].class)))
            .thenReturn(mockRegistration);

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, roleMap));
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Assert - verify handlers were registered
        verify(mockHandlers).registerHandlers(eq(sessionId), any(Listener[].class));

        // Verify registration stored in session
        verify(mockSession).setHandlerRegistration(mockRegistration);
    }

    @Test
    public void testStartGameRegistersOnlyListenerRoles() throws GameStartException {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Map<Player, ManhuntRoleIdentifier> roleMap = new HashMap<>();
        roleMap.put(mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        roleMap.put(mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        // Create mock roles that implement Listener
        AManhuntRole mockRole1 = mock(AManhuntRole.class, withSettings().extraInterfaces(Listener.class));
        AManhuntRole mockRole2 = mock(AManhuntRole.class, withSettings().extraInterfaces(Listener.class));

        Map<Player, AManhuntRole> roleObjects = new HashMap<>();
        roleObjects.put(mockPlayer1, mockRole1);
        roleObjects.put(mockPlayer2, mockRole2);

        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockSession.getAllRoles()).thenReturn(roleObjects);
        when(mockDistribution.distributeRoles(any())).thenReturn(roleMap);

        // Capture registered listeners
        ArgumentCaptor<Listener[]> listenersCaptor = ArgumentCaptor.forClass(Listener[].class);
        when(mockHandlers.registerHandlers(any(GameSessionId.class), listenersCaptor.capture()))
            .thenReturn(mock(HandlerRegistration.class));

        StartGameCommand command = StartGameCommand.builder()
            .players(Arrays.asList(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();

        // Act
        saga.start(command);
        testPublisher.publish(new GameSessionCreatedEvent(sessionId, command.getPlayers()));
        testPublisher.publish(new RolesDistributedEvent(sessionId, roleMap));
        testPublisher.publish(new RolesAssignedEvent(sessionId));

        // Assert - verify only Listener instances were registered
        Listener[] registeredListeners = listenersCaptor.getValue();
        assertNotNull(registeredListeners);
        assertEquals(2, registeredListeners.length); // Both roles implement Listener
    }

    /**
     * Test-friendly event publisher that doesn't require Bukkit.
     */
    private static class TestEventPublisher implements DomainEventPublisher {
        private final Map<Class<?>, java.util.List<DomainEventHandler<?>>> handlers = new HashMap<>();

        @Override
        public <T extends me.flamboyant.manhunt.domain.event.DomainEvent> void subscribe(
                Class<T> eventType, DomainEventHandler<T> handler) {
            handlers.computeIfAbsent(eventType, k -> new java.util.ArrayList<>()).add(handler);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void publish(me.flamboyant.manhunt.domain.event.DomainEvent event) {
            java.util.List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
            if (eventHandlers != null) {
                for (DomainEventHandler handler : eventHandlers) {
                    handler.handle(event);
                }
            }
        }

        @Override
        public void unsubscribeAll() {
            handlers.clear();
        }
    }
}
