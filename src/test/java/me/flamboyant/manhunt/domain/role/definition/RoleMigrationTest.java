package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.application.injection.ManhuntModule;
import me.flamboyant.manhunt.domain.role.ability.*;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleMigrationTest {
    private RoleDefinitionRegistry registry;
    private AbilityContext mockContext;

    @BeforeEach
    void setUp() {
        // Use actual registry provider from ManhuntModule
        Plugin mockPlugin = mock(Plugin.class);
        ManhuntModule module = new ManhuntModule(mockPlugin);
        registry = module.provideRoleDefinitionRegistry();
        mockContext = mock(AbilityContext.class);
    }

    @Test
    void allRoleIdentifiersHaveDefinitions() {
        for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
            assertTrue(
                registry.hasDefinition(identifier),
                "Missing definition for " + identifier
            );
        }
    }

    @Test
    void speedrunnerSimpleHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_SIMPLE);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(4, abilities.size(), "Speedrunner should have 4 abilities");
        assertTrue(hasAbilityOfType(abilities, UIPickerCompassAbility.class));
        assertTrue(hasAbilityOfType(abilities, DragonWinConditionAbility.class));
        assertTrue(hasAbilityOfType(abilities, PortalTrackingAbility.class));
        assertTrue(hasAbilityOfType(abilities, SpeedrunnerDeathAbility.class));
    }

    @Test
    void checkpointSpeedrunnerHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_CHECKPOINT);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(6, abilities.size(), "Checkpoint speedrunner should have 6 abilities");
        assertTrue(hasAbilityOfType(abilities, CheckpointSaveAbility.class));
        assertTrue(hasAbilityOfType(abilities, CheckpointRollbackAbility.class));
    }

    @Test
    void linkSpeedrunnerHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_LINK);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertTrue(hasAbilityOfType(abilities, GrassDropAbility.class));
        assertTrue(hasAbilityOfType(abilities, SwordSoundAbility.class));
    }

    @Test
    void hunterSimpleHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(HUNTER_SIMPLE);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(3, abilities.size(), "Hunter should have 3 abilities");
        assertTrue(hasAbilityOfType(abilities, CyclingCompassAbility.class));
        assertTrue(hasAbilityOfType(abilities, CompassOnStartAbility.class));
        assertTrue(hasAbilityOfType(abilities, CompassOnRespawnAbility.class));
    }

    @Test
    void proMinerHunterHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(HUNTER_PRO_MINER);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertTrue(hasAbilityOfType(abilities, ProMinerAbility.class));
    }

    @Test
    void undecidedAllyHasCorrectAbilities() {
        RoleDefinition definition = registry.getDefinition(ALLY_UNDECIDED);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(1, abilities.size());
        assertTrue(hasAbilityOfType(abilities, UndecidedRoleAbility.class));
    }

    @Test
    void superHunterHasWinModifierAbility() {
        RoleDefinition definition = registry.getDefinition(HUNTER_SUPER);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(4, abilities.size(), "Super Hunter should have 4 abilities");
        assertTrue(hasAbilityOfType(abilities, CyclingCompassAbility.class));
        assertTrue(hasAbilityOfType(abilities, CompassOnStartAbility.class));
        assertTrue(hasAbilityOfType(abilities, CompassOnRespawnAbility.class));
        assertTrue(hasAbilityOfType(abilities, SuperHunterWinModifierAbility.class));
    }

    private boolean hasAbilityOfType(List<Ability> abilities, Class<? extends Ability> type) {
        return abilities.stream().anyMatch(type::isInstance);
    }
}
