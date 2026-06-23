package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleTypeRegistryTest {

    @Test
    void testGetRolesByType_ReturnsAllHunters() {
        List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);

        assertEquals(7, hunters.size(), "Should have 7 hunter variants");
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_SIMPLE));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CHECKPOINT));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CUTCLEAN));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_LINK));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_PRO_MINER));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_ELF));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.SUPER_HUNTER));
    }

    @Test
    void testGetRolesByType_ReturnsAllSpeedrunners() {
        List<ManhuntRoleIdentifier> speedrunners = RoleTypeRegistry.getRolesByType(ManhuntRoleType.SPEEDRUNNER);

        assertEquals(8, speedrunners.size(), "Should have 8 speedrunner variants");
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_LINK));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_ELF));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_WEREWOLF));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_TNT_TACTICAL));
    }

    @Test
    void testGetRolesByType_ReturnsAllAllies() {
        List<ManhuntRoleIdentifier> allies = RoleTypeRegistry.getRolesByType(ManhuntRoleType.ALLY);

        assertEquals(1, allies.size(), "Should have 1 ally variant");
        assertTrue(allies.contains(ManhuntRoleIdentifier.ALLY_IMPOSTER));
    }

    @Test
    void testGetRolesByType_ReturnsAllNeutrals() {
        List<ManhuntRoleIdentifier> neutrals = RoleTypeRegistry.getRolesByType(ManhuntRoleType.NEUTRAL);

        assertEquals(2, neutrals.size(), "Should have 2 neutral variants");
        assertTrue(neutrals.contains(ManhuntRoleIdentifier.NEUTRAL_GLUER));
        assertTrue(neutrals.contains(ManhuntRoleIdentifier.NEUTRAL_UNDECIDED));
    }

    @Test
    void testGetRolesByType_ReturnsImmutableList() {
        List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);

        assertThrows(UnsupportedOperationException.class, () -> {
            hunters.add(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        }, "Returned list should be immutable");
    }

    @Test
    void testGetRolesByType_ReturnsSameInstanceOnMultipleCalls() {
        List<ManhuntRoleIdentifier> hunters1 = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);
        List<ManhuntRoleIdentifier> hunters2 = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);

        assertSame(hunters1, hunters2, "Should return cached instance");
    }

    @Test
    void testGetRolesByTypeExcluding_SingleExclusion() {
        List<ManhuntRoleIdentifier> speedrunners = RoleTypeRegistry.getRolesByTypeExcluding(
            ManhuntRoleType.SPEEDRUNNER,
            ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
        );

        assertEquals(7, speedrunners.size(), "Should have 7 speedrunners (8 - 1 excluded)");
        assertFalse(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN));
    }

    @Test
    void testGetRolesByTypeExcluding_MultipleExclusions() {
        List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByTypeExcluding(
            ManhuntRoleType.HUNTER,
            ManhuntRoleIdentifier.HUNTER_SIMPLE,
            ManhuntRoleIdentifier.HUNTER_CHECKPOINT
        );

        assertEquals(5, hunters.size(), "Should have 5 hunters (7 - 2 excluded)");
        assertFalse(hunters.contains(ManhuntRoleIdentifier.HUNTER_SIMPLE));
        assertFalse(hunters.contains(ManhuntRoleIdentifier.HUNTER_CHECKPOINT));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CUTCLEAN));
    }

    @Test
    void testGetRolesByTypeExcluding_ExcludeAll() {
        List<ManhuntRoleIdentifier> allies = RoleTypeRegistry.getRolesByTypeExcluding(
            ManhuntRoleType.ALLY,
            ManhuntRoleIdentifier.ALLY_IMPOSTER
        );

        assertEquals(0, allies.size(), "Should have 0 allies when all excluded");
    }

    @Test
    void testCannotInstantiate() {
        assertThrows(AssertionError.class, () -> {
            java.lang.reflect.Constructor<RoleTypeRegistry> constructor =
                RoleTypeRegistry.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }, "Should not be able to instantiate utility class");
    }
}
