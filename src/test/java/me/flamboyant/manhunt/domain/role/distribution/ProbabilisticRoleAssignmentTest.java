package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.distribution.strategies.ProbabilisticRoleAssignment;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProbabilisticRoleAssignmentTest {
    private final ProbabilisticRoleAssignment strategy = new ProbabilisticRoleAssignment();

    @Test
    void testAssignRoles_specialOnlyMode_onlySpecialRoles() {
        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(5);
        RoleCounts counts = new RoleCounts(2, 0, 3, 0);
        ConflictResolution resolution = new ConflictResolution(
            Collections.emptyMap(),
            counts,
            players
        );
        RoleDistributionConfig config = RoleDistributionConfig.specialOnly();
        Random rng = RoleDistributionTestFixtures.seededRandom(12345L);

        Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);

        assertEquals(5, result.size());
        // Verify no simple roles
        assertFalse(result.containsValue(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
        assertFalse(result.containsValue(ManhuntRoleIdentifier.HUNTER_SIMPLE));
    }

    @Test
    void testAssignRoles_probabilityMode_deterministic() {
        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(10);
        RoleCounts counts = new RoleCounts(2, 1, 7, 0);
        ConflictResolution resolution = new ConflictResolution(
            Collections.emptyMap(),
            counts,
            players
        );
        RoleDistributionConfig config = RoleDistributionTestFixtures.balancedConfig();
        Random rng = RoleDistributionTestFixtures.seededRandom(99999L);

        Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);

        assertEquals(10, result.size());

        // Count role types
        long speedrunners = result.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .count();
        long allies = result.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.ALLY)
            .count();
        long hunters = result.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.HUNTER)
            .count();

        assertEquals(2, speedrunners);
        assertEquals(1, allies);
        assertEquals(7, hunters);
    }

    @Test
    void testAssignRoles_everyPlayerGetsOneRole() {
        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
        RoleCounts counts = new RoleCounts(1, 0, 7, 0);
        ConflictResolution resolution = new ConflictResolution(
            Collections.emptyMap(),
            counts,
            players
        );
        RoleDistributionConfig config = RoleDistributionTestFixtures.balancedConfig();
        Random rng = RoleDistributionTestFixtures.seededRandom(42L);

        Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);

        assertEquals(8, result.size());
        for (Player player : players) {
            assertTrue(result.containsKey(player), "Player " + player.getName() + " missing assignment");
        }
    }
}
