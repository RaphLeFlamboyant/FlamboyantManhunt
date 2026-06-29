package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleDistributionConfigTest {
    @Test
    void testBalanced_createsConfigWithDefaultTiers() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();

        assertEquals(0, config.getSpeedrunnerCount()); // auto-calculate
        assertEquals(0, config.getAllyCount()); // auto-calculate
        assertFalse(config.isSpecialRolesOnly());
        assertEquals(0.3, config.getSpecialRoleProbability(), 0.001);
        assertFalse(config.getTiers().isEmpty());
    }

    @Test
    void testCustom_createsConfigWithExplicitCounts() {
        RoleDistributionConfig config = RoleDistributionConfig.custom(2, 1);

        assertEquals(2, config.getSpeedrunnerCount());
        assertEquals(1, config.getAllyCount());
        assertFalse(config.isSpecialRolesOnly());
        assertEquals(0.3, config.getSpecialRoleProbability(), 0.001);
    }

    @Test
    void testSpecialOnly_createsConfigWithSpecialFlag() {
        RoleDistributionConfig config = RoleDistributionConfig.specialOnly();

        assertEquals(0, config.getSpeedrunnerCount());
        assertTrue(config.isSpecialRolesOnly());
        assertEquals(1.0, config.getSpecialRoleProbability(), 0.001);
    }

    @Test
    void testConstructor_invalidProbability_throwsException() {
        List<PlayerCountTier> tiers = RoleDistributionConfig.balanced().getTiers();

        assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleDistributionConfig(0, 0, false, 1.5, tiers)
        );
    }

    @Test
    void testConstructor_emptyTiers_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleDistributionConfig(0, 0, false, 0.3, Collections.emptyList())
        );
    }
}
