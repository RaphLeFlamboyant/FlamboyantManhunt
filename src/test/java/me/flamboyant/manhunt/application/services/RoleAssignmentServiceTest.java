package me.flamboyant.manhunt.application.services;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.commands.AssignRolesCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesAssignedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.behavior.HunterRole;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RoleAssignmentServiceTest {

    private GameSessionManager mockSessionManager;
    private RoleRegistry roleRegistry;
    private DomainEventPublisher mockPublisher;
    private RoleAssignmentService service;

    @Before
    public void setUp() {
        mockSessionManager = mock(GameSessionManager.class);
        roleRegistry = new RoleRegistry();
        roleRegistry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
        roleRegistry.register(HUNTER_SIMPLE, HunterRole::new);
        mockPublisher = mock(DomainEventPublisher.class);
        service = new RoleAssignmentService(mockSessionManager, roleRegistry, mockPublisher);
    }

    @Test
    public void assignRoles_shouldCreateAndAssignRoles() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Player player = mock(Player.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);

        Map<Player, ManhuntRoleIdentifier> assignments = Map.of(
            player, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
        );
        AssignRolesCommand command = new AssignRolesCommand(sessionId, assignments);

        // Act
        service.assignRoles(command);

        // Assert
        verify(mockSession).assignRole(eq(player), any(AManhuntRole.class));
    }

    @Test
    public void assignRoles_shouldPublishEvent() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        Player player = mock(Player.class);

        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);

        Map<Player, ManhuntRoleIdentifier> assignments = Map.of(
            player, ManhuntRoleIdentifier.HUNTER_SIMPLE
        );
        AssignRolesCommand command = new AssignRolesCommand(sessionId, assignments);

        // Act
        service.assignRoles(command);

        // Assert
        ArgumentCaptor<RolesAssignedEvent> eventCaptor =
            ArgumentCaptor.forClass(RolesAssignedEvent.class);
        verify(mockPublisher).publish(eventCaptor.capture());

        RolesAssignedEvent event = eventCaptor.getValue();
        assertEquals(sessionId, event.getSessionId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void assignRoles_shouldThrowIfSessionNotFound() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        when(mockSessionManager.getSession(sessionId)).thenReturn(null);

        Player player = mock(Player.class);
        AssignRolesCommand command = new AssignRolesCommand(
            sessionId,
            Map.of(player, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE)
        );

        // Act
        service.assignRoles(command);
    }
}
