package me.flamboyant.manhunt.application.services;

import me.flamboyant.manhunt.application.commands.DistributeRolesCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RoleDistributionServiceTest {

    private DomainEventPublisher mockPublisher;
    private RoleDistributionService service;
    private GameSessionId sessionId;

    @Before
    public void setUp() {
        mockPublisher = mock(DomainEventPublisher.class);
        RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
        ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
        RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
        Random rng = new Random(12345L);

        service = new RoleDistributionService(
            mockPublisher,
            countStrategy,
            conflictStrategy,
            assignmentStrategy,
            rng
        );
        sessionId = GameSessionId.generate();
    }

    @Test
    public void distributeRoles_shouldReturnAssignments() {
        // Arrange
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        List<Player> players = List.of(p1, p2);
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId, players, 1, 0, false, Map.of()
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(p1));
        assertTrue(result.containsKey(p2));
    }

    @Test
    public void distributeRoles_shouldPublishEvent() {
        // Arrange
        Player p1 = mock(Player.class);
        List<Player> players = List.of(p1);
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId, players, 1, 0, false, Map.of()
        );

        // Act
        service.distributeRoles(command);

        // Assert
        ArgumentCaptor<RolesDistributedEvent> eventCaptor =
            ArgumentCaptor.forClass(RolesDistributedEvent.class);
        verify(mockPublisher).publish(eventCaptor.capture());

        RolesDistributedEvent event = eventCaptor.getValue();
        assertNotNull(event.getAssignments());
        assertEquals(1, event.getAssignments().size());
    }

    @Test
    public void distributeRoles_shouldRespectFixedAssignments() {
        // Arrange
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        List<Player> players = List.of(p1, p2);
        Map<Player, ManhuntRoleIdentifier> fixed = Map.of(
            p1, ManhuntRoleIdentifier.HUNTER_SIMPLE
        );
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId, players, 1, 0, false, fixed
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        assertEquals(ManhuntRoleIdentifier.HUNTER_SIMPLE, result.get(p1));
    }

    @Test
    public void distributeRoles_shouldCreateCorrectSpeedrunnerCount() {
        // Arrange
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        List<Player> players = List.of(p1, p2, p3);
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId, players, 2, 0, false, Map.of()
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        long speedrunnerCount = result.values().stream()
            .filter(role -> role.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .count();
        assertEquals(2, speedrunnerCount);
    }

    @Test
    public void testDistributeRoles_noFixedAssignments_fullFlow() {
        // Arrange
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
        ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
        RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
        Random rng = new Random(12345L);

        RoleDistributionService service = new RoleDistributionService(
            eventPublisher,
            countStrategy,
            conflictStrategy,
            assignmentStrategy,
            rng
        );

        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
        GameSessionId sessionId = GameSessionId.generate();
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId,
            players,
            0, 0, // auto-calculate
            false,
            Collections.emptyMap()
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        assertEquals(8, result.size());
        verify(eventPublisher, times(1)).publish(any(RolesDistributedEvent.class));
    }

    @Test
    public void testDistributeRoles_fixedAssignmentsNoConflict_keepsFixed() {
        // Arrange
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
        ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
        RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
        Random rng = new Random(12345L);

        RoleDistributionService service = new RoleDistributionService(
            eventPublisher,
            countStrategy,
            conflictStrategy,
            assignmentStrategy,
            rng
        );

        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
        Player fixedPlayer = players.get(0);
        Map<Player, ManhuntRoleIdentifier> fixedAssignments = new HashMap<>();
        fixedAssignments.put(fixedPlayer, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);

        GameSessionId sessionId = GameSessionId.generate();
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId,
            players,
            0, 0,
            false,
            fixedAssignments
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        assertEquals(8, result.size());
        assertEquals(ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT, result.get(fixedPlayer));
    }

    @Test
    public void testDistributeRoles_conflictingFixedAssignments_overwritesAll() {
        // Arrange
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
        ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
        RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
        Random rng = new Random(12345L);

        RoleDistributionService service = new RoleDistributionService(
            eventPublisher,
            countStrategy,
            conflictStrategy,
            assignmentStrategy,
            rng
        );

        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(5);
        // Fix 3 speedrunners, but tier only wants 1
        Map<Player, ManhuntRoleIdentifier> fixedAssignments = new HashMap<>();
        fixedAssignments.put(players.get(0), ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        fixedAssignments.put(players.get(1), ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);
        fixedAssignments.put(players.get(2), ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER);

        GameSessionId sessionId = GameSessionId.generate();
        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId,
            players,
            0, 0,
            false,
            fixedAssignments
        );

        // Act
        Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);

        // Assert
        assertEquals(5, result.size());
        // Conflict mode should have cleared fixed assignments
        // So players may no longer have their fixed roles
    }
}
