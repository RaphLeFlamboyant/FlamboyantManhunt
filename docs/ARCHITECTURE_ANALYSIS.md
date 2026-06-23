# Manhunt Plugin - Architecture Analysis & Recommendations

## Executive Summary

This Minecraft Spigot plugin implements a "Manhunt" game mode with 18 role variants across 4 role types. The codebase is functional but has maintainability issues due to tight coupling, lack of modularity, and reliance on string-based role identification. This analysis provides concrete recommendations to improve maintainability and accelerate feature development.

---

## Current Architecture Overview

### Package Structure
```
me.flamboyant.manhunt/
├── Main.java                    # Plugin entry point
├── CommandsDispatcher.java      # Command handler
├── GameData.java               # Global static state
├── NewManhuntLauncher.java     # Game configuration & startup
├── NewManhuntManager.java      # Game lifecycle & event handling
├── roles/
│   ├── AManhuntRole.java       # Abstract role base
│   ├── ManhuntRoleType.java    # SPEEDRUNNER, HUNTER, ALLY, NEUTRAL
│   ├── ManhuntRoleIdentifier.java  # Enum of 18 role variants
│   ├── ManhuntRoleFactory.java # Giant switch statement
│   ├── GameRolesManagement.java # Role distribution logic
│   ├── IHunterWinConditionModifier.java
│   └── impl/
│       └── [18 role implementation classes]
└── views/
    └── PlayerSelectionView.java
```

### Key Components

**1. GameData (Global State Container)**
- Static HashMaps storing player-role mappings and portal locations
- No encapsulation, accessed directly throughout codebase
- Makes testing difficult and creates hidden dependencies

**2. Role Hierarchy**
- Base: `AManhuntRole` (abstract)
- Two main concrete classes: `HunterRole`, `SpeedrunnerRole`
- 16 variants extend these two classes
- Uses Bukkit event listener registration per role

**3. ManhuntRoleFactory**
- 47-line switch statement mapping identifiers to role instances
- Tightly couples role creation to specific implementations

**4. GameRolesManagement**
- Complex role distribution algorithm
- String-based role type detection (`roleId.toString().contains("SPEEDRUNNER")`)
- Random role assignment with special role logic

---

## Critical Issues Identified

### 1. **Global Mutable State (SEVERITY: HIGH)**

**Problem:**
```java
public class GameData {
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    public static HashMap<Player, Location> overworldLocationBeforePortal = new HashMap<>();
    public static HashMap<Player, Location> netherLocationBeforePortal = new HashMap<>();
    public static int remainingSpeedrunner;
}
```

**Issues:**
- No encapsulation - any class can modify critical game state
- Makes concurrent games impossible (single static state)
- Hidden dependencies throughout codebase
- Testing requires global state cleanup
- Race conditions if async operations are added

**Impact:** Makes it difficult to run multiple game instances, increases bug surface area, complicates testing.

---

### 2. **String-Based Role Type Detection (SEVERITY: HIGH)**

**Problem:**
```java
// In GameRolesManagement.java
if (roleId.toString().contains("SPEEDRUNNER"))
if (roleId.toString().contains("ALLY"))
if (roleId.toString().contains("HUNTER"))

// In ManhuntRoleIdentifier.java
// Bad design but f*ck : Names must start with corresponding ManhuntRoleType
HUNTER_SIMPLE, SPEEDRUNNER_SIMPLE, ALLY_IMPOSTER, NEUTRAL_GLUER
```

**Issues:**
- Fragile: renaming a role breaks logic
- No compile-time safety
- Scattered string comparisons
- Comment acknowledges it's "bad design"
- Forces naming conventions

**Impact:** High risk of bugs when adding/renaming roles, makes refactoring dangerous.

---

### 3. **Monolithic Factory (SEVERITY: MEDIUM)**

**Problem:**
```java
public static AManhuntRole createRole(Player owner, ManhuntRoleIdentifier roleIdentifier) {
    switch (roleIdentifier) {
        case SPEEDRUNNER_SIMPLE: return new SpeedrunnerRole(owner);
        case SPEEDRUNNER_SWAPPER: return new SpeedrunnerSwapperRole(owner);
        // ... 16 more cases
    }
}
```

**Issues:**
- Every new role requires modifying factory
- Violates Open/Closed Principle
- No extension mechanism
- Factory grows linearly with role count

**Impact:** Adding roles requires touching central factory class, increases merge conflicts.

---

### 4. **Mixed Responsibilities (SEVERITY: MEDIUM)**

**Problem:**
- `NewManhuntLauncher` handles: configuration UI, parameter management, game startup, dragon death detection, player state reset
- 200 lines mixing concerns
- `NewManhuntManager` handles: game lifecycle, death detection, speedrunner counting
- Hard to understand what each class owns

**Impact:** Classes are hard to modify, changes ripple unexpectedly, testing is difficult.

---

### 5. **Event Listener Management Chaos (SEVERITY: MEDIUM)**

**Problem:**
```java
// Roles manually register/unregister
Common.server.getPluginManager().registerEvents(this, Common.plugin);
PlayerInteractEvent.getHandlerList().unregister(this);

// Different patterns across roles
// Some roles forget to unregister
// Static listener lists in HunterRole
public static List<IHunterWinConditionModifier> winconModifiers = new ArrayList<>();
```

**Issues:**
- Inconsistent registration patterns
- Manual unregister prone to leaks
- Static state in role classes
- No centralized listener lifecycle

**Impact:** Memory leaks, event handler accumulation, bugs when stopping/starting games.

---

### 6. **Complex Role Distribution Algorithm (SEVERITY: LOW-MEDIUM)**

**Problem:**
- 174-line `GameRolesManagement` class with complex logic
- Randomization mixed with business rules
- Hard-coded probabilities
- "Berzerk mode" when role counts don't match
- Mutates input parameters

**Impact:** Hard to test, difficult to understand role assignment rules, hard to balance.

---

### 7. **Tight Coupling to External Framework (SEVERITY: LOW)**

**Problem:**
```java
import me.flamboyant.FlamboyantPlugin;
import me.flamboyant.gui.ConfigurablePluginListener;
import me.flamboyant.utils.*;
import me.flamboyant.configurable.parameters.*;
```

**Issues:**
- Depends on external submodule `FlamboyantPluginTools`
- No abstraction layer
- Hard to test without framework
- Framework appears unmaintained

**Impact:** Hard to migrate to different UI/config systems, testing requires full framework.

---

### 8. **Weak Abstraction in Role Hierarchy (SEVERITY: LOW)**

**Problem:**
```java
public abstract class AManhuntRole {
    protected boolean doStart();
    protected boolean doStop();
    protected void broadcastPlayerResultMessage();
    protected String getName();
    protected String getDescription();
    public ManhuntRoleType getRoleType();
}
```

**Issues:**
- Template method pattern but minimal shared logic
- Each role manages own event listeners
- No shared behavior for common role patterns
- Roles directly implement Listener interface

**Impact:** Duplication across similar roles, each role handles boilerplate.

---

## Architecture Recommendations

### Priority 1: Encapsulate Game State

**Goal:** Replace static `GameData` with encapsulated `GameSession` instances.

**Implementation:**
```java
public class GameSession {
    private final String sessionId;
    private final Map<Player, AManhuntRole> playerRoles;
    private final Map<Player, Location> overworldPortalLocations;
    private final Map<Player, Location> netherPortalLocations;
    private int remainingSpeedrunners;
    private GameState state; // CONFIGURING, STARTING, RUNNING, ENDED
    
    // Encapsulated access methods
    public void assignRole(Player player, AManhuntRole role) { ... }
    public AManhuntRole getRole(Player player) { ... }
    public void recordPortalEntry(Player player, Location location, World.Environment env) { ... }
    public int decrementSpeedrunners() { return --remainingSpeedrunners; }
}

public class GameSessionManager {
    private static GameSessionManager instance;
    private Map<String, GameSession> activeSessions;
    
    public GameSession createSession(String id) { ... }
    public GameSession getActiveSession(Player player) { ... }
}
```

**Benefits:**
- Enables multiple concurrent games
- Clear ownership of game state
- Testable in isolation
- No global static access

**Migration Path:**
1. Create `GameSession` class with private fields
2. Update `NewManhuntManager` to hold a `GameSession` instance
3. Replace `GameData.playerClassList` calls with `session.getRole(player)`
4. Deprecate static `GameData` fields
5. Remove `GameData` class

**Effort:** 4-6 hours

---

### Priority 2: Replace String-Based Role Detection with Type-Safe Mapping

**Goal:** Use explicit role type associations instead of string parsing.

**Implementation:**
```java
public enum ManhuntRoleIdentifier {
    // Add getRoleType() method to enum
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),
    HUNTER_CHECKPOINT(ManhuntRoleType.HUNTER),
    SPEEDRUNNER_SIMPLE(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_CHECKPOINT(ManhuntRoleType.SPEEDRUNNER),
    ALLY_IMPOSTER(ManhuntRoleType.ALLY),
    NEUTRAL_GLUER(ManhuntRoleType.NEUTRAL);
    
    private final ManhuntRoleType roleType;
    
    ManhuntRoleIdentifier(ManhuntRoleType roleType) {
        this.roleType = roleType;
    }
    
    public ManhuntRoleType getRoleType() {
        return roleType;
    }
    
    // Static grouped access
    public static List<ManhuntRoleIdentifier> byType(ManhuntRoleType type) {
        return Arrays.stream(values())
            .filter(r -> r.getRoleType() == type)
            .collect(Collectors.toList());
    }
}
```

**Replace:**
```java
// OLD (fragile)
if (roleId.toString().contains("SPEEDRUNNER"))

// NEW (safe)
if (roleId.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
```

**Benefits:**
- Compile-time safety
- Can rename roles freely
- Clear role type associations
- Enables IDE refactoring

**Effort:** 2-3 hours

---

### Priority 3: Implement Plugin-Based Role Registration

**Goal:** Remove factory switch statement, enable dynamic role registration.

**Implementation:**
```java
// Role registration system
public class RoleRegistry {
    private static RoleRegistry instance;
    private final Map<ManhuntRoleIdentifier, RoleFactory> factories = new HashMap<>();
    
    public void register(ManhuntRoleIdentifier id, RoleFactory factory) {
        factories.put(id, factory);
    }
    
    public AManhuntRole createRole(ManhuntRoleIdentifier id, Player owner) {
        RoleFactory factory = factories.get(id);
        if (factory == null) {
            throw new IllegalStateException("No factory for role: " + id);
        }
        return factory.create(owner);
    }
    
    @FunctionalInterface
    public interface RoleFactory {
        AManhuntRole create(Player owner);
    }
}

// Self-registration in Main.onEnable()
public class Main extends FlamboyantPlugin {
    @Override
    public void onEnable() {
        super.onEnable();
        
        RoleRegistry registry = RoleRegistry.getInstance();
        
        // Register all roles
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_CHECKPOINT, CheckpointSpeedrunnerRole::new);
        registry.register(HUNTER_SIMPLE, HunterRole::new);
        // ... etc
        
        // Or use annotation scanning (advanced)
    }
}
```

**Alternative: Annotation-Based Registration**
```java
@Role(identifier = ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT)
public class CheckpointSpeedrunnerRole extends SpeedrunnerRole {
    // Automatically registered via reflection
}
```

**Benefits:**
- Open/Closed Principle compliance
- Can add roles without modifying factory
- Enables plugin extensions
- Clear registration point

**Effort:** 3-4 hours

---

### Priority 4: Extract Game Phases into State Machine

**Goal:** Make game lifecycle explicit and manageable.

**Implementation:**
```java
public enum GamePhase {
    CONFIGURING,
    COUNTDOWN,
    ROLE_HIDDEN,      // Speedrunner surprise mode
    ROLE_REVEALED,
    DRAGON_FIGHT,
    ENDED
}

public class GamePhaseManager {
    private GamePhase currentPhase = GamePhase.CONFIGURING;
    private final GameSession session;
    private final List<GamePhaseListener> listeners = new ArrayList<>();
    
    public void transitionTo(GamePhase newPhase) {
        GamePhase oldPhase = currentPhase;
        currentPhase = newPhase;
        listeners.forEach(l -> l.onPhaseChange(oldPhase, newPhase));
    }
    
    public boolean canTransitionTo(GamePhase phase) {
        // Validation logic
    }
}

// Listener example
public class RoleRevealListener implements GamePhaseListener {
    @Override
    public void onPhaseChange(GamePhase from, GamePhase to) {
        if (to == GamePhase.ROLE_REVEALED) {
            // Reveal roles to players
            session.getRoles().forEach(role -> role.reveal());
        }
    }
}
```

**Benefits:**
- Clear game progression
- Easy to add new phases
- Centralized phase transition logic
- Better separation of concerns

**Effort:** 5-7 hours

---

### Priority 5: Centralized Event Listener Management

**Goal:** Prevent event listener leaks and standardize lifecycle.

**Implementation:**
```java
public class RoleEventManager {
    private final Map<AManhuntRole, List<Listener>> roleListeners = new HashMap<>();
    private final JavaPlugin plugin;
    
    public void registerRoleListeners(AManhuntRole role, Listener... listeners) {
        List<Listener> listenerList = new ArrayList<>();
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            listenerList.add(listener);
        }
        roleListeners.put(role, listenerList);
    }
    
    public void unregisterRoleListeners(AManhuntRole role) {
        List<Listener> listeners = roleListeners.remove(role);
        if (listeners != null) {
            listeners.forEach(HandlerList::unregisterAll);
        }
    }
    
    public void unregisterAll() {
        roleListeners.values().forEach(list -> 
            list.forEach(HandlerList::unregisterAll));
        roleListeners.clear();
    }
}

// In AManhuntRole
protected abstract List<Listener> createListeners();

// In role implementation
@Override
protected List<Listener> createListeners() {
    return Arrays.asList(this); // or multiple listener objects
}
```

**Benefits:**
- No listener leaks
- Centralized lifecycle management
- Easy to debug listener issues
- Automatic cleanup on game end

**Effort:** 3-4 hours

---

### Priority 6: Simplify Role Distribution with Strategy Pattern

**Goal:** Make role assignment configurable and testable.

**Implementation:**
```java
public interface RoleDistributionStrategy {
    Map<Player, ManhuntRoleIdentifier> distributeRoles(
        List<Player> players,
        RoleDistributionConfig config
    );
}

public class RoleDistributionConfig {
    private int speedrunnerCount;
    private int allyCount;
    private boolean specialRolesOnly;
    private Map<Player, ManhuntRoleIdentifier> fixedAssignments;
    
    // Builder pattern
    public static class Builder { ... }
}

// Default implementation
public class RandomRoleDistribution implements RoleDistributionStrategy {
    @Override
    public Map<Player, ManhuntRoleIdentifier> distributeRoles(
        List<Player> players,
        RoleDistributionConfig config
    ) {
        // Current complex logic extracted here
        // But now testable and swappable
    }
}

// Alternative implementations
public class BalancedRoleDistribution implements RoleDistributionStrategy { ... }
public class CustomRoleDistribution implements RoleDistributionStrategy { ... }
```

**Benefits:**
- Testable in isolation
- Swappable algorithms
- Clear configuration
- Easier to understand

**Effort:** 4-5 hours

---

### Priority 7: Extract Win Condition System

**Goal:** Make win conditions explicit and composable.

**Implementation:**
```java
public interface WinCondition {
    boolean isMet(GameSession session);
    String getDescription();
    Set<ManhuntRoleType> getWinners();
}

public class WinConditionRegistry {
    private final List<WinCondition> conditions = new ArrayList<>();
    
    public void register(WinCondition condition) {
        conditions.add(condition);
    }
    
    public Optional<WinConditionResult> checkWinConditions(GameSession session) {
        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                return Optional.of(new WinConditionResult(condition));
            }
        }
        return Optional.empty();
    }
}

// Implementations
public class AllSpeedrunnersDeadCondition implements WinCondition {
    @Override
    public boolean isMet(GameSession session) {
        return session.getRemainingSpeedrunners() == 0;
    }
    
    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.HUNTER);
    }
}

public class DragonDeadCondition implements WinCondition {
    @Override
    public boolean isMet(GameSession session) {
        return session.isDragonDead();
    }
    
    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleType.ALLY);
    }
}
```

**Benefits:**
- Explicit win condition logic
- Composable (multiple conditions)
- Easy to add custom win conditions per role
- Clearer game ending logic

**Effort:** 3-4 hours

---

### Priority 8: Decouple from FlamboyantPluginTools

**Goal:** Reduce dependency on external framework, improve testability.

**Implementation:**
```java
// Create abstraction layer
public interface GameConfigurationUI {
    void show(Player player, List<ConfigParameter> parameters);
    void onConfigComplete(Consumer<Map<String, Object>> callback);
}

// Adapter for current framework
public class FlamboyantConfigUIAdapter implements GameConfigurationUI {
    @Override
    public void show(Player player, List<ConfigParameter> parameters) {
        // Delegate to ConfigurablePluginListener
    }
}

// Simple in-memory implementation for testing
public class MockConfigUI implements GameConfigurationUI {
    @Override
    public void show(Player player, List<ConfigParameter> parameters) {
        // No-op or test implementation
    }
}

// In Main
private GameConfigurationUI configUI;

@Override
public void onEnable() {
    // Dependency injection
    if (isFlamboyantPluginAvailable()) {
        configUI = new FlamboyantConfigUIAdapter();
    } else {
        configUI = new ChatBasedConfigUI(); // Fallback
    }
}
```

**Benefits:**
- Framework independence
- Easier testing
- Can swap UI implementations
- Reduces coupling

**Effort:** 6-8 hours

---

## Suggested New Architecture

### Layered Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  (Commands, Config UI, Chat Messages)                │
│  - CommandsDispatcher                                │
│  - GameConfigurationUI (abstraction)                 │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                   Application Layer                  │
│  (Game Orchestration, Business Logic)                │
│  - GameSessionManager                                │
│  - GamePhaseManager                                  │
│  - WinConditionRegistry                              │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                     Domain Layer                     │
│  (Core Game Entities)                                │
│  - GameSession                                       │
│  - AManhuntRole (hierarchy)                          │
│  - RoleRegistry                                      │
│  - RoleDistributionStrategy                          │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                Infrastructure Layer                  │
│  (Bukkit Integration, Events, Storage)               │
│  - RoleEventManager                                  │
│  - BukkitEventBridge                                 │
│  - PlayerLocationTracker                             │
└─────────────────────────────────────────────────────┘
```

### Recommended Package Structure

```
me.flamboyant.manhunt/
├── ManhuntPlugin.java              # Main entry point
├── domain/                          # Core game logic
│   ├── game/
│   │   ├── GameSession.java
│   │   ├── GamePhase.java
│   │   └── GameConfiguration.java
│   ├── role/
│   │   ├── Role.java               # Renamed from AManhuntRole
│   │   ├── RoleType.java
│   │   ├── RoleIdentifier.java
│   │   ├── RoleRegistry.java
│   │   └── roles/                  # Role implementations
│   │       ├── hunter/
│   │       │   ├── HunterRole.java
│   │       │   ├── CheckpointHunterRole.java
│   │       │   └── ...
│   │       ├── speedrunner/
│   │       │   ├── SpeedrunnerRole.java
│   │       │   ├── CheckpointSpeedrunnerRole.java
│   │       │   └── ...
│   │       ├── ally/
│   │       └── neutral/
│   └── wincondition/
│       ├── WinCondition.java
│       ├── WinConditionRegistry.java
│       └── conditions/
├── application/                     # Use cases
│   ├── GameSessionManager.java
│   ├── GamePhaseManager.java
│   ├── RoleDistributionService.java
│   └── distribution/
│       ├── RoleDistributionStrategy.java
│       └── RandomRoleDistribution.java
├── infrastructure/                  # Framework integration
│   ├── events/
│   │   ├── RoleEventManager.java
│   │   └── BukkitEventBridge.java
│   ├── storage/
│   │   └── PlayerLocationTracker.java
│   └── config/
│       ├── GameConfigurationUI.java
│       └── FlamboyantConfigUIAdapter.java
├── presentation/                    # User interface
│   ├── commands/
│   │   └── ManhuntCommand.java
│   └── ui/
│       └── PlayerSelectionView.java
└── util/                           # Shared utilities
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1) - 16-20 hours
1. **Priority 2**: Type-safe role detection (2-3h)
2. **Priority 1**: Encapsulate game state into GameSession (4-6h)
3. **Priority 3**: Role registry system (3-4h)
4. **Priority 5**: Centralized event management (3-4h)
5. Write migration tests

**Deliverable:** Core architecture refactored, existing functionality preserved.

---

### Phase 2: Separation of Concerns (Week 2) - 12-16 hours
1. **Priority 4**: Game phase state machine (5-7h)
2. **Priority 7**: Win condition system (3-4h)
3. **Priority 6**: Role distribution strategy (4-5h)

**Deliverable:** Clear separation between game orchestration and game logic.

---

### Phase 3: Decoupling (Week 3) - 8-12 hours
1. **Priority 8**: Framework abstraction (6-8h)
2. Package restructuring (2-4h)
3. Documentation updates

**Deliverable:** Plugin independent of FlamboyantPluginTools, testable in isolation.

---

### Phase 4: Polish & Testing (Week 4) - 8-12 hours
1. Integration tests for all game flows
2. Unit tests for role distribution, win conditions
3. Performance testing (multiple concurrent games)
4. Documentation completion

**Deliverable:** Production-ready refactored codebase with test coverage.

---

## Quick Wins (Can Implement Now)

### 1. Add getRoleType() to ManhuntRoleIdentifier (30 minutes)
```java
public enum ManhuntRoleIdentifier {
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),
    // ... etc
    
    private final ManhuntRoleType type;
    ManhuntRoleIdentifier(ManhuntRoleType type) { this.type = type; }
    public ManhuntRoleType getRoleType() { return type; }
}
```

### 2. Extract Constants (15 minutes)
```java
public class GameConstants {
    public static final int DEFAULT_ROLE_REVEAL_DELAY_MINUTES = 10;
    public static final int HUNTER_COMPASS_COOLDOWN_SECONDS = 30;
    public static final int SPEEDRUNNER_COMPASS_COOLDOWN_MINUTES = 15;
    public static final int CHECKPOINT_COOLDOWN_MINUTES = 15;
}
```

### 3. Add Javadoc to Public APIs (1-2 hours)
Document `AManhuntRole`, `ManhuntRoleFactory`, `NewManhuntManager` public methods.

### 4. Rename Classes for Clarity (30 minutes)
- `AManhuntRole` → `ManhuntRole` (abstract base, no need for A prefix)
- `NewManhuntLauncher` → `GameLauncher` (remove "New" prefix)
- `NewManhuntManager` → `GameManager`

---

## Benefits Summary

### After Refactoring You Will Have:

**Maintainability:**
- ✅ Clear separation of concerns
- ✅ Type-safe role handling
- ✅ No global mutable state
- ✅ Explicit game lifecycle
- ✅ Composable win conditions

**Extensibility:**
- ✅ Add roles without modifying factory
- ✅ Plug in custom role distribution algorithms
- ✅ Add custom win conditions
- ✅ Support multiple concurrent games
- ✅ Easy to add new game phases

**Testability:**
- ✅ Unit testable role distribution
- ✅ Unit testable win conditions
- ✅ Integration testable game flows
- ✅ Mockable configuration UI
- ✅ No framework dependencies required for core logic

**Performance:**
- ✅ No static state contention
- ✅ Clean event listener lifecycle (no leaks)
- ✅ Can run multiple games concurrently

---

## Estimated Total Effort

- **Phase 1 (Foundation):** 16-20 hours
- **Phase 2 (Separation):** 12-16 hours
- **Phase 3 (Decoupling):** 8-12 hours
- **Phase 4 (Testing):** 8-12 hours

**Total:** 44-60 hours (approximately 1.5-2 months part-time work)

**ROI:** After refactoring, adding a new role will take ~30 minutes instead of ~2-3 hours due to reduced coupling and clearer architecture.

---

## Migration Strategy

### Incremental Refactoring (Recommended)

Use **Strangler Fig Pattern**: Build new system alongside old, gradually migrate functionality.

1. **Don't break existing code:** Keep `GameData` functional while building `GameSession`
2. **Add adapters:** Create bridge classes between old and new systems
3. **Migrate one subsystem at a time:** Start with role registry, then state management, etc.
4. **Test continuously:** Ensure game remains playable after each migration step
5. **Remove old code:** Only after new system is stable and tested

### Example Migration for GameSession

```java
// Step 1: Create new GameSession
public class GameSession {
    private Map<Player, AManhuntRole> playerRoles = new HashMap<>();
    // ...
}

// Step 2: Adapter in GameData (temporary)
public class GameData {
    @Deprecated
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    
    // Bridge to new system
    private static GameSession currentSession;
    
    public static void setCurrentSession(GameSession session) {
        currentSession = session;
    }
    
    public static AManhuntRole getRole(Player player) {
        if (currentSession != null) {
            return currentSession.getRole(player);
        }
        return playerClassList.get(player); // Fallback
    }
}

// Step 3: Gradually replace direct GameData access with getRole() calls
// Step 4: Once all callers migrated, remove static HashMap
// Step 5: Remove GameData class entirely
```

---

## Questions to Consider

1. **Do you need to support multiple concurrent games?**
   - If yes, Priority 1 (GameSession) is critical
   - If no, can keep singleton pattern but still encapsulate

2. **Do you plan to add many more roles?**
   - If yes, Priority 3 (Role Registry) will save significant time
   - Consider plugin/addon architecture

3. **Will you open-source or allow third-party extensions?**
   - If yes, decoupling from FlamboyantPluginTools (Priority 8) is essential
   - Publish clear API interfaces

4. **What's your test coverage goal?**
   - If high coverage needed, Priorities 1,6,7 enable unit testing
   - Current architecture is hard to test

5. **How important is performance?**
   - Current architecture is fine for small player counts
   - Refactoring enables optimizations but not critical now

---

## Conclusion

Your codebase implements a fun game mode but has accumulated technical debt that makes it harder to maintain and extend. The recommended refactoring focuses on:

1. **Encapsulation** (remove global state)
2. **Type Safety** (remove string-based logic)
3. **Extensibility** (plugin architecture for roles)
4. **Separation of Concerns** (clear layering)

By following the phased approach, you can incrementally improve the architecture without breaking existing functionality. The investment will pay off through faster feature development and easier maintenance.

**Next Steps:**
1. Review this analysis
2. Prioritize which issues matter most to your goals
3. Start with Phase 1 Quick Wins
4. Begin Phase 1 refactoring when ready

Would you like me to help implement any of these recommendations?
