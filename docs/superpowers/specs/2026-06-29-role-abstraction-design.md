# Priority 13: Role Abstraction Improvement - Design Specification

**Priority:** 13 (LOW)  
**Category:** Design - Inheritance  
**Estimated Effort:** 2-3 hours (original) → 12-15 hours (revised for full migration)  
**Status:** Design Complete  
**Date:** 2026-06-29

---

## Problem Statement

The current role implementation suffers from three key issues:

### A. Code Duplication
Each of the 19 role classes contains repeated boilerplate:
- Event filtering (`if (owner != event.getPlayer()) return`)
- Cooldown management (`owner.setCooldown(Material.X, seconds * 20)`)
- Item matching logic (`event.getItem().getType() == Material.X`)
- Compass lodestone handling (duplicated in Speedrunner and Hunter variants)

**Example duplication:**
```java
// Appears in ~8 role classes
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (owner != event.getPlayer()) return;  // Duplicated
    if (!event.hasItem()) return;             // Duplicated
    if (owner.hasCooldown(Material.COMPASS)) return;  // Duplicated
    owner.setCooldown(Material.COMPASS, 30 * 20);      // Duplicated
    // ... actual ability logic (5-10 lines)
}
```

### D. Poor Separation of Concerns
Roles mix three responsibilities:
1. **Domain logic** - What the ability does (checkpoint save, grass drops)
2. **Event handling infrastructure** - Bukkit event registration, filtering
3. **State management** - Cooldowns, saved locations, inventory

This makes it hard to:
- Test ability logic without mocking Bukkit events
- Reuse common patterns across roles
- Understand what a role actually does (buried in event handlers)

### C. Testing Difficulty
Current role tests require:
- Mocking entire Bukkit event system
- Setting up event infrastructure for simple logic tests
- Testing multiple concerns at once (event filtering + cooldowns + logic)

---

## Goals

1. **Eliminate duplication (A)** - Extract common patterns into reusable components
2. **Separate concerns (D)** - Decouple ability logic from event handling infrastructure
3. **Improve testability (C)** - Test abilities in isolation without Bukkit mocks
4. **Maintain compatibility** - All 19 roles work exactly as before
5. **Simplify role creation** - New roles are just configurations, not classes

**Non-Goals:**
- Changing role behavior or game mechanics
- Adding new abilities or roles
- Modifying win conditions or game flow

---

## Design Overview

### Transformation Summary

**Before:**
- 19 role classes with inheritance (SpeedrunnerRole → CheckpointSpeedrunnerRole → etc.)
- ~3000 lines of duplicated boilerplate
- Event handlers scattered across role classes

**After:**
- 1 configurable `Role` class (composition, not inheritance)
- ~10-12 reusable `Ability` classes
- 19 role definitions (pure data/configuration)
- ~2000 lines total (33% reduction)

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Role (Composition)                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ - identifier: ManhuntRoleIdentifier                   │  │
│  │ - roleType: ManhuntRoleType                           │  │
│  │ - name: String                                        │  │
│  │ - description: String                                 │  │
│  │ - abilities: List<Ability>    ◄────────────────┐     │  │
│  │ - abilityManager: AbilityManager               │     │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ delegates to
                          ▼
          ┌───────────────────────────────┐
          │      AbilityManager           │
          │  - routeEvent()               │
          │  - registerAbility()          │
          │  - unregisterAbility()        │
          │  - cooldownTracker            │
          └───────────────────────────────┘
                          │
                          │ manages
                          ▼
        ┌─────────────────────────────────────┐
        │         Ability (Interface)         │
        │  - onRoleStart()                    │
        │  - onRoleStop()                     │
        │  - getName()                        │
        │  - getDescription()                 │
        └─────────────────────────────────────┘
                          △
                          │ implemented by
          ┌───────────────┼───────────────┐
          │               │               │
┌─────────────────┐ ┌─────────────┐ ┌─────────────────┐
│ ItemActivated   │ │  Passive    │ │  WinCondition   │
│    Ability      │ │  Ability    │ │     Ability     │
└─────────────────┘ └─────────────┘ └─────────────────┘
          △               △               △
          │               │               │
    Concrete abilities:
    - CompassTracking (UIPicker, Cycling)
    - CheckpointSave/Rollback
    - GrassDrop, SwordSound
    - DragonWinCondition
    - PortalTracking
    - SpeedrunnerDeath
    - CompassOnStart/Respawn
```

---

## Component Design

### 1. Ability System (Domain Layer)

#### 1.1 Base Ability Interface

```java
package me.flamboyant.manhunt.domain.role.ability;

public interface Ability {
    /**
     * Called when the role starts. Ability registers event handlers here.
     */
    void onRoleStart(AbilityContext context);
    
    /**
     * Called when the role stops. Ability cleans up here.
     */
    void onRoleStop(AbilityContext context);
    
    /**
     * Human-readable ability name.
     */
    String getName();
    
    /**
     * Human-readable ability description.
     */
    String getDescription();
}
```

#### 1.2 AbilityContext

Provides abilities access to dependencies without coupling to infrastructure:

```java
package me.flamboyant.manhunt.domain.role.ability;

public class AbilityContext {
    private final Player owner;
    private final GameSession session;
    private final MessageService messageService;
    private final ItemService itemService;
    private final EventRegistrationService eventRegistration;
    private final Server server;
    private final Plugin plugin;
    private final AbilityManager abilityManager;
    
    // Getters for all dependencies
    public Player getOwner() { return owner; }
    public GameSession getSession() { return session; }
    public MessageService getMessageService() { return messageService; }
    public ItemService getItemService() { return itemService; }
    // ... etc
    
    // Convenience methods
    public void sendMessage(String message) {
        messageService.sendMessage(owner, message);
    }
    
    public void registerEventHandler(Class<? extends Event> eventType, Consumer<Event> handler) {
        eventRegistration.registerEventHandler(eventType, handler, owner);
    }
}
```

#### 1.3 Specialized Ability Types

**ItemActivatedAbility** - For abilities triggered by item use:

```java
public abstract class ItemActivatedAbility implements Ability {
    protected final ItemStack triggerItem;
    protected final Duration cooldown;
    protected final AbilityContext context;
    
    protected ItemActivatedAbility(AbilityContext context, ItemStack triggerItem, Duration cooldown) {
        this.context = context;
        this.triggerItem = triggerItem;
        this.cooldown = cooldown;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        context.registerEventHandler(PlayerInteractEvent.class, this::handleInteract);
    }
    
    private void handleInteract(Event event) {
        PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
        
        // Boilerplate handled once here
        if (!interactEvent.hasItem()) return;
        if (!context.getItemService().isSameItemKind(interactEvent.getItem(), triggerItem)) return;
        if (context.getAbilityManager().isOnCooldown(context.getOwner(), getName())) return;
        
        // Call subclass logic
        activate();
        
        // Apply cooldown
        context.getAbilityManager().setCooldown(context.getOwner(), getName(), cooldown);
    }
    
    /**
     * Subclasses implement their specific activation logic here.
     */
    protected abstract void activate();
}
```

**PassiveAbility** - For always-on effects:

```java
public abstract class PassiveAbility implements Ability {
    protected final AbilityContext context;
    
    protected PassiveAbility(AbilityContext context) {
        this.context = context;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        registerEventHandlers(context);
    }
    
    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
    }
    
    /**
     * Subclasses register their specific event handlers.
     */
    protected abstract void registerEventHandlers(AbilityContext context);
}
```

**WinConditionAbility** - For abilities affecting win conditions:

```java
public abstract class WinConditionAbility implements Ability {
    protected final AbilityContext context;
    
    protected WinConditionAbility(AbilityContext context) {
        this.context = context;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        registerWinConditionHandlers(context);
    }
    
    /**
     * Subclasses define win condition logic.
     */
    protected abstract void registerWinConditionHandlers(AbilityContext context);
}
```

#### 1.4 Concrete Ability Examples

**CompassTrackingAbility** (with variants):

```java
// Base class for compass abilities
public abstract class CompassAbility extends ItemActivatedAbility {
    protected CompassAbility(AbilityContext context, Duration cooldown) {
        super(context, new ItemStack(Material.COMPASS), cooldown);
    }
    
    @Override
    protected void activate() {
        Player target = selectTarget();
        if (target == null) return;
        
        updateCompass(target);
    }
    
    protected abstract Player selectTarget();
    
    protected void updateCompass(Player target) {
        CompassTarget compassTarget = calculateCompassTarget(target);
        
        if (compassTarget.isCrossDimension()) {
            context.sendMessage(target.getDisplayName() + " est dans la dimension " 
                + compassTarget.getTargetDimensionName());
        }
        
        applyCompassPointing(compassTarget);
    }
    
    // Shared compass logic extracted from old roles
    private void applyCompassPointing(CompassTarget compassTarget) {
        // Lodestone vs vanilla compass logic
    }
}

// Speedrunner variant - UI picker
public class UIPickerCompassAbility extends CompassAbility {
    private PlayerSelectionView trackView;
    
    public UIPickerCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
    }
    
    @Override
    protected Player selectTarget() {
        // Show UI, return selected player
    }
}

// Hunter variant - cycles through speedrunners
public class CyclingCompassAbility extends CompassAbility {
    private List<Player> speedrunners;
    private int targetIndex = 0;
    
    public CyclingCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        // Build speedrunner list from session
        speedrunners = context.getSession().getPlayers().stream()
            .filter(p -> context.getSession().getRole(p).getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .collect(Collectors.toList());
    }
    
    @Override
    protected Player selectTarget() {
        if (++targetIndex >= speedrunners.size()) {
            targetIndex = 0;
        }
        return speedrunners.get(targetIndex);
    }
}
```

**CheckpointAbility** (save and rollback):

```java
public class CheckpointSaveAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage; // Shared state
    
    public CheckpointSaveAbility(AbilityContext context, ItemStack triggerItem, 
                                  Duration cooldown, CheckpointStorage storage) {
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

public class CheckpointRollbackAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage; // Shared state
    
    public CheckpointRollbackAbility(AbilityContext context, ItemStack triggerItem, 
                                      Duration cooldown, CheckpointStorage storage) {
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
        checkpoint.getInventory().forEach(item -> owner.getInventory().addItem(item));
        
        owner.getActivePotionEffects().forEach(effect -> owner.removePotionEffect(effect.getType()));
        checkpoint.getEffects().forEach(effect -> owner.addPotionEffect(effect));
        
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

// Shared state between save/rollback abilities
class CheckpointStorage {
    private Checkpoint checkpoint;
    
    void saveCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }
    
    Checkpoint getCheckpoint() {
        return checkpoint;
    }
}
```

**GrassDropAbility** (passive):

```java
public class GrassDropAbility extends PassiveAbility {
    private static final List<Material> GRASSES = Arrays.asList(
        Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, 
        Material.TALL_SEAGRASS, Material.WARPED_ROOTS, 
        Material.NETHER_SPROUTS, Material.CRIMSON_ROOTS
    );
    private final Random rng = new Random();
    
    public GrassDropAbility(AbilityContext context) {
        super(context);
    }
    
    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }
    
    private void onBlockBreak(Event event) {
        BlockBreakEvent breakEvent = (BlockBreakEvent) event;
        if (!GRASSES.contains(breakEvent.getBlock().getType())) return;
        
        breakEvent.setDropItems(false);
        int roll = rng.nextInt(100);
        Location dropLocation = breakEvent.getBlock().getLocation();
        
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

**Full Ability List** (10-12 total):

1. `UIPickerCompassAbility` - Speedrunner compass with UI
2. `CyclingCompassAbility` - Hunter compass that cycles
3. `CheckpointSaveAbility` - Save checkpoint
4. `CheckpointRollbackAbility` - Rollback to checkpoint
5. `GrassDropAbility` - Link's grass emerald drops
6. `SwordSoundAbility` - Link's sword sound
7. `CutCleanAbility` - Auto-smelt drops
8. `DragonWinConditionAbility` - Speedrunner wins on dragon death
9. `SpeedrunnerDeathAbility` - Speedrunner death handling
10. `PortalTrackingAbility` - Track portal entries
11. `CompassOnStartAbility` - Give compass on role start
12. `CompassOnRespawnAbility` - Give compass on respawn
13. `SuperHunterWinModifierAbility` - Modify hunter win condition
14. `WerewolfNightStrengthAbility` - Night strength boost
15. `TntTacticalAbility` - TNT ability
16. `ElfBonusEffectAbility` - Elf bonus effects
17. `ProMinerAbility` - Pro miner effects
18. `GluerSlownessAbility` - Gluer slowness effect
19. `ImposterDeceptionAbility` - Imposter deception

---

### 2. Role Class (Domain Layer)

Simplified role class using composition:

```java
package me.flamboyant.manhunt.domain.role.behavior;

public class Role extends AManhuntRole implements Listener {
    private final ManhuntRoleIdentifier identifier;
    private final ManhuntRoleType roleType;
    private final String name;
    private final String description;
    private final List<Ability> abilities;
    private final AbilityManager abilityManager;
    private final AbilityContext context;
    
    @Inject
    public Role(
        @Assisted Player owner,
        @Assisted RoleDefinition definition,
        AbilityManager abilityManager,
        GameSessionManager sessionManager,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration,
        Server server,
        Plugin plugin
    ) {
        super(owner);
        this.identifier = definition.getIdentifier();
        this.roleType = definition.getRoleType();
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.abilityManager = abilityManager;
        
        // Build context
        this.context = new AbilityContext(
            owner, 
            sessionManager, 
            messageService, 
            itemService, 
            eventRegistration,
            server,
            plugin,
            abilityManager
        );
        
        // Create abilities
        this.abilities = definition.createAbilities(context);
    }
    
    @Override
    protected boolean doStart() {
        GameSession session = context.getSessionManager().getActiveSessionForPlayer(owner);
        if (session == null) {
            return false;
        }
        context.setSession(session);
        
        // Start all abilities
        abilities.forEach(ability -> {
            try {
                abilityManager.registerAbility(ability, context);
            } catch (Exception e) {
                context.getLogger().warning("Failed to start ability " + ability.getName() + ": " + e.getMessage());
            }
        });
        
        return true;
    }
    
    @Override
    protected boolean doStop() {
        // Stop all abilities
        abilities.forEach(ability -> {
            try {
                abilityManager.unregisterAbility(ability, context);
            } catch (Exception e) {
                context.getLogger().warning("Failed to stop ability " + ability.getName() + ": " + e.getMessage());
            }
        });
        
        return true;
    }
    
    @Override
    protected void broadcastPlayerResultMessage() {
        // Default message - can be overridden by abilities
        boolean won = determineWinStatus();
        context.getMessageService().broadcastMessage(
            "&6" + owner.getDisplayName() + ", qui était " + name + " a " 
            + (won ? "gagné" : "perdu") + " !"
        );
    }
    
    private boolean determineWinStatus() {
        // Check win condition via evaluator
        GameSession session = context.getSession();
        if (session == null) return false;
        
        // Default logic - can be enhanced by WinConditionAbility instances
        if (roleType == ManhuntRoleType.SPEEDRUNNER) {
            return session.getRemainingSpeedrunners() > 0;
        } else {
            return session.getRemainingSpeedrunners() == 0;
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    protected String getDescription() {
        return description;
    }
    
    @Override
    public ManhuntRoleType getRoleType() {
        return roleType;
    }
    
    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return identifier;
    }
    
    // Bukkit event handlers - route to ability manager
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if ((Player) event.getEntity() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
}
```

**Key Design Points:**

1. **Single responsibility** - Role is just a container and event router
2. **Event filtering once** - `if (event.getPlayer() != owner)` handled in Role, not scattered across abilities
3. **Error resilience** - Ability failures don't crash the role
4. **Clean lifecycle** - Abilities started/stopped in order

---

### 3. Ability Manager (Domain Layer)

Coordinates ability lifecycle and event routing:

```java
package me.flamboyant.manhunt.domain.role.ability;

public class AbilityManager {
    private final Map<Player, Map<Class<? extends Event>, List<EventHandler>>> eventHandlers;
    private final CooldownTracker cooldownTracker;
    
    @Inject
    public AbilityManager(CooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
        this.eventHandlers = new ConcurrentHashMap<>();
    }
    
    /**
     * Register an ability's event handlers for a specific player.
     */
    public void registerAbility(Ability ability, AbilityContext context) {
        ability.onRoleStart(context);
    }
    
    /**
     * Unregister an ability's event handlers for a specific player.
     */
    public void unregisterAbility(Ability ability, AbilityContext context) {
        ability.onRoleStop(context);
        // Event cleanup handled by EventRegistrationService
    }
    
    /**
     * Route a Bukkit event to registered ability handlers.
     */
    public void routeEvent(Event event, Player owner) {
        Map<Class<? extends Event>, List<EventHandler>> playerHandlers = eventHandlers.get(owner);
        if (playerHandlers == null) return;
        
        List<EventHandler> handlers = playerHandlers.get(event.getClass());
        if (handlers == null) return;
        
        handlers.forEach(handler -> {
            try {
                handler.handle(event);
            } catch (Exception e) {
                // Log but don't crash
                Bukkit.getLogger().warning("Event handler failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register an event handler for a player.
     */
    public void registerEventHandler(Player owner, Class<? extends Event> eventType, EventHandler handler) {
        eventHandlers
            .computeIfAbsent(owner, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(handler);
    }
    
    /**
     * Clear all event handlers for a player.
     */
    public void clearEventHandlers(Player owner) {
        eventHandlers.remove(owner);
    }
    
    // Cooldown delegation
    public boolean isOnCooldown(Player player, String abilityId) {
        return cooldownTracker.isOnCooldown(player, abilityId);
    }
    
    public void setCooldown(Player player, String abilityId, Duration duration) {
        cooldownTracker.setCooldown(player, abilityId, duration);
    }
    
    public void clearCooldown(Player player, String abilityId) {
        cooldownTracker.clearCooldown(player, abilityId);
    }
    
    @FunctionalInterface
    public interface EventHandler {
        void handle(Event event);
    }
}
```

**CooldownTracker:**

```java
package me.flamboyant.manhunt.domain.role.ability;

public class CooldownTracker {
    private final Map<Player, Map<String, Instant>> cooldowns;
    
    public CooldownTracker() {
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    public boolean isOnCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return false;
        
        Instant cooldownEnd = playerCooldowns.get(abilityId);
        if (cooldownEnd == null) return false;
        
        return Instant.now().isBefore(cooldownEnd);
    }
    
    public void setCooldown(Player player, String abilityId, Duration duration) {
        Instant cooldownEnd = Instant.now().plus(duration);
        cooldowns
            .computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(abilityId, cooldownEnd);
    }
    
    public void clearCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns != null) {
            playerCooldowns.remove(abilityId);
        }
    }
    
    public void clearAllCooldowns(Player player) {
        cooldowns.remove(player);
    }
}
```

---

### 4. Role Definitions (Configuration)

**RoleDefinition Value Object:**

```java
package me.flamboyant.manhunt.domain.role.definition;

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
    
    // Getters
    public ManhuntRoleIdentifier getIdentifier() { return identifier; }
    public ManhuntRoleType getRoleType() { return roleType; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    
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

@FunctionalInterface
public interface AbilityFactory {
    Ability create(AbilityContext context);
}
```

**RoleDefinitionRegistry:**

```java
package me.flamboyant.manhunt.domain.role.definition;

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

**Example Role Configurations:**

```java
// In ManhuntModule or separate configuration class
@Provides
@Singleton
public RoleDefinitionRegistry provideRoleDefinitionRegistry() {
    RoleDefinitionRegistry registry = new RoleDefinitionRegistry();
    
    // ===== SPEEDRUNNER VARIANTS =====
    
    // Simple Speedrunner
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
    
    // Checkpoint Speedrunner
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
    
    // Link Speedrunner
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
    
    // ... (similar for other 16 roles)
    
    // ===== HUNTER VARIANTS =====
    
    // Simple Hunter
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
    
    // ... (similar for other hunter variants)
    
    return registry;
}
```

---

## Error Handling

### Exception Handling Policies

1. **Ability startup failure**
   - Log warning
   - Skip that ability
   - Role still starts with remaining abilities
   - **Why:** One broken ability shouldn't prevent the entire role from working

2. **Event handler failure**
   - Log error with stack trace
   - Continue processing other events
   - **Why:** Event failures shouldn't cascade to other abilities

3. **Cooldown failure**
   - Default to allowing activation (fail-open)
   - Log warning
   - **Why:** Better to allow ability than block player permanently

4. **Missing dependency**
   - Fail fast at role creation time (fail-closed)
   - Throw IllegalStateException
   - **Why:** Missing services indicate configuration error, not runtime error

### Error Recovery Examples

```java
// In Role.doStart()
abilities.forEach(ability -> {
    try {
        abilityManager.registerAbility(ability, context);
    } catch (Exception e) {
        // Log but continue with other abilities
        context.getLogger().warning(
            "Failed to start ability " + ability.getName() + 
            " for role " + name + ": " + e.getMessage()
        );
    }
});

// In AbilityManager.routeEvent()
handlers.forEach(handler -> {
    try {
        handler.handle(event);
    } catch (Exception e) {
        // Log but don't crash
        Bukkit.getLogger().warning(
            "Event handler failed for " + event.getClass().getSimpleName() + 
            ": " + e.getMessage()
        );
        e.printStackTrace();
    }
});
```

---

## Testing Strategy

### Test Pyramid

1. **Unit Tests - Ability Classes** (~50-60 tests)
   - Test each ability in isolation
   - Mock AbilityContext and services
   - Verify activation conditions, cooldowns, effects

2. **Integration Tests - Role Composition** (~20 tests)
   - Test roles with multiple abilities
   - Verify abilities don't conflict
   - Test ability lifecycle (start/stop)

3. **Migration Tests - Backwards Compatibility** (~19 tests)
   - One test per role identifier
   - Verify new Role behaves like old role classes

### Unit Test Examples

**CompassTrackingAbilityTest:**

```java
class UIPickerCompassAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private GameSession mockSession;
    private UIPickerCompassAbility ability;
    
    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        mockSession = mock(GameSession.class);
        
        when(mockContext.getOwner()).thenReturn(mockOwner);
        when(mockContext.getSession()).thenReturn(mockSession);
        
        ability = new UIPickerCompassAbility(mockContext, Duration.ofMinutes(15));
    }
    
    @Test
    void activateOpensPlayerSelectionUI() {
        // Arrange
        List<Player> hunters = Arrays.asList(mock(Player.class), mock(Player.class));
        when(mockSession.getPlayers()).thenReturn(hunters);
        
        // Act
        ability.activate();
        
        // Assert
        verify(mockOwner).openInventory(any());
    }
    
    @Test
    void cooldownPreventsActivation() {
        // Arrange
        AbilityManager mockManager = mock(AbilityManager.class);
        when(mockContext.getAbilityManager()).thenReturn(mockManager);
        when(mockManager.isOnCooldown(mockOwner, ability.getName())).thenReturn(true);
        
        // Act
        boolean activated = ability.tryActivate(mockContext);
        
        // Assert
        assertFalse(activated);
    }
}
```

**CheckpointAbilityTest:**

```java
class CheckpointSaveAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private CheckpointStorage storage;
    private ItemStack checkpointItem;
    
    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        storage = new CheckpointStorage();
        checkpointItem = new ItemStack(Material.RECOVERY_COMPASS);
        
        when(mockContext.getOwner()).thenReturn(mockOwner);
        when(mockOwner.getLocation()).thenReturn(new Location(null, 100, 64, 200));
        when(mockOwner.getHealth()).thenReturn(15.0);
        when(mockOwner.getFoodLevel()).thenReturn(18);
    }
    
    @Test
    void saveCheckpointStoresPlayerState() {
        // Arrange
        CheckpointSaveAbility ability = new CheckpointSaveAbility(
            mockContext, checkpointItem, Duration.ofSeconds(15), storage
        );
        
        // Act
        ability.activate();
        
        // Assert
        assertNotNull(storage.getCheckpoint());
        assertEquals(15.0, storage.getCheckpoint().getHealth());
        assertEquals(18, storage.getCheckpoint().getFoodLevel());
    }
    
    @Test
    void rollbackRestoresPlayerState() {
        // Arrange - save checkpoint first
        CheckpointSaveAbility saveAbility = new CheckpointSaveAbility(
            mockContext, checkpointItem, Duration.ofSeconds(15), storage
        );
        saveAbility.activate();
        
        // Change player state
        when(mockOwner.getHealth()).thenReturn(5.0);
        when(mockOwner.getFoodLevel()).thenReturn(10);
        
        CheckpointRollbackAbility rollbackAbility = new CheckpointRollbackAbility(
            mockContext, checkpointItem, Duration.ofSeconds(15), storage
        );
        
        // Act
        rollbackAbility.activate();
        
        // Assert
        verify(mockOwner).setHealth(15.0);
        verify(mockOwner).setFoodLevel(18);
        verify(mockOwner).teleport(any(Location.class));
    }
}
```

### Integration Test Example

**CheckpointSpeedrunnerRoleIntegrationTest:**

```java
class CheckpointSpeedrunnerRoleIntegrationTest {
    private RoleDefinitionRegistry registry;
    private AbilityManager abilityManager;
    
    @BeforeEach
    void setUp() {
        registry = new RoleDefinitionRegistry();
        // Register checkpoint speedrunner definition
        registerCheckpointSpeedrunner(registry);
        
        abilityManager = new AbilityManager(new CooldownTracker());
    }
    
    @Test
    void checkpointSpeedrunnerHasAllExpectedAbilities() {
        // Arrange
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_CHECKPOINT);
        AbilityContext mockContext = mock(AbilityContext.class);
        
        // Act
        List<Ability> abilities = definition.createAbilities(mockContext);
        
        // Assert
        assertEquals(6, abilities.size());
        assertTrue(hasAbilityOfType(abilities, UIPickerCompassAbility.class));
        assertTrue(hasAbilityOfType(abilities, CheckpointSaveAbility.class));
        assertTrue(hasAbilityOfType(abilities, CheckpointRollbackAbility.class));
        assertTrue(hasAbilityOfType(abilities, DragonWinConditionAbility.class));
    }
    
    @Test
    void abilitiesDoNotConflict() {
        // Arrange
        Player mockPlayer = mock(Player.class);
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_CHECKPOINT);
        Role role = createRole(mockPlayer, definition);
        
        // Act - start role (activates all abilities)
        boolean started = role.start();
        
        // Assert
        assertTrue(started);
        // Verify no exceptions thrown, all abilities registered
    }
}
```

### Migration Test Example

**RoleMigrationTest:**

```java
class RoleMigrationTest {
    /**
     * Verify that SPEEDRUNNER_SIMPLE has the same abilities as the old SpeedrunnerRole.
     */
    @Test
    void speedrunnerSimpleEquivalence() {
        // Arrange
        RoleDefinitionRegistry registry = createRegistryWithAllRoles();
        RoleDefinition definition = registry.getDefinition(SPEEDRUNNER_SIMPLE);
        AbilityContext mockContext = mock(AbilityContext.class);
        
        // Act
        List<Ability> abilities = definition.createAbilities(mockContext);
        
        // Assert - verify expected abilities present
        assertTrue(hasAbilityOfType(abilities, UIPickerCompassAbility.class));
        assertTrue(hasAbilityOfType(abilities, DragonWinConditionAbility.class));
        assertTrue(hasAbilityOfType(abilities, PortalTrackingAbility.class));
        assertTrue(hasAbilityOfType(abilities, SpeedrunnerDeathAbility.class));
        assertEquals(4, abilities.size()); // No extra abilities
    }
    
    /**
     * Verify all 19 roles have definitions.
     */
    @Test
    void allRolesHaveDefinitions() {
        RoleDefinitionRegistry registry = createRegistryWithAllRoles();
        
        for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
            assertTrue(
                registry.hasDefinition(identifier),
                "Missing definition for " + identifier
            );
        }
    }
}
```

---

## Migration Strategy

### Four-Phase Migration

**Phase 1: Build Ability Framework** (3-4 hours)
- Create Ability interfaces and base classes
- Implement AbilityManager and CooldownTracker
- Create new Role class
- Add RoleDefinitionRegistry
- Add comprehensive unit tests
- **Deliverable:** Framework ready, no impact on existing roles

**Phase 2: Implement Concrete Abilities** (4-5 hours)
- Extract abilities from existing role classes:
  - CompassTrackingAbility (UIPicker, Cycling variants)
  - DragonWinConditionAbility
  - SpeedrunnerDeathAbility
  - PortalTrackingAbility
  - CheckpointAbility (save/rollback)
  - GrassDropAbility
  - SwordSoundAbility
  - CutCleanAbility
  - CompassOnStartAbility
  - CompassOnRespawnAbility
  - WerewolfNightStrengthAbility
  - TntTacticalAbility
  - ElfBonusEffectAbility
  - ProMinerAbility
  - GluerSlownessAbility
  - ImposterDeceptionAbility
  - SuperHunterWinModifierAbility
  - UndecidedRoleAbility
- Unit test each ability
- **Deliverable:** ~17-19 tested ability classes, no impact on existing roles

**Phase 3: Register Role Definitions** (2-3 hours)
- Create RoleDefinition for all 19 roles
- Register in RoleDefinitionRegistry provider method
- Update AssistedRoleFactory interface to create new Role class
- Write migration tests
- **Deliverable:** Parallel system ready, old classes still present

**Phase 4: Cutover & Delete** (2-3 hours)
- Update ManhuntModule to wire RoleDefinitionRegistry
- Switch AssistedRoleFactory to create Role instead of old role classes
- Run full test suite (unit + integration + migration tests)
- Delete old role classes:
  - AManhuntRole (keep as abstract base for Role)
  - SpeedrunnerRole
  - HunterRole
  - All 17 role variant classes
- Delete old role tests
- Update documentation
- **Deliverable:** Clean cutover, ~1000 lines of code deleted

### Rollback Strategy

If issues arise during Phase 4:
1. Revert the cutover commit
2. Old role classes still exist in git history
3. Switch AssistedRoleFactory back to old implementation
4. System returns to pre-migration state

### Git Commit Strategy

```
feat(priority-13): add ability framework (Phase 1)
  - Ability interfaces, AbilityManager, CooldownTracker
  - New Role class skeleton
  - RoleDefinitionRegistry
  
feat(priority-13): implement concrete abilities (Phase 2)
  - 17-19 ability implementations
  - Unit tests for all abilities
  
feat(priority-13): register role definitions (Phase 3)
  - RoleDefinition for all 19 roles
  - Migration tests
  
feat(priority-13): cutover to ability system (Phase 4)
  - Switch to new Role class
  - Delete old role classes
  - Update documentation
```

### Affected Components

**Files Deleted** (~19 files):
- `SpeedrunnerRole.java`
- `HunterRole.java`
- `CheckpointSpeedrunnerRole.java`
- `LinkSpeedrunnerRole.java`
- `TntTacticalSpeedrunnerRole.java`
- `WerewolfSpeedrunnerRole.java`
- `ElfSpeedrunnerRole.java`
- `CutCleanSpeedrunnerRole.java`
- `NoNameTagSpeedrunnerRole.java`
- `SpeedrunnerSwapperRole.java`
- `CheckpointHunterRole.java`
- `ProMinerRole.java`
- `SuperHunterRole.java`
- `ElfHunterRole.java`
- `LinkHunterRole.java`
- `CutCleanHunterRole.java`
- `GluerRole.java`
- `ImposterRole.java`
- `UndecidedRole.java`
- Associated test files (~15 files)

**Files Created** (~30 files):
- Ability interfaces (5 files)
- Concrete abilities (~17 files)
- AbilityManager, CooldownTracker (2 files)
- RoleDefinition, RoleDefinitionRegistry (2 files)
- Role class (1 file)
- Test files (~60 tests across ~20 files)

**Files Modified** (~5 files):
- `AManhuntRole.java` - Keep as abstract base
- `AssistedRoleFactory.java` - Change signature
- `ManhuntModule.java` - Wire new components
- `RoleRegistry.java` - May need updates
- Documentation files

**Net Change:**
- **Before:** ~3000 lines of role code + tests
- **After:** ~2000 lines of ability code + tests
- **Reduction:** ~33% less code, much less duplication

### Data/State Preservation

**No breaking changes to:**
- GameSession (still stores `Player → Role` mapping, just Role is different class)
- ManhuntRoleIdentifier enum (unchanged)
- ManhuntRoleType enum (unchanged)
- WinConditionEvaluator (abilities integrate via same interfaces)
- GameLifecycleService (event lifecycle unchanged)
- Event listener management (handled by EventRegistrationService)

**Behavioral preservation:**
- All 19 roles behave identically to before
- Win conditions unchanged
- Cooldowns same duration
- Event handling same
- Player messages unchanged

---

## Success Criteria

### Functional Requirements
- ✅ All 19 roles work exactly as before
- ✅ Win conditions unchanged
- ✅ Cooldowns work correctly
- ✅ Event handling preserved
- ✅ No regressions in game behavior

### Code Quality Requirements
- ✅ No event filtering boilerplate in ability classes
- ✅ Cooldowns managed centrally, not scattered
- ✅ No manual event registration/cleanup in roles
- ✅ Abilities testable without Bukkit mocks
- ✅ ~33% code reduction (3000 lines → 2000 lines)

### Testing Requirements
- ✅ 50-60 unit tests for abilities
- ✅ 20 integration tests for role composition
- ✅ 19 migration tests (one per role)
- ✅ All tests pass
- ✅ Test coverage >80% for ability classes

### Documentation Requirements
- ✅ Design spec written (this document)
- ✅ Implementation plan created
- ✅ Architecture docs updated
- ✅ Migration guide for future role additions

---

## Future Enhancements (Out of Scope)

These are explicitly NOT part of Priority 13, but could be follow-up work:

1. **Dynamic ability loading** - Load abilities from configuration files
2. **Ability hot-reload** - Reload abilities without server restart
3. **Ability marketplace** - Plugin API for third-party abilities
4. **Ability chains** - Combine abilities with AND/OR logic
5. **Ability conditions** - Context-aware ability activation (time, location, team)
6. **Ability cooldown UI** - Boss bar or action bar for cooldown visualization
7. **Ability analytics** - Track ability usage statistics

---

## Appendix A: Complete Ability List

| Ability Class | Type | Used By | Description |
|--------------|------|---------|-------------|
| UIPickerCompassAbility | ItemActivated | All speedrunner variants | Compass with UI picker for target |
| CyclingCompassAbility | ItemActivated | All hunter variants | Compass that cycles through speedrunners |
| CheckpointSaveAbility | ItemActivated | SPEEDRUNNER_CHECKPOINT | Save checkpoint |
| CheckpointRollbackAbility | ItemActivated | SPEEDRUNNER_CHECKPOINT | Rollback to checkpoint |
| GrassDropAbility | Passive | SPEEDRUNNER_LINK, HUNTER_LINK | Grass drops emeralds |
| SwordSoundAbility | Passive | SPEEDRUNNER_LINK, HUNTER_LINK | Sword makes villager sound |
| CutCleanAbility | Passive | SPEEDRUNNER_CUTCLEAN, HUNTER_CUTCLEAN | Auto-smelt drops |
| DragonWinConditionAbility | WinCondition | All speedrunner variants | Win when dragon dies |
| SpeedrunnerDeathAbility | WinCondition | All speedrunner variants | Handle speedrunner death |
| PortalTrackingAbility | Passive | All speedrunner variants | Track portal entries |
| CompassOnStartAbility | ItemActivated | All hunter variants | Give compass on start |
| CompassOnRespawnAbility | Passive | All hunter variants | Give compass on respawn |
| SuperHunterWinModifierAbility | WinCondition | HUNTER_SUPER | Modify hunter win condition |
| WerewolfNightStrengthAbility | Passive | SPEEDRUNNER_WEREWOLF | Night strength boost |
| TntTacticalAbility | ItemActivated | SPEEDRUNNER_TNT_TACTICAL | TNT ability |
| ElfBonusEffectAbility | Passive | SPEEDRUNNER_ELF, HUNTER_ELF | Bonus effects |
| ProMinerAbility | Passive | HUNTER_PRO_MINER | Mining bonuses |
| GluerSlownessAbility | Passive | HUNTER_GLUER | Apply slowness |
| ImposterDeceptionAbility | Passive | HUNTER_IMPOSTER | Deception mechanics |
| UndecidedRoleAbility | Special | ALLY_UNDECIDED | Undecided role logic |
| NoNameTagAbility | Passive | SPEEDRUNNER_NO_NAME_TAG | Hide name tag |
| SwapperAbility | ItemActivated | SPEEDRUNNER_SWAPPER | Swap positions |

---

## Appendix B: Role Definition Mapping

| Role Identifier | Abilities |
|----------------|-----------|
| SPEEDRUNNER_SIMPLE | UIPickerCompass, DragonWinCondition, PortalTracking, SpeedrunnerDeath |
| SPEEDRUNNER_CHECKPOINT | + CheckpointSave, CheckpointRollback |
| SPEEDRUNNER_LINK | + GrassDrop, SwordSound |
| SPEEDRUNNER_TNT_TACTICAL | + TntTactical |
| SPEEDRUNNER_WEREWOLF | + WerewolfNightStrength |
| SPEEDRUNNER_ELF | + ElfBonusEffect |
| SPEEDRUNNER_CUTCLEAN | + CutClean |
| SPEEDRUNNER_NO_NAME_TAG | + NoNameTag |
| SPEEDRUNNER_SWAPPER | + Swapper |
| HUNTER_SIMPLE | CyclingCompass, CompassOnStart, CompassOnRespawn |
| HUNTER_CHECKPOINT | + CheckpointSave, CheckpointRollback |
| HUNTER_PRO_MINER | + ProMiner |
| HUNTER_SUPER | + SuperHunterWinModifier |
| HUNTER_ELF | + ElfBonusEffect |
| HUNTER_LINK | + GrassDrop, SwordSound |
| HUNTER_CUTCLEAN | + CutClean |
| HUNTER_GLUER | + GluerSlowness |
| HUNTER_IMPOSTER | + ImposterDeception |
| ALLY_UNDECIDED | UndecidedRole |

---

## Design Sign-Off

**Design Approved By:** [User]  
**Date:** 2026-06-29  
**Next Step:** Write implementation plan via writing-plans skill

---

**End of Design Specification**
