package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerCountTierTest {
    @Test
    void testMatches_playerCountWithinRange_returnsTrue() {
        PlayerCountTier tier = new PlayerCountTier(4, 7, 1, 0);

        assertTrue(tier.matches(4));
        assertTrue(tier.matches(5));
        assertTrue(tier.matches(7));
    }

    @Test
    void testMatches_playerCountOutsideRange_returnsFalse() {
        PlayerCountTier tier = new PlayerCountTier(4, 7, 1, 0);

        assertFalse(tier.matches(3));
        assertFalse(tier.matches(8));
    }

    @Test
    void testConstructor_invalidMinPlayers_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new PlayerCountTier(0, 5, 1, 0)
        );
    }

    @Test
    void testConstructor_maxLessThanMin_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new PlayerCountTier(8, 4, 1, 0)
        );
    }
}
