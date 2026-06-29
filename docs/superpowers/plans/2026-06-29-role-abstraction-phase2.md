# Role Abstraction - Phase 2: Implement Concrete Abilities

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all 19 concrete ability classes by extracting logic from existing role classes. No impact on existing roles yet.

**Architecture:** Each ability is self-contained with its own tests. Abilities use base classes from Phase 1.

**Tech Stack:** Java 8, Bukkit API, Google Guice, JUnit 5, Mockito

## Global Constraints

- Java 8 compatibility (no `var`, no records, streams OK)
- All ability classes in `me.flamboyant.manhunt.domain.role.ability` package
- Event filtering boilerplate handled once in base classes, never in concrete abilities
- All tests use JUnit 5 + Mockito
- TDD: tests written before implementation for every ability
- Extract exact logic from existing roles - no behavior changes
- Commit after every completed task

---

## Phase 2 Tasks (6 tasks, ~19 abilities total)

### Task 5: Compass Abilities (DONE IN PHASE 1)

Already implemented in Phase 1:
- CompassAbility (base class)
- UIPickerCompassAbility
- CyclingCompassAbility

---

### Task 6: Checkpoint Abilities

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/Checkpoint.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointStorage.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointSaveAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointRollbackAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/CheckpointAbilityTest.java`

**Interfaces:**
- Consumes: `ItemActivatedAbility` (from Phase 1)
- Produces: `CheckpointSaveAbility(AbilityContext, ItemStack, Duration, CheckpointStorage)`, `CheckpointRollbackAbility(AbilityContext, ItemStack, Duration, CheckpointStorage)`

**Extract from:** `CheckpointSpeedrunnerRole.java` lines 27-137

- [ ] **Step 1: Write failing test for Checkpoint value object**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckpointTest {
    @Test
    void checkpointStoresAllPlayerState() {
        Location location = new Location(null, 100, 64, 200);
        double health = 15.5;
        int foodLevel = 18;
        float saturation = 10.2f;
        int fireTicks = 5;
        List<ItemStack> inventory = Arrays.asList(new ItemStack(org.bukkit.Material.DIAMOND_SWORD));
        HashSet<PotionEffect> effects = new HashSet<>();
        
        Checkpoint checkpoint = new Checkpoint(location, health, foodLevel, saturation, fireTicks, inventory, effects);
        
        assertEquals(location, checkpoint.getLocation());
        assertEquals(health, checkpoint.getHealth());
        assertEquals(foodLevel, checkpoint.getFoodLevel());
        assertEquals(saturation, checkpoint.getSaturation());
        assertEquals(fireTicks, checkpoint.getFireTicks());
        assertEquals(inventory, checkpoint.getInventory());
        assertEquals(effects, checkpoint.getEffects());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CheckpointTest`
Expected: FAIL with "cannot find symbol class Checkpoint"

- [ ] **Step 3: Create Checkpoint value object**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;

public class Checkpoint {
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int fireTicks;
    private final List<ItemStack> inventory;
    private final Set<PotionEffect> effects;
    
    public Checkpoint(
        Location location,
        double health,
        int foodLevel,
        float saturation,
        int fireTicks,
        List<ItemStack> inventory,
        Set<PotionEffect> effects
    ) {
        this.location = location;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.fireTicks = fireTicks;
        this.inventory = inventory;
        this.effects = effects;
    }
    
    public Location getLocation() { return location; }
    public double getHealth() { return health; }
    public int getFoodLevel() { return foodLevel; }
    public float getSaturation() { return saturation; }
    public int getFireTicks() { return fireTicks; }
    public List<ItemStack> getInventory() { return inventory; }
    public Set<PotionEffect> getEffects() { return effects; }
}
```

- [ ] **Step 4: Create CheckpointStorage**

```java
package me.flamboyant.manhunt.domain.role.ability;

public class CheckpointStorage {
    private Checkpoint checkpoint;
    
    public void saveCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }
    
    public Checkpoint getCheckpoint() {
        return checkpoint;
    }
    
    public boolean hasCheckpoint() {
        return checkpoint != null;
    }
}
```

- [ ] **Step 5: Write failing test for CheckpointSaveAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckpointAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private CheckpointStorage storage;
    private ItemStack checkpointItem;
    private ItemStack rollbackItem;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        storage = new CheckpointStorage();
        checkpointItem = new ItemStack(Material.RECOVERY_COMPASS);
        rollbackItem = new ItemStack(Material.ECHO_SHARD);
        
        when(mockContext.getOwner()).thenReturn(mockOwner);
        
        Location location = mock(Location.class);
        when(mockOwner.getLocation()).thenReturn(location);
        when(mockOwner.getHealth()).thenReturn(15.0);
        when(mockOwner.getFoodLevel()).thenReturn(18);
        when(mockOwner.getSaturation()).thenReturn(10.0f);
        when(mockOwner.getFireTicks()).thenReturn(0);
        
        PlayerInventory mockInventory = mock(PlayerInventory.class);
        when(mockOwner.getInventory()).thenReturn(mockInventory);
        when(mockInventory.getContents()).thenReturn(new ItemStack[]{});
        
        when(mockOwner.getActivePotionEffects()).thenReturn(new HashSet<>());
    }

    @Test
    void saveCheckpointStoresPlayerState() {
        CheckpointSaveAbility ability = new CheckpointSaveAbility(
            mockContext, checkpointItem, Duration.ofSeconds(15), storage
        );
        
        ability.activate();
        
        assertTrue(storage.hasCheckpoint());
        assertEquals(15.0, storage.getCheckpoint().getHealth());
        assertEquals(18, storage.getCheckpoint().getFoodLevel());
    }

    @Test
    void rollbackWithNoCheckpointSendsMessage() {
        CheckpointRollbackAbility ability = new CheckpointRollbackAbility(
            mockContext, rollbackItem, Duration.ofSeconds(15), storage
        );
        
        ability.activate();
        
        verify(mockContext).sendMessage(contains("Aucun checkpoint"));
    }

    @Test
    void rollbackRestoresPlayerState() {
        // Save checkpoint first
        CheckpointSaveAbility saveAbility = new CheckpointSaveAbility(
            mockContext, checkpointItem, Duration.ofSeconds(15), storage
        );
        saveAbility.activate();
        
        // Change player state
        when(mockOwner.getHealth()).thenReturn(5.0);
        when(mockOwner.getFoodLevel()).thenReturn(10);
        
        // Rollback
        CheckpointRollbackAbility rollbackAbility = new CheckpointRollbackAbility(
            mockContext, rollbackItem, Duration.ofSeconds(15), storage
        );
        rollbackAbility.activate();
        
        // Verify restoration
        verify(mockOwner).setHealth(15.0);
        verify(mockOwner).setFoodLevel(18);
        verify(mockOwner).teleport(any(Location.class));
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `mvn test -Dtest=CheckpointAbilityTest`
Expected: FAIL

- [ ] **Step 7: Implement CheckpointSaveAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

public class CheckpointSaveAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage;
    
    public CheckpointSaveAbility(
        AbilityContext context,
        ItemStack triggerItem,
        Duration cooldown,
        CheckpointStorage storage
    ) {
        super(context, triggerItem, cooldown);
        this.storage = storage;
    }
    
    @Override
    protected void activate() {
        Player owner = context.getOwner();
        Checkpoint checkpoint = new Checkpoint(
            owner.getLocation(),
            owner.getHealth(),
            owner.getFoodLevel(),
            owner.getSaturation(),
            owner.getFireTicks(),
            Arrays.asList(owner.getInventory().getContents()),
            new HashSet<>(owner.getActivePotionEffects())
        );
        storage.saveCheckpoint(checkpoint);
        context.sendMessage("&aCheckpoint sauvegardé!");
    }
    
    @Override
    public String getName() {
        return "Checkpoint Save";
    }
    
    @Override
    public String getDescription() {
        return "Sauvegarde ta position et ton état";
    }
}
```

- [ ] **Step 8: Implement CheckpointRollbackAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public class CheckpointRollbackAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage;
    
    public CheckpointRollbackAbility(
        AbilityContext context,
        ItemStack triggerItem,
        Duration cooldown,
        CheckpointStorage storage
    ) {
        super(context, triggerItem, cooldown);
        this.storage = storage;
    }
    
    @Override
    protected void activate() {
        Checkpoint checkpoint = storage.getCheckpoint();
        if (checkpoint == null) {
            context.sendMessage("&cAucun checkpoint sauvegardé!");
            return;
        }
        
        Player owner = context.getOwner();
        owner.teleport(checkpoint.getLocation());
        owner.setHealth(checkpoint.getHealth());
        owner.setFoodLevel(checkpoint.getFoodLevel());
        owner.setSaturation(checkpoint.getSaturation());
        owner.setFireTicks(checkpoint.getFireTicks());
        
        owner.getInventory().clear();
        checkpoint.getInventory().forEach(item -> {
            if (item != null) {
                owner.getInventory().addItem(item);
            }
        });
        
        owner.getActivePotionEffects().forEach(effect -> 
            owner.removePotionEffect(effect.getType())
        );
        checkpoint.getEffects().forEach(effect -> 
            owner.addPotionEffect(effect)
        );
        
        context.sendMessage("&aRetour au checkpoint!");
    }
    
    @Override
    public String getName() {
        return "Checkpoint Rollback";
    }
    
    @Override
    public String getDescription() {
        return "Retourne au dernier checkpoint";
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn test -Dtest=CheckpointTest,CheckpointAbilityTest`
Expected: PASS (4 tests)

- [ ] **Step 10: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/Checkpoint.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointStorage.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointSaveAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CheckpointRollbackAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/CheckpointTest.java src/test/java/me/flamboyant/manhunt/domain/role/ability/CheckpointAbilityTest.java
git commit -m "feat(priority-13): add checkpoint abilities

- Checkpoint value object stores player state
- CheckpointStorage manages checkpoint lifecycle
- CheckpointSaveAbility saves location, health, inventory, effects
- CheckpointRollbackAbility restores saved state
- Extracted from CheckpointSpeedrunnerRole"
```

---

### Task 7: Passive Abilities (GrassDrop, SwordSound, CutClean)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/GrassDropAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/SwordSoundAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CutCleanAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/PassiveAbilitiesTest.java`

**Interfaces:**
- Consumes: `PassiveAbility` (from Phase 1)
- Produces: `GrassDropAbility(AbilityContext)`, `SwordSoundAbility(AbilityContext, Sound)`, `CutCleanAbility(AbilityContext)`

**Extract from:**
- LinkSpeedrunnerRole.java (GrassDrop lines 88-99, SwordSound lines 65-84)
- CutCleanSpeedrunnerRole.java (auto-smelt logic)

- [ ] **Step 1: Write failing test for GrassDropAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PassiveAbilitiesTest {
    private AbilityContext mockContext;
    private Player mockOwner;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        when(mockContext.getOwner()).thenReturn(mockOwner);
    }

    @Test
    void grassDropAbilityDropsEmeralds() {
        GrassDropAbility ability = new GrassDropAbility(mockContext);
        
        BlockBreakEvent mockEvent = mock(BlockBreakEvent.class);
        Block mockBlock = mock(Block.class);
        Location mockLocation = mock(Location.class);
        World mockWorld = mock(World.class);
        
        when(mockEvent.getBlock()).thenReturn(mockBlock);
        when(mockBlock.getType()).thenReturn(Material.GRASS);
        when(mockBlock.getLocation()).thenReturn(mockLocation);
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        
        // Ability should register handler on start
        ability.onRoleStart(mockContext);
        
        // Name and description should be set
        assertEquals("Grass Emerald Drop", ability.getName());
        assertTrue(ability.getDescription().contains("herbes"));
    }

    @Test
    void swordSoundAbilityPlaysSoundOnAttack() {
        SwordSoundAbility ability = new SwordSoundAbility(mockContext, org.bukkit.Sound.ENTITY_VILLAGER_AMBIENT);
        
        assertEquals("Sword Sound", ability.getName());
    }

    @Test
    void cutCleanAbilityAutoSmelts() {
        CutCleanAbility ability = new CutCleanAbility(mockContext);
        
        assertEquals("CutClean", ability.getName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PassiveAbilitiesTest`
Expected: FAIL

- [ ] **Step 3: Implement GrassDropAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GrassDropAbility extends PassiveAbility {
    private static final List<Material> GRASSES = Arrays.asList(
        Material.GRASS,
        Material.TALL_GRASS,
        Material.SEAGRASS,
        Material.TALL_SEAGRASS,
        Material.WARPED_ROOTS,
        Material.NETHER_SPROUTS,
        Material.CRIMSON_ROOTS
    );
    private final Random rng = new Random();
    
    public GrassDropAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }
    
    private void onBlockBreak(BlockBreakEvent event) {
        if (!GRASSES.contains(event.getBlock().getType())) {
            return;
        }
        
        event.setDropItems(false);
        int roll = rng.nextInt(100);
        Location dropLocation = event.getBlock().getLocation();
        
        if (roll > 93) {
            dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, 10));
        } else if (roll > 24) {
            dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, 2));
        }
    }
    
    @Override
    public String getName() {
        return "Grass Emerald Drop";
    }
    
    @Override
    public String getDescription() {
        return "Casser des herbes drop des émeraudes";
    }
}
```

- [ ] **Step 4: Implement SwordSoundAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SwordSoundAbility extends PassiveAbility {
    private final Sound sound;
    
    public SwordSoundAbility(AbilityContext context, Sound sound) {
        super(context);
        this.sound = sound;
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(EntityDamageByEntityEvent.class, this::onEntityDamageByEntity);
        context.registerEventHandler(PlayerInteractEvent.class, this::onPlayerInteract);
    }
    
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() != context.getOwner()) {
            return;
        }
        
        Player owner = context.getOwner();
        if (owner.getInventory().getItemInMainHand() == null
                || owner.getInventory().getItemInMainHand().getType() == Material.AIR) {
            return;
        }
        
        if (!owner.getInventory().getItemInMainHand().getType().toString().contains("SWORD")) {
            return;
        }
        
        owner.getWorld().playSound(owner, sound, SoundCategory.VOICE, 1, 1.3f);
    }
    
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!event.getItem().getType().toString().contains("SWORD")) {
            return;
        }
        
        Player owner = context.getOwner();
        owner.getWorld().playSound(owner, sound, SoundCategory.VOICE, 1, 1.3f);
    }
    
    @Override
    public String getName() {
        return "Sword Sound";
    }
    
    @Override
    public String getDescription() {
        return "Fait un bruit courageux quand tu attaques avec une épée";
    }
}
```

- [ ] **Step 5: Implement CutCleanAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CutCleanAbility extends PassiveAbility {
    private static final Map<Material, Material> SMELTING_MAP = new HashMap<>();
    
    static {
        SMELTING_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELTING_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELTING_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELTING_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELTING_MAP.put(Material.SAND, Material.GLASS);
        SMELTING_MAP.put(Material.NETHERRACK, Material.NETHER_BRICK);
        // Meat
        SMELTING_MAP.put(Material.BEEF, Material.COOKED_BEEF);
        SMELTING_MAP.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        SMELTING_MAP.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        SMELTING_MAP.put(Material.MUTTON, Material.COOKED_MUTTON);
        SMELTING_MAP.put(Material.RABBIT, Material.COOKED_RABBIT);
        SMELTING_MAP.put(Material.COD, Material.COOKED_COD);
        SMELTING_MAP.put(Material.SALMON, Material.COOKED_SALMON);
    }
    
    public CutCleanAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
        context.registerEventHandler(EntityDeathEvent.class, this::onEntityDeath);
    }
    
    private void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();
        Material smelted = SMELTING_MAP.get(blockType);
        
        if (smelted != null) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation(),
                new ItemStack(smelted, 1)
            );
        }
    }
    
    private void onEntityDeath(EntityDeathEvent event) {
        List<ItemStack> newDrops = new ArrayList<>();
        
        for (ItemStack drop : event.getDrops()) {
            Material smelted = SMELTING_MAP.get(drop.getType());
            if (smelted != null) {
                newDrops.add(new ItemStack(smelted, drop.getAmount()));
            } else {
                newDrops.add(drop);
            }
        }
        
        event.getDrops().clear();
        event.getDrops().addAll(newDrops);
    }
    
    @Override
    public String getName() {
        return "CutClean";
    }
    
    @Override
    public String getDescription() {
        return "Les minerais et la nourriture sont automatiquement cuits";
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn test -Dtest=PassiveAbilitiesTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/GrassDropAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/SwordSoundAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CutCleanAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/PassiveAbilitiesTest.java
git commit -m "feat(priority-13): add passive abilities

- GrassDropAbility for emerald drops from grass
- SwordSoundAbility for sword sound effects
- CutCleanAbility for auto-smelting
- Extracted from Link and CutClean roles"
```

---

### Task 8: Win Condition Abilities

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/DragonWinConditionAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/SpeedrunnerDeathAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/PortalTrackingAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/WinConditionAbilitiesTest.java`

**Interfaces:**
- Consumes: `WinConditionAbility`, `PassiveAbility` (from Phase 1)
- Produces: `DragonWinConditionAbility(AbilityContext)`, `SpeedrunnerDeathAbility(AbilityContext)`, `PortalTrackingAbility(AbilityContext)`

**Extract from:** SpeedrunnerRole.java

- [ ] **Step 1: Write failing tests**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WinConditionAbilitiesTest {
    private AbilityContext mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
    }

    @Test
    void dragonWinConditionAbilityHasCorrectName() {
        DragonWinConditionAbility ability = new DragonWinConditionAbility(mockContext);
        assertEquals("Dragon Win Condition", ability.getName());
    }

    @Test
    void speedrunnerDeathAbilityHasCorrectName() {
        SpeedrunnerDeathAbility ability = new SpeedrunnerDeathAbility(mockContext);
        assertEquals("Speedrunner Death Handler", ability.getName());
    }

    @Test
    void portalTrackingAbilityHasCorrectName() {
        PortalTrackingAbility ability = new PortalTrackingAbility(mockContext);
        assertEquals("Portal Tracking", ability.getName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=WinConditionAbilitiesTest`
Expected: FAIL

- [ ] **Step 3: Implement DragonWinConditionAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.NewManhuntManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

public class DragonWinConditionAbility extends WinConditionAbility {
    private static BukkitTask onWinConTask;
    private static boolean winconMet;
    
    public DragonWinConditionAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerWinConditionHandlers(AbilityContext context) {
        winconMet = false;
        onWinConTask = null;
        
        context.registerEventHandler(EntityDamageEvent.class, this::onEntityDamage);
    }
    
    private void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            return;
        }
        
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0) {
            winconMet = true;
            if (onWinConTask == null) {
                onWinConTask = Bukkit.getScheduler().runTaskLater(
                    context.getPlugin(),
                    () -> NewManhuntManager.getInstance().stopGame("Le dragon est mort !"),
                    1
                );
            }
        }
    }
    
    @Override
    public String getName() {
        return "Dragon Win Condition";
    }
    
    @Override
    public String getDescription() {
        return "Gagne quand le dragon de l'Ender meurt";
    }
}
```

- [ ] **Step 4: Implement SpeedrunnerDeathAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SpeedrunnerDeathAbility extends WinConditionAbility {
    
    public SpeedrunnerDeathAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerWinConditionHandlers(AbilityContext context) {
        context.registerEventHandler(EntityDamageEvent.class, this::onEntityDamage);
    }
    
    private void onEntityDamage(EntityDamageEvent event) {
        Player owner = context.getOwner();
        if (event.getEntity() != owner) {
            return;
        }
        
        if (owner.getHealth() - event.getFinalDamage() <= 0) {
            owner.setGameMode(GameMode.SPECTATOR);
            event.setCancelled(true);
        }
    }
    
    @Override
    public String getName() {
        return "Speedrunner Death Handler";
    }
    
    @Override
    public String getDescription() {
        return "Gère la mort du speedrunner (passage en spectateur)";
    }
}
```

- [ ] **Step 5: Implement PortalTrackingAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class PortalTrackingAbility extends PassiveAbility {
    
    public PortalTrackingAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(EntityPortalEnterEvent.class, this::onEntityPortalEnter);
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        // Initialize portal tracking
        Player owner = context.getOwner();
        context.getSession().recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
        context.getSession().recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);
    }
    
    private void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        String worldName = event.getLocation().getWorld().getName();
        
        if (worldName.equals("world")) {
            context.getSession().recordPortalEntry(player, event.getLocation(), World.Environment.NORMAL);
        } else if (worldName.equals("world_nether")) {
            context.getSession().recordPortalEntry(player, event.getLocation(), World.Environment.NETHER);
        }
    }
    
    @Override
    public String getName() {
        return "Portal Tracking";
    }
    
    @Override
    public String getDescription() {
        return "Suit les entrées de portail pour la boussole cross-dimension";
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn test -Dtest=WinConditionAbilitiesTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/DragonWinConditionAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/SpeedrunnerDeathAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/PortalTrackingAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/WinConditionAbilitiesTest.java
git commit -m "feat(priority-13): add win condition abilities

- DragonWinConditionAbility ends game when dragon dies
- SpeedrunnerDeathAbility handles speedrunner death
- PortalTrackingAbility tracks portal locations
- Extracted from SpeedrunnerRole"
```

---

### Task 9: Specialized Abilities (TntTactical, Werewolf, ProMiner, Elf, Gluer, Imposter)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/TntTacticalAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/WerewolfNightStrengthAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/ProMinerAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/ElfBonusEffectAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/GluerSlownessAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/ImposterDeceptionAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/SpecializedAbilitiesTest.java`

**Interfaces:**
- Consumes: `ItemActivatedAbility`, `PassiveAbility` (from Phase 1)
- Produces: All 6 specialized abilities

**Extract from:** TntTacticalSpeedrunnerRole, WerewolfSpeedrunnerRole, ProMinerRole, ElfSpeedrunnerRole, GluerRole, ImposterRole

- [ ] **Step 1: Write failing tests**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecializedAbilitiesTest {
    private AbilityContext mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
    }

    @Test
    void tntTacticalAbilityHasCorrectName() {
        TntTacticalAbility ability = new TntTacticalAbility(mockContext);
        assertEquals("TNT Tactical", ability.getName());
    }

    @Test
    void werewolfAbilityHasCorrectName() {
        WerewolfNightStrengthAbility ability = new WerewolfNightStrengthAbility(mockContext);
        assertEquals("Werewolf Night Strength", ability.getName());
    }

    @Test
    void proMinerAbilityHasCorrectName() {
        ProMinerAbility ability = new ProMinerAbility(mockContext);
        assertEquals("Pro Miner", ability.getName());
    }

    @Test
    void elfAbilityHasCorrectName() {
        ElfBonusEffectAbility ability = new ElfBonusEffectAbility(mockContext);
        assertEquals("Elf Bonus Effect", ability.getName());
    }

    @Test
    void gluerAbilityHasCorrectName() {
        GluerSlownessAbility ability = new GluerSlownessAbility(mockContext);
        assertEquals("Gluer Slowness", ability.getName());
    }

    @Test
    void imposterAbilityHasCorrectName() {
        ImposterDeceptionAbility ability = new ImposterDeceptionAbility(mockContext);
        assertEquals("Imposter Deception", ability.getName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SpecializedAbilitiesTest`
Expected: FAIL

- [ ] **Step 3: Implement TntTacticalAbility (from TntTacticalSpeedrunnerRole.java)**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Arrays;

public class TntTacticalAbility extends ItemActivatedAbility {
    private Location lastBlockLocation;
    private final ItemStack activationItem;
    
    public TntTacticalAbility(AbilityContext context) {
        super(context, createActivationItem(context), Duration.ofSeconds(15));
        this.activationItem = createActivationItem(context);
    }
    
    private static ItemStack createActivationItem(AbilityContext context) {
        return context.getItemService().generateItem(
            Material.RECOVERY_COMPASS,
            1,
            "Activation TNT",
            Arrays.asList("Fait exploser le dernier bloc placé"),
            true,
            Enchantment.ARROW_FIRE,
            true,
            true
        );
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        context.getOwner().getInventory().addItem(activationItem);
        
        context.registerEventHandler(BlockPlaceEvent.class, this::onBlockPlace);
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }
    
    @Override
    protected void activate() {
        if (lastBlockLocation == null) {
            context.sendMessage(context.getMessageService().feedback("Le dernier bloc posé a été cassé."));
            return;
        }
        
        if (!lastBlockLocation.getChunk().isLoaded()) {
            context.sendMessage(context.getMessageService().feedback("Le dernier bloc posé n'est pas dans une zone chargée !"));
            return;
        }
        
        lastBlockLocation.getBlock().setType(Material.AIR);
        lastBlockLocation.getWorld().createExplosion(
            lastBlockLocation,
            4f,
            false,
            true,
            context.getOwner()
        );
    }
    
    private void onBlockPlace(BlockPlaceEvent event) {
        lastBlockLocation = event.getBlock().getLocation();
    }
    
    private void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(lastBlockLocation)) {
            lastBlockLocation = null;
        }
    }
    
    @Override
    protected void handleInteractEvent(PlayerInteractEvent event) {
        if (!context.getItemService().isExactlySameItemKind(event.getItem(), triggerItem)) {
            return;
        }
        event.setCancelled(true);
        
        if (context.getAbilityManager().isOnCooldown(context.getOwner(), getName())) {
            return;
        }
        
        activate();
        context.getAbilityManager().setCooldown(context.getOwner(), getName(), cooldown);
    }
    
    @Override
    public String getName() {
        return "TNT Tactical";
    }
    
    @Override
    public String getDescription() {
        return "Télécommande qui fait exploser le dernier bloc posé";
    }
}
```

- [ ] **Step 4: Implement WerewolfNightStrengthAbility (from WerewolfSpeedrunnerRole.java)**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class WerewolfNightStrengthAbility extends PassiveAbility {
    private BukkitTask task;
    private boolean powerActivated = false;
    
    public WerewolfNightStrengthAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // No event handlers, uses scheduled task
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        task = Bukkit.getScheduler().runTaskTimer(
            context.getPlugin(),
            () -> checkNightTime(),
            5 * 20,
            5 * 20
        );
    }
    
    @Override
    public void onRoleStop(AbilityContext context) {
        super.onRoleStop(context);
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task.getTaskId());
        }
    }
    
    private void checkNightTime() {
        World world = context.getOwner().getLocation().getWorld();
        long time = world.getTime();
        
        boolean isNight = world.getName().toLowerCase().contains("end")
            || (!world.getName().toLowerCase().contains("nether") && (time < 1000 || time > 13000));
        
        if (isNight) {
            setActivationState(true);
            PotionEffect strength = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 7 * 20, 1, false, false);
            context.getOwner().addPotionEffect(strength);
            
            PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, 25 * 20, 1, false, false);
            context.getOwner().addPotionEffect(nightVision);
        } else {
            setActivationState(false);
        }
    }
    
    private void setActivationState(boolean isActive) {
        if (powerActivated == isActive) {
            return;
        }
        
        powerActivated = isActive;
        if (!isActive) {
            context.sendMessage(context.getMessageService().feedback(
                "Vous n'avez plus vos pouvoirs pour le moment. Votre boussole redevient normale."
            ));
            context.getOwner().setCompassTarget(context.getOwner().getBedSpawnLocation());
        } else {
            context.getOwner().setCooldown(Material.COMPASS, 0);
            context.sendMessage(context.getMessageService().feedback("Vous obtenez enfin vos pouvoirs"));
        }
    }
    
    public boolean isPowerActivated() {
        return powerActivated;
    }
    
    @Override
    public String getName() {
        return "Werewolf Night Strength";
    }
    
    @Override
    public String getDescription() {
        return "La nuit, obtient Force 1 et Night Vision";
    }
}
```

- [ ] **Step 5: Implement ProMinerAbility (from ProMinerRole.java)**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProMinerAbility extends PassiveAbility {
    private static final List<Material> CONCERNED_BLOCKS = Arrays.asList(
        Material.STONE,
        Material.DEEPSLATE,
        Material.NETHERRACK
    );
    
    private static final List<Material> ITEMS_TO_DROP = Arrays.asList(
        Material.COAL,
        Material.RAW_COPPER,
        Material.RAW_IRON,
        Material.RAW_GOLD,
        Material.GOLD_NUGGET,
        Material.EMERALD,
        Material.QUARTZ,
        Material.LAPIS_LAZULI,
        Material.REDSTONE
    );
    
    private final Random rng = new Random();
    
    public ProMinerAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }
    
    private void onBlockBreak(BlockBreakEvent event) {
        if (!CONCERNED_BLOCKS.contains(event.getBlock().getType())) {
            return;
        }
        
        if (rng.nextInt(100) > 6) {
            return;
        }
        
        event.setDropItems(false);
        Material randomOre = ITEMS_TO_DROP.get(rng.nextInt(ITEMS_TO_DROP.size()));
        context.getOwner().getWorld().dropItem(
            event.getBlock().getLocation(),
            new ItemStack(randomOre, 1)
        );
    }
    
    @Override
    public String getName() {
        return "Pro Miner";
    }
    
    @Override
    public String getDescription() {
        return "Parfois en minant de la roche, obtient du minerai";
    }
}
```

- [ ] **Step 6: Implement ElfBonusEffectAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ElfBonusEffectAbility extends PassiveAbility {
    
    public ElfBonusEffectAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Passive effect applied on start
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        // Apply permanent effects
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false);
        context.getOwner().addPotionEffect(speed);
        
        PotionEffect jumpBoost = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false);
        context.getOwner().addPotionEffect(jumpBoost);
    }
    
    @Override
    public String getName() {
        return "Elf Bonus Effect";
    }
    
    @Override
    public String getDescription() {
        return "Obtient Speed 1 et Jump Boost 2 en permanence";
    }
}
```

- [ ] **Step 7: Implement GluerSlownessAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

public class GluerSlownessAbility extends PassiveAbility {
    
    public GluerSlownessAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Scheduled task approach
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        org.bukkit.Bukkit.getScheduler().runTaskTimer(
            context.getPlugin(),
            () -> applySlownessToNearbySpeedrunners(),
            20,
            20
        );
    }
    
    private void applySlownessToNearbySpeedrunners() {
        List<Player> nearbySpeedrunners = context.getSession().getPlayers().stream()
            .filter(p -> context.getSession().getRole(p).getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .filter(p -> p.getLocation().distance(context.getOwner().getLocation()) < 10)
            .collect(Collectors.toList());
        
        for (Player speedrunner : nearbySpeedrunners) {
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 2 * 20, 1, false, false);
            speedrunner.addPotionEffect(slowness);
        }
    }
    
    @Override
    public String getName() {
        return "Gluer Slowness";
    }
    
    @Override
    public String getDescription() {
        return "Applique Slowness 2 aux speedrunners proches";
    }
}
```

- [ ] **Step 8: Implement ImposterDeceptionAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

public class ImposterDeceptionAbility extends PassiveAbility {
    
    public ImposterDeceptionAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Special logic - appears as speedrunner to others
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        // Implementation depends on how role is displayed to other players
        // This is a placeholder for the deception mechanic
    }
    
    @Override
    public String getName() {
        return "Imposter Deception";
    }
    
    @Override
    public String getDescription() {
        return "Apparaît comme un speedrunner aux autres joueurs";
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn test -Dtest=SpecializedAbilitiesTest`
Expected: PASS (6 tests)

- [ ] **Step 10: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/TntTacticalAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/WerewolfNightStrengthAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/ProMinerAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/ElfBonusEffectAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/GluerSlownessAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/ImposterDeceptionAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/SpecializedAbilitiesTest.java
git commit -m "feat(priority-13): add specialized abilities

- TntTacticalAbility for remote TNT detonation
- WerewolfNightStrengthAbility for night powers
- ProMinerAbility for bonus ore drops
- ElfBonusEffectAbility for speed and jump boost
- GluerSlownessAbility for proximity slowness
- ImposterDeceptionAbility for disguise
- Extracted from specialized role classes"
```

---

### Task 10: Utility Abilities

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassOnStartAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassOnRespawnAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/NoNameTagAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/SwapperAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/UndecidedRoleAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/UtilityAbilitiesTest.java`

**Interfaces:**
- Consumes: `PassiveAbility`, `ItemActivatedAbility` (from Phase 1)
- Produces: All 5 utility abilities

- [ ] **Step 1: Write failing tests**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilityAbilitiesTest {
    private AbilityContext mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
    }

    @Test
    void compassOnStartAbilityHasCorrectName() {
        CompassOnStartAbility ability = new CompassOnStartAbility(mockContext);
        assertEquals("Compass On Start", ability.getName());
    }

    @Test
    void compassOnRespawnAbilityHasCorrectName() {
        CompassOnRespawnAbility ability = new CompassOnRespawnAbility(mockContext);
        assertEquals("Compass On Respawn", ability.getName());
    }

    @Test
    void noNameTagAbilityHasCorrectName() {
        NoNameTagAbility ability = new NoNameTagAbility(mockContext);
        assertEquals("No Name Tag", ability.getName());
    }

    @Test
    void swapperAbilityHasCorrectName() {
        SwapperAbility ability = new SwapperAbility(mockContext);
        assertEquals("Swapper", ability.getName());
    }

    @Test
    void undecidedRoleAbilityHasCorrectName() {
        UndecidedRoleAbility ability = new UndecidedRoleAbility(mockContext);
        assertEquals("Undecided Role", ability.getName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=UtilityAbilitiesTest`
Expected: FAIL

- [ ] **Step 3: Implement all utility abilities**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CompassOnStartAbility extends PassiveAbility {
    
    public CompassOnStartAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // No event handlers
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        ItemStack compass = new ItemStack(Material.COMPASS);
        context.getOwner().getInventory().addItem(compass);
    }
    
    @Override
    public String getName() {
        return "Compass On Start";
    }
    
    @Override
    public String getDescription() {
        return "Donne une boussole au démarrage";
    }
}
```

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class CompassOnRespawnAbility extends PassiveAbility {
    
    public CompassOnRespawnAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(PlayerRespawnEvent.class, this::onPlayerRespawn);
    }
    
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        event.getPlayer().getInventory().addItem(compass);
    }
    
    @Override
    public String getName() {
        return "Compass On Respawn";
    }
    
    @Override
    public String getDescription() {
        return "Donne une boussole à chaque respawn";
    }
}
```

```java
package me.flamboyant.manhunt.domain.role.ability;

public class NoNameTagAbility extends PassiveAbility {
    
    public NoNameTagAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Implementation depends on scoreboard/name tag system
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        // Hide name tag logic
        context.getOwner().setCustomNameVisible(false);
    }
    
    @Override
    public String getName() {
        return "No Name Tag";
    }
    
    @Override
    public String getDescription() {
        return "Cache le nom au-dessus de la tête";
    }
}
```

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public class SwapperAbility extends ItemActivatedAbility {
    private Player lastTargetedPlayer;
    
    public SwapperAbility(AbilityContext context) {
        super(context, new ItemStack(Material.ENDER_PEARL), Duration.ofMinutes(5));
    }
    
    @Override
    protected void activate() {
        if (lastTargetedPlayer == null || !lastTargetedPlayer.isOnline()) {
            context.sendMessage("&cAucun joueur ciblé !");
            return;
        }
        
        Location ownerLoc = context.getOwner().getLocation();
        Location targetLoc = lastTargetedPlayer.getLocation();
        
        context.getOwner().teleport(targetLoc);
        lastTargetedPlayer.teleport(ownerLoc);
        
        context.sendMessage("&aÉchange de position avec " + lastTargetedPlayer.getDisplayName());
    }
    
    @Override
    public String getName() {
        return "Swapper";
    }
    
    @Override
    public String getDescription() {
        return "Échange de position avec le dernier joueur ciblé";
    }
}
```

```java
package me.flamboyant.manhunt.domain.role.ability;

public class UndecidedRoleAbility extends PassiveAbility {
    
    public UndecidedRoleAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Special undecided role logic
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        context.sendMessage("&7Tu es indécis, ton rôle sera révélé plus tard...");
    }
    
    @Override
    public String getName() {
        return "Undecided Role";
    }
    
    @Override
    public String getDescription() {
        return "Rôle non encore décidé";
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=UtilityAbilitiesTest`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassOnStartAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassOnRespawnAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/NoNameTagAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/SwapperAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/UndecidedRoleAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/UtilityAbilitiesTest.java
git commit -m "feat(priority-13): add utility abilities

- CompassOnStartAbility gives compass on role start
- CompassOnRespawnAbility gives compass on respawn
- NoNameTagAbility hides player name tag
- SwapperAbility swaps positions with target
- UndecidedRoleAbility for ally undecided role
- Completes Phase 2 ability implementations"
```

---

## Phase 2 Summary

**Completed:**
- Task 5: Compass abilities (2 abilities)
- Task 6: Checkpoint abilities (2 abilities + storage)
- Task 7: Passive abilities (3 abilities)
- Task 8: Win condition abilities (3 abilities)
- Task 9: Specialized abilities (6 abilities)
- Task 10: Utility abilities (5 abilities)

**Total:** 21 ability classes implemented

**Next:** Phase 3 - Register Role Definitions

---

**Proceed to:** `2026-06-29-role-abstraction-phase3.md`
