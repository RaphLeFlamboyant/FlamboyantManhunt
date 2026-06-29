package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CommandsTest {

    @Test
    public void endGameCommand_shouldAcceptSessionIdAndReason() {
        GameSessionId sessionId = GameSessionId.generate();

        EndGameCommand command = new EndGameCommand(sessionId, "Test end");

        assertEquals(sessionId, command.getSessionId());
        assertEquals("Test end", command.getReason());
    }

    @Test
    public void endGameCommand_shouldDefaultReasonIfNull() {
        GameSessionId sessionId = GameSessionId.generate();

        EndGameCommand command = new EndGameCommand(sessionId, null);

        assertEquals("Game ended", command.getReason());
    }

    @Test(expected = IllegalArgumentException.class)
    public void endGameCommand_shouldRejectNullSessionId() {
        new EndGameCommand(null, "reason");
    }

    @Test
    public void distributeRolesCommand_shouldStoreAllParameters() {
        GameSessionId sessionId = GameSessionId.generate();
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        List<Player> players = Arrays.asList(p1, p2);
        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        DistributeRolesCommand command = new DistributeRolesCommand(
            sessionId, players, 1, 0, false, fixed
        );

        assertEquals(sessionId, command.getSessionId());
        assertEquals(2, command.getPlayers().size());
        assertEquals(1, command.getSpeedrunnerCount());
        assertEquals(0, command.getAllyCount());
        assertFalse(command.isSpecialRolesOnly());
        assertEquals(1, command.getFixedAssignments().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void distributeRolesCommand_shouldRejectNullSessionId() {
        Player p1 = mock(Player.class);
        new DistributeRolesCommand(null, Arrays.asList(p1), 1, 0, false, Collections.<Player, ManhuntRoleIdentifier>emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void distributeRolesCommand_shouldRejectNullPlayers() {
        new DistributeRolesCommand(GameSessionId.generate(), null, 1, 0, false, Collections.<Player, ManhuntRoleIdentifier>emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void distributeRolesCommand_shouldRejectEmptyPlayers() {
        new DistributeRolesCommand(GameSessionId.generate(), Collections.<Player>emptyList(), 1, 0, false, Collections.<Player, ManhuntRoleIdentifier>emptyMap());
    }

    @Test
    public void assignRolesCommand_shouldStoreSessionIdAndAssignments() {
        GameSessionId sessionId = GameSessionId.generate();
        Player player = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(player, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        AssignRolesCommand command = new AssignRolesCommand(sessionId, assignments);

        assertEquals(sessionId, command.getSessionId());
        assertEquals(1, command.getRoleAssignments().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void assignRolesCommand_shouldRejectNullSessionId() {
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(mock(Player.class), ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        new AssignRolesCommand(null, assignments);
    }

    @Test(expected = IllegalArgumentException.class)
    public void assignRolesCommand_shouldRejectNullAssignments() {
        new AssignRolesCommand(GameSessionId.generate(), null);
    }

    @Test
    public void startGameCommand_shouldBuildWithValidParameters() {
        Player p1 = mock(Player.class);
        List<Player> players = Collections.singletonList(p1);

        StartGameCommand command = StartGameCommand.builder()
            .players(players)
            .speedrunnerCount(1)
            .allyCount(0)
            .specialRolesOnly(false)
            .resetPlayerStuff(true)
            .surpriseSpeedrunner(false)
            .minutesBeforeRoleReveal(10)
            .fixedRoleAssignments(Collections.<Player, ManhuntRoleIdentifier>emptyMap())
            .build();

        assertNotNull(command);
        assertEquals(1, command.getPlayers().size());
        assertEquals(1, command.getSpeedrunnerCount());
        assertEquals(10, command.getMinutesBeforeRoleReveal());
    }

    @Test(expected = IllegalArgumentException.class)
    public void startGameCommand_shouldRejectEmptyPlayers() {
        StartGameCommand.builder()
            .players(Collections.<Player>emptyList())
            .speedrunnerCount(1)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void startGameCommand_shouldRejectTooManySpeedrunners() {
        Player p1 = mock(Player.class);
        StartGameCommand.builder()
            .players(Collections.singletonList(p1))
            .speedrunnerCount(3)
            .allyCount(2)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void startGameCommand_shouldRejectInvalidRoleRevealTime() {
        Player p1 = mock(Player.class);
        StartGameCommand.builder()
            .players(Collections.singletonList(p1))
            .minutesBeforeRoleReveal(100)
            .build();
    }

    @Test
    public void startGameCommand_shouldCreateImmutablePlayerList() {
        Player p1 = mock(Player.class);
        List<Player> mutableList = new ArrayList<>();
        mutableList.add(p1);

        StartGameCommand command = StartGameCommand.builder()
            .players(mutableList)
            .speedrunnerCount(1)
            .build();

        mutableList.add(mock(Player.class));

        assertEquals(1, command.getPlayers().size());
    }
}
