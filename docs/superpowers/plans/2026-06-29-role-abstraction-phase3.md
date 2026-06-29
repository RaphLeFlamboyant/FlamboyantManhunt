# Role Abstraction - Phase 3: Register Role Definitions

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the role definition system and register all 19 roles. Build parallel system without touching existing roles yet.

**Architecture:** RoleDefinition value objects describe roles as ability compositions. RoleDefinitionRegistry provides lookup.

**Tech Stack:** Java 8, Google Guice, JUnit 5, Mockito

## Global Constraints

- Java 8 compatibility (no `var`, no records, streams OK)
- All definitions in `me.flamboyant.manhunt.domain.role.definition` package
- No changes to existing role classes
- All tests use JUnit 5 + Mockito
- Commit after every completed task

---

## Phase 3 Tasks (3 tasks)

### Task 11: RoleDefinition and RoleDefinitionRegistry

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinition.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/AbilityFactory.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionRegistry.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionTest.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionRegistryTest.java`

**Interfaces:**
- Consumes: `ManhuntRoleIdentifier`, `ManhuntRoleType` (existing enums), `Ability` (from Phase 1)
- Produces: `RoleDefinition.Builder`, `RoleDefinitionRegistry`

- [ ] **Step 1: Write failing test for RoleDefinition**

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleDefinitionTest {
    
    @Test
    void builderCreatesRoleDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test description")
            .withAbility(ctx -> mock(Ability.class))
            .build();
        
        assertEquals(SPEEDRUNNER_SIMPLE, definition.getIdentifier());
        assertEquals(SPEEDRUNNER, definition.getRoleType());
        assertEquals("Speedrunner", definition.getName());
        assertEquals("Test description", definition.getDescription());
    }
    
    @Test
    void createAbilitiesCallsFactories() {
        Ability mockAbility1 = mock(Ability.class);
        Ability mockAbility2 = mock(Ability.class);
        
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Test")
            .description("Test")
            .withAbility(ctx -> mockAbility1)
            .withAbility(ctx -> mockAbility2)
            .build();
        
        AbilityContext mockContext = mock(AbilityContext.class);
        List<Ability> abilities = definition.createAbilities(mockContext);
        
        assertEquals(2, abilities.size());
        assertEquals(mockAbility1, abilities.get(0));
        assertEquals(mockAbility2, abilities.get(1));
    }
    
    @Test
    void builderRequiresAllFields() {
        assertThrows(NullPointerException.class, () ->
            RoleDefinition.builder().build()
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoleDefinitionTest`
Expected: FAIL

- [ ] **Step 3: Create AbilityFactory interface**

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;

@FunctionalInterface
public interface AbilityFactory {
    Ability create(AbilityContext context);
}
```

- [ ] **Step 4: Create RoleDefinition**

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RoleDefinition {
    private final ManhuntRoleIdentifier identifier;
    private final ManhuntRoleType roleType;
    private final String name;
    private final String description;
    private final List<AbilityFactory> abilityFactories;
    
    private RoleDefinition(Builder builder) {
        this.identifier = builder.identifier;
        this.roleType = builder.roleType;
        this.name = builder.name;
        this.description = builder.description;
        this.abilityFactories = builder.abilityFactories;
    }
    
    public List<Ability> createAbilities(AbilityContext context) {
        return abilityFactories.stream()
            .map(factory -> factory.create(context))
            .collect(Collectors.toList());
    }
    
    public ManhuntRoleIdentifier getIdentifier() {
        return identifier;
    }
    
    public ManhuntRoleType getRoleType() {
        return roleType;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ManhuntRoleIdentifier identifier;
        private ManhuntRoleType roleType;
        private String name;
        private String description;
        private List<AbilityFactory> abilityFactories = new ArrayList<>();
        
        public Builder identifier(ManhuntRoleIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }
        
        public Builder roleType(ManhuntRoleType roleType) {
            this.roleType = roleType;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder withAbility(AbilityFactory factory) {
            this.abilityFactories.add(factory);
            return this;
        }
        
        public RoleDefinition build() {
            Objects.requireNonNull(identifier, "identifier required");
            Objects.requireNonNull(roleType, "roleType required");
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(description, "description required");
            return new RoleDefinition(this);
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDefinitionTest`
Expected: PASS (3 tests)

- [ ] **Step 6: Write failing test for RoleDefinitionRegistry**

```java
package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleDefinitionRegistryTest {
    private RoleDefinitionRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new RoleDefinitionRegistry();
    }
    
    @Test
    void registerStoresDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();
        
        registry.register(SPEEDRUNNER_SIMPLE, definition);
        
        assertTrue(registry.hasDefinition(SPEEDRUNNER_SIMPLE));
    }
    
    @Test
    void getDefinitionReturnsRegisteredDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();
        
        registry.register(SPEEDRUNNER_SIMPLE, definition);
        
        assertEquals(definition, registry.getDefinition(SPEEDRUNNER_SIMPLE));
    }
    
    @Test
    void getDefinitionThrowsForUnregistered() {
        assertThrows(IllegalArgumentException.class, () ->
            registry.getDefinition(SPEEDRUNNER_SIMPLE)
        );
    }
    
    @Test
    void registerDuplicateThrows() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();
        
        registry.register(SPEEDRUNNER_SIMPLE, definition);
        
        assertThrows(IllegalArgumentException.class, () ->
            registry.register(SPEEDRUNNER_SIMPLE, definition)
        );
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `mvn test -Dtest=RoleDefinitionRegistryTest`
Expected: FAIL

- [ ] **Step 8: Create RoleDefinitionRegistry**

```java
package me.flamboyant.manhunt.domain.role.definition;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

@Singleton
public class RoleDefinitionRegistry {
    private final Map<ManhuntRoleIdentifier, RoleDefinition> definitions;
    
    @Inject
    public RoleDefinitionRegistry() {
        this.definitions = new EnumMap<>(ManhuntRoleIdentifier.class);
    }
    
    public void register(ManhuntRoleIdentifier identifier, RoleDefinition definition) {
        if (definitions.containsKey(identifier)) {
            throw new IllegalArgumentException("Role definition already registered: " + identifier);
        }
        definitions.put(identifier, definition);
    }
    
    public RoleDefinition getDefinition(ManhuntRoleIdentifier identifier) {
        RoleDefinition definition = definitions.get(identifier);
        if (definition == null) {
            throw new IllegalArgumentException("No role definition for: " + identifier);
        }
        return definition;
    }
    
    public boolean hasDefinition(ManhuntRoleIdentifier identifier) {
        return definitions.containsKey(identifier);
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn test -Dtest=RoleDefinitionTest,RoleDefinitionRegistryTest`
Expected: PASS (7 tests)

- [ ] **Step 10: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinition.java src/main/java/me/flamboyant/manhunt/domain/role/definition/AbilityFactory.java src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionRegistry.java src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionTest.java src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleDefinitionRegistryTest.java
git commit -m "feat(priority-13): add role definition system

- RoleDefinition value object with builder
- AbilityFactory functional interface
- RoleDefinitionRegistry for definition lookup
- Full test coverage"
```

---

### Task 12: Register All 19 Role Definitions

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/ManhuntModule.java`

**Interfaces:**
- Consumes: All ability classes from Phase 2, `RoleDefinitionRegistry` from Task 11
- Produces: Guice provider method that registers all 19 roles

- [ ] **Step 1: Add provider method to ManhuntModule**

```java
@Provides
@Singleton
public RoleDefinitionRegistry provideRoleDefinitionRegistry() {
    RoleDefinitionRegistry registry = new RoleDefinitionRegistry();
    
    // Register all 19 roles
    registerSpeedrunnerVariants(registry);
    registerHunterVariants(registry);
    registerAllyVariants(registry);
    
    return registry;
}

private void registerSpeedrunnerVariants(RoleDefinitionRegistry registry) {
    // SPEEDRUNNER_SIMPLE
    registry.register(SPEEDRUNNER_SIMPLE, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_SIMPLE)
        .roleType(SPEEDRUNNER)
        .name("Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Utiliser une boussole te donne la localisation d'un hunter, avec un " +
                    "cooldown de 15 minutes (mais il te faudra la fabriquer ou la trouver).")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .build());
    
    // SPEEDRUNNER_CHECKPOINT
    CheckpointStorage checkpointStorage = new CheckpointStorage();
    ItemStack checkpointItem = createCheckpointItem();
    ItemStack rollbackItem = createRollbackItem();
    
    registry.register(SPEEDRUNNER_CHECKPOINT, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_CHECKPOINT)
        .roleType(SPEEDRUNNER)
        .name("Checkpoint Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Tu obtiens deux objets. Le premier te permet de poser un checkpoint. " +
                    "Le second te permet de revenir à ton dernier checkpoint.")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new CheckpointSaveAbility(ctx, checkpointItem, Duration.ofSeconds(15), checkpointStorage))
        .withAbility(ctx -> new CheckpointRollbackAbility(ctx, rollbackItem, Duration.ofSeconds(15), checkpointStorage))
        .build());
    
    // SPEEDRUNNER_LINK
    registry.register(SPEEDRUNNER_LINK, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_LINK)
        .roleType(SPEEDRUNNER)
        .name("Link Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Casser des herbes te drop parfois des émeraudes. " +
                    "Tu fais un bruit courageux quand tu attaques avec une épée")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new GrassDropAbility(ctx))
        .withAbility(ctx -> new SwordSoundAbility(ctx, Sound.ENTITY_VILLAGER_AMBIENT))
        .build());
    
    // SPEEDRUNNER_TNT_TACTICAL
    registry.register(SPEEDRUNNER_TNT_TACTICAL, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_TNT_TACTICAL)
        .roleType(SPEEDRUNNER)
        .name("Speedrunner Tactique TNT")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Tu as également une télécommande qui fait exploser le dernier bloc que tu as posé !")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new TntTacticalAbility(ctx))
        .build());
    
    // SPEEDRUNNER_WEREWOLF
    registry.register(SPEEDRUNNER_WEREWOLF, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_WEREWOLF)
        .roleType(SPEEDRUNNER)
        .name("Speedrunner Garou")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "La nuit tu obtiens Force 1, Night Vision et tu peux détecter les hunters avec " +
                    "une boussole toutes les 30 secondes")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new WerewolfNightStrengthAbility(ctx))
        .build());
    
    // SPEEDRUNNER_ELF
    registry.register(SPEEDRUNNER_ELF, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_ELF)
        .roleType(SPEEDRUNNER)
        .name("Elf Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Tu obtiens Speed 1 et Jump Boost 2 en permanence")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new ElfBonusEffectAbility(ctx))
        .build());
    
    // SPEEDRUNNER_CUTCLEAN
    registry.register(SPEEDRUNNER_CUTCLEAN, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_CUTCLEAN)
        .roleType(SPEEDRUNNER)
        .name("CutClean Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Les minerais et la nourriture sont automatiquement cuits")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new CutCleanAbility(ctx))
        .build());
    
    // SPEEDRUNNER_NO_NAME_TAG
    registry.register(SPEEDRUNNER_NO_NAME_TAG, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_NO_NAME_TAG)
        .roleType(SPEEDRUNNER)
        .name("No Name Tag Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Ton nom est caché au-dessus de ta tête")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new NoNameTagAbility(ctx))
        .build());
    
    // SPEEDRUNNER_SWAPPER
    registry.register(SPEEDRUNNER_SWAPPER, RoleDefinition.builder()
        .identifier(SPEEDRUNNER_SWAPPER)
        .roleType(SPEEDRUNNER)
        .name("Swapper Speedrunner")
        .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                    "Tu peux échanger de position avec le dernier joueur ciblé")
        .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
        .withAbility(ctx -> new DragonWinConditionAbility(ctx))
        .withAbility(ctx -> new PortalTrackingAbility(ctx))
        .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
        .withAbility(ctx -> new SwapperAbility(ctx))
        .build());
}

private void registerHunterVariants(RoleDefinitionRegistry registry) {
    // HUNTER_SIMPLE
    registry.register(HUNTER_SIMPLE, RoleDefinition.builder()
        .identifier(HUNTER_SIMPLE)
        .roleType(HUNTER)
        .name("Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position.")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .build());
    
    // HUNTER_CHECKPOINT
    CheckpointStorage hunterCheckpointStorage = new CheckpointStorage();
    ItemStack hunterCheckpointItem = createCheckpointItem();
    ItemStack hunterRollbackItem = createRollbackItem();
    
    registry.register(HUNTER_CHECKPOINT, RoleDefinition.builder()
        .identifier(HUNTER_CHECKPOINT)
        .roleType(HUNTER)
        .name("Checkpoint Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Tu peux poser des checkpoints.")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new CheckpointSaveAbility(ctx, hunterCheckpointItem, Duration.ofSeconds(15), hunterCheckpointStorage))
        .withAbility(ctx -> new CheckpointRollbackAbility(ctx, hunterRollbackItem, Duration.ofSeconds(15), hunterCheckpointStorage))
        .build());
    
    // HUNTER_PRO_MINER
    registry.register(HUNTER_PRO_MINER, RoleDefinition.builder()
        .identifier(HUNTER_PRO_MINER)
        .roleType(HUNTER)
        .name("Pro Miner Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Parfois en minant de la roche, de la " +
                    "deepslate ou de la netherack, tu obtiens du minerai.")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new ProMinerAbility(ctx))
        .build());
    
    // HUNTER_SUPER
    registry.register(HUNTER_SUPER, RoleDefinition.builder()
        .identifier(HUNTER_SUPER)
        .roleType(HUNTER)
        .name("Super Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Conditions de victoire modifiées.")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new SuperHunterWinModifierAbility(ctx))
        .build());
    
    // HUNTER_ELF
    registry.register(HUNTER_ELF, RoleDefinition.builder()
        .identifier(HUNTER_ELF)
        .roleType(HUNTER)
        .name("Elf Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Tu obtiens Speed 1 et Jump Boost 2 en permanence")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new ElfBonusEffectAbility(ctx))
        .build());
    
    // HUNTER_LINK
    registry.register(HUNTER_LINK, RoleDefinition.builder()
        .identifier(HUNTER_LINK)
        .roleType(HUNTER)
        .name("Link Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Casser des herbes te drop parfois des émeraudes.")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new GrassDropAbility(ctx))
        .withAbility(ctx -> new SwordSoundAbility(ctx, Sound.ENTITY_VILLAGER_AMBIENT))
        .build());
    
    // HUNTER_CUTCLEAN
    registry.register(HUNTER_CUTCLEAN, RoleDefinition.builder()
        .identifier(HUNTER_CUTCLEAN)
        .roleType(HUNTER)
        .name("CutClean Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Les minerais et la nourriture sont automatiquement cuits")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new CutCleanAbility(ctx))
        .build());
    
    // HUNTER_GLUER
    registry.register(HUNTER_GLUER, RoleDefinition.builder()
        .identifier(HUNTER_GLUER)
        .roleType(HUNTER)
        .name("Gluer Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Applique Slowness 2 aux speedrunners proches")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new GluerSlownessAbility(ctx))
        .build());
    
    // HUNTER_IMPOSTER
    registry.register(HUNTER_IMPOSTER, RoleDefinition.builder()
        .identifier(HUNTER_IMPOSTER)
        .roleType(HUNTER)
        .name("Imposter Hunter")
        .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                    "te donne sa position. Tu apparais comme un speedrunner aux autres joueurs")
        .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
        .withAbility(ctx -> new CompassOnStartAbility(ctx))
        .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
        .withAbility(ctx -> new ImposterDeceptionAbility(ctx))
        .build());
}

private void registerAllyVariants(RoleDefinitionRegistry registry) {
    // ALLY_UNDECIDED
    registry.register(ALLY_UNDECIDED, RoleDefinition.builder()
        .identifier(ALLY_UNDECIDED)
        .roleType(ALLY)
        .name("Undecided")
        .description("Ton rôle sera révélé plus tard...")
        .withAbility(ctx -> new UndecidedRoleAbility(ctx))
        .build());
}

// Helper methods for item creation
private ItemStack createCheckpointItem() {
    // Implementation from CheckpointSpeedrunnerRole
    return new ItemStack(Material.RECOVERY_COMPASS);
}

private ItemStack createRollbackItem() {
    // Implementation from CheckpointSpeedrunnerRole
    return new ItemStack(Material.ECHO_SHARD);
}
```

- [ ] **Step 2: Add missing imports to ManhuntModule**

```java
import me.flamboyant.manhunt.domain.role.ability.*;
import me.flamboyant.manhunt.domain.role.definition.*;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.time.Duration;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
```

- [ ] **Step 3: Compile and verify no errors**

Run: `mvn compile`
Expected: SUCCESS (no compilation errors)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/ManhuntModule.java
git commit -m "feat(priority-13): register all 19 role definitions

- Provider method in ManhuntModule
- All speedrunner variants (9 roles)
- All hunter variants (9 roles)
- Ally undecided (1 role)
- Helper methods for item creation"
```

---

### Task 13: Migration Tests

**Files:**
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleMigrationTest.java`

**Interfaces:**
- Consumes: `RoleDefinitionRegistry` from Task 12
- Produces: Migration tests that verify all 19 roles have definitions with correct abilities

- [ ] **Step 1: Write migration tests**

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.*;
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
        ManhuntModule module = new ManhuntModule();
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
    
    private boolean hasAbilityOfType(List<Ability> abilities, Class<? extends Ability> type) {
        return abilities.stream().anyMatch(type::isInstance);
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=RoleMigrationTest`
Expected: PASS (all 19 roles verified)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleMigrationTest.java
git commit -m "test(priority-13): add migration tests for role definitions

- Verify all 19 roles have definitions
- Verify correct ability composition per role
- Test speedrunner variants
- Test hunter variants
- Test ally undecided"
```

---

## Phase 3 Summary

**Completed:**
- Task 11: RoleDefinition and RoleDefinitionRegistry
- Task 12: Register all 19 role definitions
- Task 13: Migration tests

**Total:** Role definition system complete, parallel to existing roles

**Next:** Phase 4 - Cutover & Delete old classes

---

**Proceed to:** `2026-06-29-role-abstraction-phase4.md`
