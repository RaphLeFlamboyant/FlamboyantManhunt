package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.distribution.strategies.OverwriteConflictResolution;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OverwriteConflictResolutionTest {
    private final OverwriteConflictResolution strategy = new OverwriteConflictResolution();

    @Test
    void testResolveConflicts_noConflict_keepsFixedAndAdjustsCounts() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);

        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);

        RoleCounts desired = new RoleCounts(2, 0, 5, 0);
        List<Player> all = Arrays.asList(p1, p2, p3);

        ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);

        assertEquals(1, result.getFixedAssignments().size());
        assertEquals(1, result.getAdjustedCounts().getSpeedrunners()); // 2 - 1 fixed
        assertEquals(0, result.getAdjustedCounts().getAllies());
        assertEquals(2, result.getPlayersNeedingAssignment().size());
    }

    @Test
    void testResolveConflicts_conflict_clearsAllFixed() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);

        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        fixed.put(p2, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);
        fixed.put(p3, ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER);

        RoleCounts desired = new RoleCounts(1, 0, 5, 0); // Want 1, have 3 fixed!
        List<Player> all = Arrays.asList(p1, p2, p3);

        ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);

        assertEquals(0, result.getFixedAssignments().size()); // Cleared
        assertEquals(1, result.getAdjustedCounts().getSpeedrunners()); // Back to desired
        assertEquals(3, result.getPlayersNeedingAssignment().size()); // All players need assignment
    }

    @Test
    void testResolveConflicts_emptyFixed_returnsAllPlayersNeedingAssignment() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);

        Map<Player, ManhuntRoleIdentifier> fixed = Collections.emptyMap();
        RoleCounts desired = new RoleCounts(1, 0, 5, 0);
        List<Player> all = Arrays.asList(p1, p2);

        ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);

        assertEquals(0, result.getFixedAssignments().size());
        assertEquals(desired.getSpeedrunners(), result.getAdjustedCounts().getSpeedrunners());
        assertEquals(desired.getAllies(), result.getAdjustedCounts().getAllies());
        assertEquals(desired.getHunters(), result.getAdjustedCounts().getHunters());
        assertEquals(desired.getNeutrals(), result.getAdjustedCounts().getNeutrals());
        assertEquals(2, result.getPlayersNeedingAssignment().size());
    }
}
