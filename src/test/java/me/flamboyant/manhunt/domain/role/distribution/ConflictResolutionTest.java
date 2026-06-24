package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConflictResolutionTest {
    @Test
    void testConstructor_validResolution() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        RoleCounts counts = new RoleCounts(1, 0, 5, 0);
        List<Player> needing = Arrays.asList(p2);

        ConflictResolution resolution = new ConflictResolution(fixed, counts, needing);

        assertEquals(1, resolution.getFixedAssignments().size());
        assertEquals(counts, resolution.getAdjustedCounts());
        assertEquals(1, resolution.getPlayersNeedingAssignment().size());
    }

    @Test
    void testConstructor_playerInBothFixedAndNeeding_throwsException() {
        Player p1 = mock(Player.class);
        when(p1.getName()).thenReturn("Player1");

        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        RoleCounts counts = new RoleCounts(1, 0, 5, 0);
        List<Player> needing = Arrays.asList(p1); // Same player!

        InvalidConfigurationException ex = assertThrows(
            InvalidConfigurationException.class,
            () -> new ConflictResolution(fixed, counts, needing)
        );
        assertTrue(ex.getMessage().contains("Player1"));
    }
}
