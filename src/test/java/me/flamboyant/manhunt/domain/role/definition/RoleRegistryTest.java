package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.*;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RoleRegistryTest {
    private RoleRegistry registry;
    private Player mockPlayer;

    @Before
    public void setUp() {
        registry = new RoleRegistry();
        mockPlayer = mock(Player.class);
    }

    @Test
    public void createRole_withRegisteredRole_createsCorrectInstance() {
        // Arrange
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

        // Act
        AManhuntRole role = registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);

        // Assert
        assertNotNull(role);
        assertTrue(role instanceof SpeedrunnerRole);
    }

    @Test(expected = IllegalStateException.class)
    public void register_duplicateIdentifier_throwsIllegalStateException() {
        // Arrange
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

        // Act & Assert
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
    }

    @Test
    public void createRole_unregisteredIdentifier_throwsIllegalArgumentException() {
        // Act & Assert
        try {
            registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("SPEEDRUNNER_SIMPLE"));
        }
    }

    @Test
    public void isRegistered_beforeRegistration_returnsFalse() {
        // Act & Assert
        assertFalse(registry.isRegistered(SPEEDRUNNER_SIMPLE));
    }

    @Test
    public void isRegistered_afterRegistration_returnsTrue() {
        // Arrange
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

        // Act & Assert
        assertTrue(registry.isRegistered(SPEEDRUNNER_SIMPLE));
    }

    @Test
    public void isRegistered_differentIdentifier_returnsFalse() {
        // Arrange
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

        // Act & Assert
        assertFalse(registry.isRegistered(ManhuntRoleIdentifier.HUNTER_SIMPLE));
    }

    @Test
    public void register_allRoleIdentifiers_noConflicts() {
        // Act - Register all 18 roles
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_SWAPPER, SpeedrunnerSwapperRole::new);
        registry.register(SPEEDRUNNER_LINK, LinkSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_CHECKPOINT, CheckpointSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_ELF, ElfSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_WEREWOLF, WerewolfSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_CUTCLEAN, CutCleanSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_TNT_TACTICAL, TntTacticalSpeedrunnerRole::new);
        registry.register(HUNTER_SIMPLE, HunterRole::new);
        registry.register(HUNTER_CHECKPOINT, CheckpointHunterRole::new);
        registry.register(HUNTER_CUTCLEAN, CutCleanHunterRole::new);
        registry.register(HUNTER_LINK, LinkHunterRole::new);
        registry.register(HUNTER_PRO_MINER, ProMinerRole::new);
        registry.register(HUNTER_ELF, ElfHunterRole::new);
        registry.register(SUPER_HUNTER, SuperHunterRole::new);
        registry.register(ALLY_IMPOSTER, ImposterRole::new);
        registry.register(NEUTRAL_GLUER, GluerRole::new);
        registry.register(NEUTRAL_UNDECIDED, UndecidedRole::new);

        // Assert - Verify all registered
        assertTrue(registry.isRegistered(SPEEDRUNNER_SIMPLE));
        assertTrue(registry.isRegistered(HUNTER_SIMPLE));
        assertTrue(registry.isRegistered(ALLY_IMPOSTER));
        assertTrue(registry.isRegistered(NEUTRAL_GLUER));

        // Verify can create instances
        AManhuntRole speedrunner = registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);
        AManhuntRole hunter = registry.createRole(HUNTER_SIMPLE, mockPlayer);
        assertNotNull(speedrunner);
        assertNotNull(hunter);
    }
}
