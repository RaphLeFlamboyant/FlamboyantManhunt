package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleCountsTest {
    @Test
    void testConstructor_validCounts() {
        RoleCounts counts = new RoleCounts(2, 1, 5, 0);

        assertEquals(2, counts.getSpeedrunners());
        assertEquals(1, counts.getAllies());
        assertEquals(5, counts.getHunters());
        assertEquals(0, counts.getNeutrals());
        assertEquals(8, counts.total());
    }

    @Test
    void testConstructor_negativeSpeedrunners_throwsException() {
        InvalidConfigurationException ex = assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleCounts(-1, 0, 5, 0)
        );
        assertTrue(ex.getMessage().contains("non-negative"));
    }

    @Test
    void testConstructor_negativeAllies_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleCounts(1, -1, 5, 0)
        );
    }

    @Test
    void testConstructor_negativeHunters_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleCounts(1, 0, -1, 0)
        );
    }

    @Test
    void testConstructor_negativeNeutrals_throwsException() {
        assertThrows(
            InvalidConfigurationException.class,
            () -> new RoleCounts(1, 0, 5, -1)
        );
    }

    @Test
    void testSubtract_validSubtraction() {
        RoleCounts original = new RoleCounts(3, 2, 10, 1);
        RoleCounts toSubtract = new RoleCounts(1, 1, 3, 0);

        RoleCounts result = original.subtract(toSubtract);

        assertEquals(2, result.getSpeedrunners());
        assertEquals(1, result.getAllies());
        assertEquals(7, result.getHunters());
        assertEquals(1, result.getNeutrals());
    }

    @Test
    void testSubtract_negativeResult_throwsException() {
        RoleCounts original = new RoleCounts(2, 1, 5, 0);
        RoleCounts toSubtract = new RoleCounts(3, 2, 6, 1);

        InvalidConfigurationException ex = assertThrows(
            InvalidConfigurationException.class,
            () -> original.subtract(toSubtract)
        );
        assertTrue(ex.getMessage().contains("negative counts"));
    }

    @Test
    void testExceedsAny_speedrunnersExceed_returnsTrue() {
        RoleCounts limit = new RoleCounts(2, 1, 5, 0);
        RoleCounts actual = new RoleCounts(3, 1, 5, 0);

        assertTrue(actual.exceedsAny(limit));
    }

    @Test
    void testExceedsAny_withinLimits_returnsFalse() {
        RoleCounts limit = new RoleCounts(2, 1, 5, 0);
        RoleCounts actual = new RoleCounts(1, 0, 4, 0);

        assertFalse(actual.exceedsAny(limit));
    }
}
