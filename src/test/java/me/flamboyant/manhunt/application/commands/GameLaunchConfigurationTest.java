package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GameLaunchConfigurationTest {

    @Test
    public void testBuilder_setsAllFields() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        List<Player> players = Arrays.asList(player1, player2);

        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(player1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        GameLaunchConfiguration config = GameLaunchConfiguration.builder()
            .players(players)
            .playerRoleAssignments(assignments)
            .speedrunnerCount(2)
            .allyCount(1)
            .specialRolesOnly(true)
            .hiddenSpeedrunner(true)
            .resetPlayerStuff(true)
            .roleRevealDelayMinutes(15)
            .build();

        assertEquals("Players should match", players, config.getPlayers());
        assertEquals("Assignments should match", assignments, config.getPlayerRoleAssignments());
        assertEquals("Speedrunner count should be 2", 2, config.getSpeedrunnerCount());
        assertEquals("Ally count should be 1", 1, config.getAllyCount());
        assertTrue("Special roles only should be true", config.isSpecialRolesOnly());
        assertTrue("Hidden speedrunner should be true", config.isHiddenSpeedrunner());
        assertTrue("Reset player stuff should be true", config.isResetPlayerStuff());
        assertEquals("Role reveal delay should be 15", 15, config.getRoleRevealDelayMinutes());
    }

    @Test
    public void testBuilder_defaultValues() {
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();

        assertTrue("Default players should be empty", config.getPlayers().isEmpty());
        assertTrue("Default assignments should be empty", config.getPlayerRoleAssignments().isEmpty());
        assertEquals("Default speedrunner count should be 1", 1, config.getSpeedrunnerCount());
        assertEquals("Default ally count should be 0", 0, config.getAllyCount());
        assertFalse("Default special roles only should be false", config.isSpecialRolesOnly());
        assertFalse("Default hidden speedrunner should be false", config.isHiddenSpeedrunner());
        assertFalse("Default reset player stuff should be false", config.isResetPlayerStuff());
        assertEquals("Default role reveal delay should be 10", 10, config.getRoleRevealDelayMinutes());
    }

    @Test
    public void testGetPlayers_returnsUnmodifiableList() {
        Player player = mock(Player.class);
        GameLaunchConfiguration config = GameLaunchConfiguration.builder()
            .players(Collections.singletonList(player))
            .build();

        List<Player> players = config.getPlayers();

        try {
            players.add(mock(Player.class));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetPlayerRoleAssignments_returnsUnmodifiableMap() {
        Player player = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>();
        assignments.put(player, ManhuntRoleIdentifier.HUNTER_SIMPLE);

        GameLaunchConfiguration config = GameLaunchConfiguration.builder()
            .playerRoleAssignments(assignments)
            .build();

        Map<Player, ManhuntRoleIdentifier> result = config.getPlayerRoleAssignments();

        try {
            result.put(mock(Player.class), ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testBuilder_handlesNullCollections() {
        GameLaunchConfiguration config = GameLaunchConfiguration.builder()
            .players(null)
            .playerRoleAssignments(null)
            .build();

        assertNotNull("Players should not be null", config.getPlayers());
        assertTrue("Players should be empty when null is passed", config.getPlayers().isEmpty());
        assertNotNull("Player role assignments should not be null", config.getPlayerRoleAssignments());
        assertTrue("Player role assignments should be empty when null is passed", config.getPlayerRoleAssignments().isEmpty());
    }
}
