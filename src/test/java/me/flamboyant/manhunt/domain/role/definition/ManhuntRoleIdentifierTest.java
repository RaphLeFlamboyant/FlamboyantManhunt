package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManhuntRoleIdentifierTest {

    @Test
    void testGetRoleType_HunterSimple_ReturnsHunter() {
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_SIMPLE.getRoleType());
    }

    @Test
    void testGetRoleType_SpeedrunnerSimple_ReturnsSpeedrunner() {
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE.getRoleType());
    }

    @Test
    void testGetRoleType_AllyImposter_ReturnsAlly() {
        assertEquals(ManhuntRoleType.ALLY, ManhuntRoleIdentifier.ALLY_IMPOSTER.getRoleType());
    }

    @Test
    void testGetRoleType_NeutralGluer_ReturnsNeutral() {
        assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_GLUER.getRoleType());
    }

    @Test
    void testGetRoleType_AllIdentifiersReturnCorrectType() {
        // Hunters (7 total)
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_SIMPLE.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_CHECKPOINT.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_CUTCLEAN.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_LINK.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_PRO_MINER.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_ELF.getRoleType());
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.SUPER_HUNTER.getRoleType());

        // Speedrunners (8 total)
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_LINK.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_ELF.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_WEREWOLF.getRoleType());
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_TNT_TACTICAL.getRoleType());

        // Allies (1 total)
        assertEquals(ManhuntRoleType.ALLY, ManhuntRoleIdentifier.ALLY_IMPOSTER.getRoleType());

        // Neutrals (2 total)
        assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_GLUER.getRoleType());
        assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_UNDECIDED.getRoleType());
    }

    @Test
    void testGetRoleType_NoNullTypes() {
        for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
            assertNotNull(identifier.getRoleType(),
                "Role identifier " + identifier + " has null type");
        }
    }

    @Test
    void testRoleTypeConsistency_NamingMatchesType() {
        for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
            String name = identifier.name();
            ManhuntRoleType type = identifier.getRoleType();

            // Verify naming convention matches type
            if (name.startsWith("HUNTER") || name.equals("SUPER_HUNTER")) {
                assertEquals(ManhuntRoleType.HUNTER, type,
                    name + " should be HUNTER type");
            } else if (name.startsWith("SPEEDRUNNER")) {
                assertEquals(ManhuntRoleType.SPEEDRUNNER, type,
                    name + " should be SPEEDRUNNER type");
            } else if (name.startsWith("ALLY")) {
                assertEquals(ManhuntRoleType.ALLY, type,
                    name + " should be ALLY type");
            } else if (name.startsWith("NEUTRAL")) {
                assertEquals(ManhuntRoleType.NEUTRAL, type,
                    name + " should be NEUTRAL type");
            }
        }
    }
}
