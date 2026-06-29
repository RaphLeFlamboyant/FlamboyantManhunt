package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.distribution.strategies.TieredRoleCountStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TieredRoleCountStrategyTest {
    private final TieredRoleCountStrategy strategy = new TieredRoleCountStrategy();

    @Test
    void testDetermineRoleCounts_4players_returns1Speedrunner() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();

        RoleCounts counts = strategy.determineRoleCounts(4, config);

        assertEquals(1, counts.getSpeedrunners());
        assertEquals(0, counts.getAllies());
        assertEquals(3, counts.getHunters());
        assertEquals(0, counts.getNeutrals());
        assertEquals(4, counts.total());
    }

    @Test
    void testDetermineRoleCounts_7players_returns1Speedrunner() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        RoleCounts counts = strategy.determineRoleCounts(7, config);

        assertEquals(1, counts.getSpeedrunners());
        assertEquals(0, counts.getAllies());
        assertEquals(6, counts.getHunters());
    }

    @Test
    void testDetermineRoleCounts_8players_returns2Speedrunners() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        RoleCounts counts = strategy.determineRoleCounts(8, config);

        assertEquals(2, counts.getSpeedrunners());
        assertEquals(0, counts.getAllies());
        assertEquals(6, counts.getHunters());
    }

    @Test
    void testDetermineRoleCounts_13players_returns2Speedrunners1Ally() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        RoleCounts counts = strategy.determineRoleCounts(13, config);

        assertEquals(2, counts.getSpeedrunners());
        assertEquals(1, counts.getAllies());
        assertEquals(10, counts.getHunters());
    }

    @Test
    void testDetermineRoleCounts_20players_returns3Speedrunners1Ally() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        RoleCounts counts = strategy.determineRoleCounts(20, config);

        assertEquals(3, counts.getSpeedrunners());
        assertEquals(1, counts.getAllies());
        assertEquals(16, counts.getHunters());
    }

    @Test
    void testDetermineRoleCounts_explicitCounts_overridesTiers() {
        RoleDistributionConfig config = RoleDistributionConfig.custom(3, 2);
        RoleCounts counts = strategy.determineRoleCounts(10, config);

        assertEquals(3, counts.getSpeedrunners());
        assertEquals(2, counts.getAllies());
        assertEquals(5, counts.getHunters());
    }

    @Test
    void testDetermineRoleCounts_noMatchingTier_throwsException() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();

        InsufficientPlayersException exception = assertThrows(
            InsufficientPlayersException.class,
            () -> strategy.determineRoleCounts(2, config)
        );

        assertTrue(exception.getMessage().contains("No tier found for player count: 2"));
    }
}
