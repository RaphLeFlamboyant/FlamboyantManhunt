package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.application.exceptions.CompensationStatus;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ApplicationEventsTest {

    @Test
    public void gameSessionCreatedEvent_shouldStoreSessionIdAndPlayers() {
        GameSessionId sessionId = GameSessionId.generate();
        Player player = mock(Player.class);
        List<Player> players = List.of(player);

        GameSessionCreatedEvent event = new GameSessionCreatedEvent(sessionId, players);

        assertEquals(sessionId, event.getSessionId());
        assertEquals(1, event.getPlayers().size());
        assertTrue(event.getOccurredAtMillis() > 0);
    }

    @Test
    public void gameSessionCreatedEvent_shouldCreateImmutablePlayerList() {
        GameSessionId sessionId = GameSessionId.generate();
        Player player = mock(Player.class);
        List<Player> mutableList = new java.util.ArrayList<>(List.of(player));

        GameSessionCreatedEvent event = new GameSessionCreatedEvent(sessionId, mutableList);
        mutableList.add(mock(Player.class));

        assertEquals(1, event.getPlayers().size());
    }

    @Test
    public void rolesDistributedEvent_shouldStoreAssignments() {
        GameSessionId sessionId = GameSessionId.generate();
        Player player = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> assignments = Map.of(player, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        RolesDistributedEvent event = new RolesDistributedEvent(sessionId, assignments);

        assertEquals(sessionId, event.getSessionId());
        assertEquals(1, event.getAssignments().size());
        assertEquals(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE, event.getAssignments().get(player));
    }

    @Test
    public void rolesDistributedEvent_shouldCreateImmutableAssignmentMap() {
        GameSessionId sessionId = GameSessionId.generate();
        Player player = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> mutableMap = new java.util.HashMap<>();
        mutableMap.put(player, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        RolesDistributedEvent event = new RolesDistributedEvent(sessionId, mutableMap);
        mutableMap.put(mock(Player.class), ManhuntRoleIdentifier.HUNTER_SIMPLE);

        assertEquals(1, event.getAssignments().size());
    }

    @Test
    public void rolesAssignedEvent_shouldStoreSessionId() {
        GameSessionId sessionId = GameSessionId.generate();

        RolesAssignedEvent event = new RolesAssignedEvent(sessionId);

        assertEquals(sessionId, event.getSessionId());
    }

    @Test
    public void handlersRegisteredEvent_shouldStoreSessionId() {
        GameSessionId sessionId = GameSessionId.generate();

        HandlersRegisteredEvent event = new HandlersRegisteredEvent(sessionId);

        assertEquals(sessionId, event.getSessionId());
    }

    @Test
    public void gameStartFailedEvent_shouldStoreCauseAndCompensation() {
        GameSessionId sessionId = GameSessionId.generate();
        Exception cause = new RuntimeException("Test failure");
        CompensationStatus status = new CompensationStatus();
        status.markSessionDeleted();

        GameStartFailedEvent event = new GameStartFailedEvent(sessionId, cause, status);

        assertEquals(sessionId, event.getSessionId());
        assertEquals(cause, event.getCause());
        assertTrue(event.getCompensationStatus().isSessionDeleted());
    }
}
