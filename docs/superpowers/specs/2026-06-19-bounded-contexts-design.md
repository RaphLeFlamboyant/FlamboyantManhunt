# Bounded Contexts Design - Priority 3

**Date:** 2026-06-19  
**Priority:** 3  
**Status:** Design Approved  
**Estimated Effort:** 4-6 hours implementation  
**Dependencies:** Priority 1 (GameSession Aggregate) ✅, Priority 2 (Rich Entities) ✅

---

## Executive Summary

This design establishes **6 bounded contexts** within the Manhunt plugin to create clear boundaries between different parts of the domain. Using **Approach A: Classic Layered DDD Architecture**, we will reorganize the codebase into well-defined contexts with explicit ubiquitous language, clear responsibilities, and defined integration points.

**Goal:** Transform the current mixed-concern architecture into a well-bounded domain model where each context has a single, clear purpose and communicates through defined interfaces.

**Note:** The original REFACTORING_PROGRESS.md proposed 4 bounded contexts. Through brainstorming, we refined this to 6 contexts by:
- Splitting "Role Domain Context" → "Role Definition Context" + "Role Behavior Context" (different volatility)
- Extracting "Win Condition Context" from "Game Management Context" (strategic importance, complexity)

This refinement provides better separation of concerns and clearer boundaries.

---

## Problem Statement

**Current State:**
- No clear boundaries between game management, role logic, tracking, and infrastructure
- Mixed concerns: UI, business logic, and infrastructure intertwined
- Win condition logic scattered across managers and launchers
- Portal tracking embedded in GameSession without clear abstraction
- Difficult to understand which component is responsible for what
- Changes ripple unpredictably across the system

**Target State:**
- 6 clearly defined bounded contexts
- Each context has explicit ubiquitous language
- Package structure reflects context boundaries
- Clear integration interfaces between contexts
- Easy to locate functionality by context
- Changes isolated to appropriate context

---

## Bounded Contexts Overview

### Context Architecture: Approach A - Classic Layered DDD

We will use a layered DDD architecture with contexts organized at the domain level:

```
domain/
├── game/              # Game Management Context
├── role/
│   ├── definition/    # Role Definition Context
│   └── behavior/      # Role Behavior Context
├── tracking/          # Player Tracking Context
└── wincondition/      # Win Condition Context

application/
└── services/          # Application layer coordinates contexts

infrastructure/        # Infrastructure Context
├── bukkit/
├── events/
└── ui/
```

**Rationale for Approach A:**
1. **Clear context boundaries** at domain level
2. **Standard DDD conventions** - familiar to developers
3. **Right-sized** for plugin complexity
4. **Natural migration path** from current structure
5. **Easy navigation** - contexts obvious from package names

---

## The Six Bounded Contexts

### 1. Game Management Context (CORE)

**Purpose:** Manages the lifecycle of game sessions, configuration, and high-level coordination.

**Package:** `me.flamboyant.manhunt.domain.game`

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Session** | A single instance of a Manhunt game with players, roles, and state |
| **Session ID** | Unique identifier for a game session (UUID-based) |
| **Configuration** | Game rules and settings (timers, modes, special rules) |
| **Launch** | Initialize and start a new game session |
| **Terminate** | End an active game session and cleanup resources |
| **Active Session** | Currently running game with players |

**Entities & Value Objects:**
- `GameSession` (Aggregate Root) - Encapsulates all game state
- `GameSessionId` (Value Object) - Session identifier
- `GameConfiguration` (Value Object) - Game settings *(to be created)*

**Responsibilities:**
- ✅ Create and destroy game sessions
- ✅ Session lookup by ID or player
- ✅ Manage player membership in sessions
- ✅ Store player-to-role assignments
- ✅ Coordinate session state
- ✅ Delegate to other contexts for specialized behavior

**Does NOT Own:**
- ❌ Role implementations or behavior (Role Behavior Context)
- ❌ Portal storage logic (Player Tracking Context)
- ❌ Win condition evaluation (Win Condition Context)
- ❌ Event handling (Infrastructure Context)

**Current Files:**
- ✅ `GameSession.java` (already exists - stays here)
- ✅ `GameSessionId.java` (already exists - stays here)
- 🆕 `GameConfiguration.java` (to be created)

**Application Layer:**
- `GameSessionManager` - Singleton managing session registry

---

### 2. Role Definition Context (CORE)

**Purpose:** Defines what roles exist in the game and how to identify them. The "catalog" of available roles.

**Package:** `me.flamboyant.manhunt.domain.role.definition`

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Role Type** | High-level category (HUNTER, SPEEDRUNNER, ALLY, UNDECIDED) |
| **Role Identifier** | Specific role variant (e.g., HUNTER_SIMPLE, SPEEDRUNNER_CHECKPOINT) |
| **Role Registry** | Central registration of available roles and factories |
| **Role Factory** | Creates role instances from identifiers |

**Entities & Value Objects:**
- `ManhuntRoleType` (Enum/Value Object) - Role categories
- `ManhuntRoleIdentifier` (Enum/Value Object) - Specific role variants
- `ManhuntRoleFactory` (Factory) - Creates role instances
- `RoleRegistry` (Service) *(Priority 7)*

**Responsibilities:**
- ✅ Define all available role types
- ✅ Map identifiers to types
- ✅ Provide role creation mechanism
- ✅ Validate role identifiers
- ✅ Support role registration (Priority 7)

**Does NOT Own:**
- ❌ Actual role behavior or abilities (Role Behavior Context)
- ❌ Role instances after creation (Game Management Context)
- ❌ Game state (Game Management Context)

**Current Files (to move here):**
- `ManhuntRoleType.java` ← from `roles/`
- `ManhuntRoleIdentifier.java` ← from `roles/`
- `ManhuntRoleFactory.java` ← from `roles/`

**Key Insight:** This context is **stable** - role definitions change rarely compared to role behavior.

---

### 3. Role Behavior Context (CORE)

**Purpose:** Implements actual role behavior, abilities, and game actions. The "what roles do" context.

**Package:** `me.flamboyant.manhunt.domain.role.behavior`

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Role** | A player's character with specific abilities and behavior |
| **Ability** | An action a role can perform (future abstraction) |
| **Damage Outcome** | Result of taking damage (died, remaining health) |
| **Compass Target** | Location a compass points to, with dimension awareness |
| **Owner** | The player who has this role |
| **Cooldown** | Time before an ability can be reused |
| **Cross-Dimension** | Navigation between Overworld/Nether/End |

**Entities & Value Objects:**
- `AManhuntRole` (Abstract Entity) - Base role behavior
- `SpeedrunnerRole`, `HunterRole` (Concrete Entities) - Core implementations
- 15+ specialized role implementations
- `DamageOutcome` (Value Object) - Damage calculation result
- `CompassTarget` (Value Object) - Compass targeting with dimension info

**Responsibilities:**
- ✅ Execute role-specific actions
- ✅ Handle damage and death (`handleDamage()`)
- ✅ Calculate compass targets (`calculateCompassTarget()`)
- ✅ Manage abilities and cooldowns
- ✅ Respond to in-game events
- ✅ Implement "Tell, Don't Ask" pattern

**Does NOT Own:**
- ❌ Role factory logic (Role Definition Context)
- ❌ Portal location storage (Player Tracking Context)
- ❌ Session lifecycle (Game Management Context)
- ❌ Win condition evaluation (Win Condition Context)

**Current Files (to move here):**
- `AManhuntRole.java` ← from `roles/`
- `SpeedrunnerRole.java` ← from `roles/impl/`
- `HunterRole.java` ← from `roles/impl/`
- All 15+ role implementations from `roles/impl/`
- `DamageOutcome.java` ← from `domain/roles/`
- `CompassTarget.java` ← from `domain/roles/`

**Key Insight:** This context is **volatile** - role behavior changes frequently as gameplay evolves.

---

### 4. Player Tracking Context (SUPPORTING)

**Purpose:** Tracks player locations, portal positions, and dimensional navigation.

**Package:** `me.flamboyant.manhunt.domain.tracking`

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Portal Location** | Entry point to another dimension for a specific player |
| **Dimension** | World environment (NORMAL/Overworld, NETHER, THE_END) |
| **Cross-Dimension Navigation** | Moving or targeting between dimensions |
| **Player Position** | Current location of a player with world context |
| **Portal Registry** | Storage of portal locations per player per dimension |

**Entities & Value Objects:**
- `PortalTracker` (Service) - Manages portal location storage
- `PlayerLocation` (Value Object) - Player position with dimension *(to be created)*
- `PortalEntry` (Value Object) - Portal location for specific dimension *(to be created)*

**Responsibilities:**
- ✅ Store portal locations per player per dimension
- ✅ Retrieve portal locations for cross-dimension targeting
- ✅ Validate portal existence
- ✅ Track player dimensional transitions
- ✅ Clear portal data on player removal

**Does NOT Own:**
- ❌ Game session state (Game Management Context)
- ❌ Role logic (Role Behavior Context)
- ❌ Player-to-role assignment (Game Management Context)

**Current State:**
Portal tracking is currently **embedded in `GameSession`** with methods:
- `getPortalLocation(Player, World.Environment)`
- `setPortalLocation(Player, World.Environment, Location)`
- `Map<Player, Map<World.Environment, Location>> portalLocations`

**Migration Strategy:**
1. Create `PortalTracker` interface and implementation
2. Extract portal storage from `GameSession`
3. `GameSession` delegates to `PortalTracker` instance
4. Roles access portals through `GameSession` (which delegates)

**New Files (to create):**
- `PortalTracker.java` (interface + implementation)
- `PlayerLocation.java` (value object)
- `PortalEntry.java` (value object)

**Key Insight:** This is a **supporting domain** - provides essential service but not core game logic.

---

### 5. Win Condition Context (CORE)

**Purpose:** Evaluates game victory conditions and determines winners.

**Package:** `me.flamboyant.manhunt.domain.wincondition`

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Win Condition** | A rule that determines game victory |
| **Evaluator** | Checks if any win conditions are met |
| **Outcome** | Result of condition evaluation (winner team, description) |
| **Victory** | Successful completion of a win condition |
| **Winner** | Role team that achieved victory (HUNTER, SPEEDRUNNER) |
| **Condition Modifier** | Custom logic that alters standard win conditions |

**Entities & Value Objects:**
- `WinCondition` (Interface) - Contract for win conditions
- `AllSpeedrunnersDeadCondition` (Concrete) - Hunters win condition
- `DragonKilledCondition` (Concrete) - Speedrunners win condition
- `WinConditionEvaluator` (Service) - Evaluates all conditions
- `WinOutcome` (Value Object) - Result of evaluation
- `IHunterWinConditionModifier` (existing interface) - Modifier pattern

**Responsibilities:**
- ✅ Define win condition contracts
- ✅ Evaluate if win conditions are met
- ✅ Determine which team won
- ✅ Support custom/modified win conditions
- ✅ Provide win outcome details (team, description)
- ✅ Trigger game end signal (not implement end logic)

**Does NOT Own:**
- ❌ Game ending implementation (Game Management Context)
- ❌ Session lifecycle (Game Management Context)
- ❌ Event handling (Infrastructure Context)

**Current State:**
Win condition logic is **scattered** across:
- `NewManhuntManager.onEntityDamage()` - checks speedrunner deaths
- `NewManhuntLauncher.onEntityDamage()` - checks dragon death
- `IHunterWinConditionModifier` - modifies hunter conditions
- Hardcoded checks in multiple places

**Migration Strategy:**
1. Create `WinCondition` interface
2. Extract speedrunner death check → `AllSpeedrunnersDeadCondition`
3. Extract dragon death check → `DragonKilledCondition`
4. Create `WinConditionEvaluator` service
5. Managers call evaluator instead of inline checks
6. Migrate `IHunterWinConditionModifier` to this context

**New Files (to create):**
- `WinCondition.java` (interface)
- `AllSpeedrunnersDeadCondition.java` (implementation)
- `DragonKilledCondition.java` (implementation)
- `WinConditionEvaluator.java` (service)
- `WinOutcome.java` (value object)

**File to Move:**
- `IHunterWinConditionModifier.java` ← from `roles/`

**Key Insight:** Win conditions are **strategically important** - they define success and are customizable per game mode.

---

### 6. Infrastructure Context

**Purpose:** Integrates with Bukkit/Spigot framework and provides technical capabilities.

**Package:** `me.flamboyant.manhunt.infrastructure`

**Subpackages:**
- `infrastructure/bukkit/` - Bukkit API adapters
- `infrastructure/events/` - Bukkit event listeners
- `infrastructure/ui/` - User interface components

**Ubiquitous Language:**
| Term | Definition |
|------|------------|
| **Event Bridge** | Translates Bukkit events to domain operations |
| **Event Listener** | Subscribes to Bukkit events |
| **UI View** | Player-facing interface (inventory GUI, chat) |
| **Command Handler** | Processes player commands |
| **Plugin Lifecycle** | Enable/disable hooks |

**Components:**
- Event handlers (Bukkit listeners)
- UI views (inventory GUIs, chat interfaces)
- Command dispatchers
- Plugin main class
- Data persistence (future)

**Responsibilities:**
- ✅ Listen to Bukkit events
- ✅ Translate infrastructure events to domain operations
- ✅ Render UI to players
- ✅ Handle player commands
- ✅ Manage plugin lifecycle
- ✅ Anti-corruption layer for Bukkit

**Does NOT Own:**
- ❌ Domain logic (belongs in domain contexts)
- ❌ Business rules (belongs in domain contexts)
- ❌ Game state (reads/writes through domain APIs)

**Current Files:**
- `Main.java` - Plugin entry point (stays at root)
- `CommandsDispatcher.java` - Command handling (move or stay)
- `PlayerSelectionView.java` ← move to `infrastructure/ui/`
- `NewManhuntManager.java` - Contains event handlers (extract to infrastructure)
- `NewManhuntLauncher.java` - Contains event handlers (extract to infrastructure)

**Key Principle:** Infrastructure depends on domain, but **domain never depends on infrastructure**.

---

## Context Map: Relationships Between Contexts

### Visual Context Map

```
┌──────────────────────────────────────────────────────────────┐
│                   Infrastructure Context                      │
│             (Events, UI, Commands, Bukkit)                    │
│                  [Anti-Corruption Layer]                      │
└────────────┬─────────────────────────────────────────────────┘
             │ (translates events to domain operations)
             │
             ▼
    ┌────────────────────────────────────────────┐
    │     Game Management Context (CORE)         │
    │    (Session lifecycle, coordination)       │
    │            [Aggregate Root]                │
    └─┬──────┬──────────┬──────────────┬────────┘
      │      │          │              │
      │      │          │              │
  ┌───▼───┐  │      ┌───▼────┐    ┌───▼──────────┐
  │ Role  │  │      │ Player │    │ Win Condition│
  │ Def.  │  │      │Tracking│    │   Context    │
  │(CORE) │  │      │(SUPP.) │    │   (CORE)     │
  └───┬───┘  │      └────────┘    └──────────────┘
      │      │
      │   ┌──▼──────┐
      └──►│  Role   │
          │Behavior │
          │ (CORE)  │
          └─────────┘
```

### Relationship Types (DDD Context Mapping Patterns)

#### 1. **Game Management → Role Definition** (Customer/Supplier)
- Game Management is the **customer**
- Role Definition is the **supplier**
- **Communication:** Direct method calls
- **Interface:** `ManhuntRoleFactory.create(identifier, player)`
- **Purpose:** Create role instances from identifiers

#### 2. **Game Management → Role Behavior** (Customer/Supplier)
- Game Management is the **customer**
- Role Behavior is the **supplier**
- **Communication:** Direct method calls
- **Interface:** `session.getRole(player)` → returns `AManhuntRole`
- **Purpose:** Store and retrieve role instances

#### 3. **Game Management → Player Tracking** (Shared Kernel → Extract to Customer/Supplier)
- **Current:** Shared Kernel (portal data embedded in GameSession)
- **Target:** Customer/Supplier (GameSession delegates to PortalTracker)
- **Communication:** Direct method calls through delegation
- **Interface:** `PortalTracker` interface
- **Purpose:** Manage portal locations per player

#### 4. **Game Management → Win Condition** (Customer/Supplier)
- Game Management is the **customer**
- Win Condition is the **supplier**
- **Communication:** Direct method calls
- **Interface:** `WinConditionEvaluator.evaluate(session)`
- **Purpose:** Check if game should end

#### 5. **Role Behavior → Role Definition** (Conformist)
- Role Behavior **conforms** to Role Definition
- **Communication:** Direct usage of enums
- **Interface:** `getRoleIdentifier()`, `getRoleType()`
- **Purpose:** Self-identification

#### 6. **Role Behavior → Player Tracking** (Customer/Supplier via Game Management)
- Role Behavior is the **customer**
- Player Tracking is the **supplier**
- **Communication:** Through GameSession (indirect)
- **Interface:** `session.getPortalLocation(player, dimension)`
- **Purpose:** Get portal locations for compass targeting

#### 7. **Win Condition → Game Management** (Customer/Supplier)
- Win Condition is the **customer**
- Game Management is the **supplier**
- **Communication:** Read-only queries
- **Interface:** `session.getRemainingSpeedrunners()`, etc.
- **Purpose:** Inspect game state for evaluation

#### 8. **Infrastructure → All Domain Contexts** (Anti-Corruption Layer)
- Infrastructure depends on domain
- Domain does NOT depend on infrastructure
- **Communication:** One-way calls from infrastructure to domain
- **Pattern:** Anti-Corruption Layer
- **Purpose:** Translate Bukkit events to domain operations

### Dependency Flow

```
Infrastructure → Application Layer → Domain Contexts
      ↓                                    ↑
   (depends)                          (independent)
```

**Key Principle:** Dependency arrows point **inward** - domain is isolated from infrastructure.

---

## Complete Package Structure & File Mapping

### Full Directory Tree

```
src/main/java/me/flamboyant/manhunt/
│
├── domain/
│   ├── game/                                    [Game Management Context]
│   │   ├── GameSession.java                     ← MOVE from domain/game/
│   │   ├── GameSessionId.java                   ← MOVE from domain/game/
│   │   └── GameConfiguration.java               ← CREATE (extract from params)
│   │
│   ├── role/
│   │   ├── definition/                          [Role Definition Context]
│   │   │   ├── ManhuntRoleType.java             ← MOVE from roles/
│   │   │   ├── ManhuntRoleIdentifier.java       ← MOVE from roles/
│   │   │   └── ManhuntRoleFactory.java          ← MOVE from roles/
│   │   │
│   │   └── behavior/                            [Role Behavior Context]
│   │       ├── AManhuntRole.java                ← MOVE from roles/
│   │       ├── SpeedrunnerRole.java             ← MOVE from roles/impl/
│   │       ├── HunterRole.java                  ← MOVE from roles/impl/
│   │       ├── CheckpointSpeedrunnerRole.java   ← MOVE from roles/impl/
│   │       ├── CheckpointHunterRole.java        ← MOVE from roles/impl/
│   │       ├── CutCleanHunterRole.java          ← MOVE from roles/impl/
│   │       ├── CutCleanSpeedrunnerRole.java     ← MOVE from roles/impl/
│   │       ├── ElfHunterRole.java               ← MOVE from roles/impl/
│   │       ├── ElfSpeedrunnerRole.java          ← MOVE from roles/impl/
│   │       ├── GluerRole.java                   ← MOVE from roles/impl/
│   │       ├── ImposterRole.java                ← MOVE from roles/impl/
│   │       ├── LinkHunterRole.java              ← MOVE from roles/impl/
│   │       ├── LinkSpeedrunnerRole.java         ← MOVE from roles/impl/
│   │       ├── NoNameTagSpeedrunnerRole.java    ← MOVE from roles/impl/
│   │       ├── ProMinerRole.java                ← MOVE from roles/impl/
│   │       ├── SpeedrunnerSwapperRole.java      ← MOVE from roles/impl/
│   │       ├── SuperHunterRole.java             ← MOVE from roles/impl/
│   │       ├── TntTacticalSpeedrunnerRole.java  ← MOVE from roles/impl/
│   │       ├── UndecidedRole.java               ← MOVE from roles/impl/
│   │       ├── WerewolfSpeedrunnerRole.java     ← MOVE from roles/impl/
│   │       ├── DamageOutcome.java               ← MOVE from domain/roles/
│   │       └── CompassTarget.java               ← MOVE from domain/roles/
│   │
│   ├── tracking/                                [Player Tracking Context]
│   │   ├── PortalTracker.java                   ← CREATE (interface + impl)
│   │   ├── PlayerLocation.java                  ← CREATE (value object)
│   │   └── PortalEntry.java                     ← CREATE (value object)
│   │
│   └── wincondition/                            [Win Condition Context]
│       ├── WinCondition.java                    ← CREATE (interface)
│       ├── AllSpeedrunnersDeadCondition.java    ← CREATE (extract from manager)
│       ├── DragonKilledCondition.java           ← CREATE (extract from launcher)
│       ├── WinConditionEvaluator.java           ← CREATE (service)
│       ├── WinOutcome.java                      ← CREATE (value object)
│       └── IHunterWinConditionModifier.java     ← MOVE from roles/
│
├── application/
│   ├── GameSessionManager.java                  ← MOVE from application/
│   └── services/
│       └── (future: StartGameService, etc.)
│
├── infrastructure/                              [Infrastructure Context]
│   ├── bukkit/
│   │   └── (future: Bukkit adapters)
│   ├── events/
│   │   └── (future: Event listeners extracted)
│   └── ui/
│       └── PlayerSelectionView.java             ← MOVE from views/
│
├── Main.java                                    ← STAYS (plugin entry)
├── CommandsDispatcher.java                      ← DECIDE: move or stay
├── NewManhuntManager.java                       ← STAYS (orchestrator)
├── NewManhuntLauncher.java                      ← STAYS (launcher)
└── roles/
    ├── GameRolesManagement.java                 ← DECIDE: move to application/services/ or stay
    └── (all other files moved to domain/)

src/test/java/me/flamboyant/manhunt/
├── domain/
│   ├── game/
│   │   ├── GameSessionTest.java                 ← MOVE from domain/game/
│   │   └── GameSessionIdTest.java               ← MOVE from domain/game/
│   ├── role/
│   │   └── behavior/
│   │       └── SpeedrunnerRoleTest.java         ← MOVE from roles/impl/
│   ├── tracking/
│   │   └── PortalTrackerTest.java               ← CREATE (new tests)
│   └── wincondition/
│       └── WinConditionTest.java                ← CREATE (new tests)
│
├── application/
│   └── GameSessionManagerTest.java              ← MOVE from application/
│
└── GameSessionIntegrationTest.java              ← STAYS (integration)
```

### File Migration Summary

**Files to Move (27 existing files):**
| From | To | Count |
|------|-----|-------|
| `domain/game/` | `domain/game/` | 2 (stay in place) |
| `roles/` | `domain/role/definition/` | 3 |
| `roles/` + `roles/impl/` | `domain/role/behavior/` | 20 |
| `domain/roles/` | `domain/role/behavior/` | 2 |
| `views/` | `infrastructure/ui/` | 1 |
| `roles/` | `domain/wincondition/` | 1 |

**Files to Create (10 new files):**
| Context | File | Purpose |
|---------|------|---------|
| Game Management | `GameConfiguration.java` | Extract game settings |
| Player Tracking | `PortalTracker.java` | Interface + implementation |
| Player Tracking | `PlayerLocation.java` | Value object |
| Player Tracking | `PortalEntry.java` | Value object |
| Win Condition | `WinCondition.java` | Interface |
| Win Condition | `AllSpeedrunnersDeadCondition.java` | Implementation |
| Win Condition | `DragonKilledCondition.java` | Implementation |
| Win Condition | `WinConditionEvaluator.java` | Service |
| Win Condition | `WinOutcome.java` | Value object |
| Tests | Various test files | 2-3 test classes |

**Files to Decide:**
- `GameRolesManagement.java` - Application service or current location?
- `CommandsDispatcher.java` - Infrastructure or root?
- Event handlers in Manager/Launcher - Extract to infrastructure/events/?

---

## Integration Interfaces Between Contexts

### 1. Game Management → Role Definition

**Interface:** `ManhuntRoleFactory`

```java
// In domain/role/definition/ManhuntRoleFactory.java
public class ManhuntRoleFactory {
    public static AManhuntRole create(ManhuntRoleIdentifier id, Player owner) {
        // Factory method creates role instances
        switch (id) {
            case SPEEDRUNNER_SIMPLE: return new SpeedrunnerRole(owner);
            case HUNTER_SIMPLE: return new HunterRole(owner);
            // ... etc
        }
    }
}
```

**Usage in Game Management:**
```java
// In domain/game/GameSession.java
public void assignRole(Player player, ManhuntRoleIdentifier roleId) {
    AManhuntRole role = ManhuntRoleFactory.create(roleId, player);
    this.playerRoles.put(player, role);
}
```

---

### 2. Game Management → Player Tracking

**Interface:** `PortalTracker` *(to be created)*

```java
// In domain/tracking/PortalTracker.java
public interface PortalTracker {
    /**
     * Store a portal location for a player in a specific dimension.
     */
    void setPortalLocation(Player player, World.Environment dimension, Location location);
    
    /**
     * Retrieve a portal location for a player in a specific dimension.
     * @return Optional containing location if portal exists
     */
    Optional<Location> getPortalLocation(Player player, World.Environment dimension);
    
    /**
     * Clear all portal locations for a player.
     */
    void clearPortals(Player player);
    
    /**
     * Check if a player has a portal in a specific dimension.
     */
    boolean hasPortal(Player player, World.Environment dimension);
}

// Implementation
public class InMemoryPortalTracker implements PortalTracker {
    private final Map<Player, Map<World.Environment, Location>> portalLocations;
    // ... implementation
}
```

**Usage in Game Management:**
```java
// In domain/game/GameSession.java
public class GameSession {
    private final PortalTracker portalTracker;
    
    public GameSession(GameSessionId id, PortalTracker portalTracker) {
        this.id = id;
        this.portalTracker = portalTracker;
        // ...
    }
    
    // Delegation methods
    public void setPortalLocation(Player player, World.Environment env, Location loc) {
        portalTracker.setPortalLocation(player, env, loc);
    }
    
    public Optional<Location> getPortalLocation(Player player, World.Environment env) {
        return portalTracker.getPortalLocation(player, env);
    }
}
```

**Migration Steps:**
1. Create `PortalTracker` interface
2. Create `InMemoryPortalTracker` implementation
3. Extract portal map from `GameSession` to `InMemoryPortalTracker`
4. `GameSession` constructor accepts `PortalTracker` instance
5. `GameSession` delegates portal methods to tracker
6. Update tests

---

### 3. Role Behavior → Player Tracking

Roles access portal tracking **through GameSession** (indirect access):

```java
// In domain/role/behavior/AManhuntRole.java
protected CompassTarget calculateCompassTarget(Player target, GameSession session) {
    World ownerWorld = owner.getWorld();
    World targetWorld = target.getWorld();
    
    if (ownerWorld.equals(targetWorld)) {
        return CompassTarget.sameDimension(target.getLocation());
    } else {
        // Cross-dimension: get portal location
        World.Environment targetEnv = determineEnvironment(targetWorld);
        Optional<Location> portalLoc = session.getPortalLocation(target, targetEnv);
        
        if (portalLoc.isPresent()) {
            String dimensionName = getDimensionName(targetWorld);
            return CompassTarget.crossDimension(portalLoc.get(), dimensionName);
        } else {
            // No portal: point to world spawn
            return CompassTarget.sameDimension(ownerWorld.getSpawnLocation());
        }
    }
}
```

**Key Design Decision:** Roles don't access `PortalTracker` directly - they go through `GameSession`. This maintains `GameSession` as the aggregate root and central access point.

---

### 4. Game Management → Win Condition

**Interface:** `WinConditionEvaluator` *(to be created)*

```java
// In domain/wincondition/WinCondition.java
public interface WinCondition {
    /**
     * Check if this win condition is met.
     * @param session The game session to evaluate
     * @return true if condition is met
     */
    boolean isMet(GameSession session);
    
    /**
     * Get the team that wins if this condition is met.
     * @return Set of winning role types
     */
    Set<ManhuntRoleType> getWinners();
    
    /**
     * Get a human-readable description of this condition.
     */
    String getDescription();
}

// In domain/wincondition/WinOutcome.java
public final class WinOutcome {
    private final Set<ManhuntRoleType> winners;
    private final String description;
    private final WinCondition triggeringCondition;
    
    public static WinOutcome of(WinCondition condition) {
        return new WinOutcome(
            condition.getWinners(),
            condition.getDescription(),
            condition
        );
    }
    
    // ... getters, immutability
}

// In domain/wincondition/WinConditionEvaluator.java
public class WinConditionEvaluator {
    private final List<WinCondition> conditions;
    
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.conditions = conditions;
    }
    
    /**
     * Evaluate all win conditions and return outcome if any are met.
     * @return Optional containing outcome if game should end
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }
        return Optional.empty();
    }
}
```

**Concrete Win Condition Example:**

```java
// In domain/wincondition/AllSpeedrunnersDeadCondition.java
public class AllSpeedrunnersDeadCondition implements WinCondition {
    
    @Override
    public boolean isMet(GameSession session) {
        return session.getRemainingSpeedrunners() == 0;
    }
    
    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.HUNTER);
    }
    
    @Override
    public String getDescription() {
        return "Tous les speedrunners sont morts !";
    }
}
```

**Usage in Game Management:**

```java
// In NewManhuntManager.java or future GameService
public class NewManhuntManager {
    private final WinConditionEvaluator winConditionEvaluator;
    
    public NewManhuntManager() {
        // Initialize with standard conditions
        this.winConditionEvaluator = new WinConditionEvaluator(Arrays.asList(
            new AllSpeedrunnersDeadCondition(),
            new DragonKilledCondition()
        ));
    }
    
    public void checkWinConditions(GameSession session) {
        Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(session);
        if (outcome.isPresent()) {
            endGame(session, outcome.get());
        }
    }
    
    private void endGame(GameSession session, WinOutcome outcome) {
        // Announce winners
        String message = outcome.getDescription();
        // ... end game logic
    }
}
```

**Migration Steps:**
1. Create `WinCondition` interface and `WinOutcome` value object
2. Extract speedrunner death check from `NewManhuntManager` → `AllSpeedrunnersDeadCondition`
3. Extract dragon death check from `NewManhuntLauncher` → `DragonKilledCondition`
4. Create `WinConditionEvaluator` service
5. Update managers to call evaluator instead of inline checks
6. Write tests for each condition and evaluator

---

### 5. Infrastructure → Domain (Anti-Corruption Layer)

**Pattern:** Infrastructure translates Bukkit events into domain operations.

**Example:**

```java
// In infrastructure/events/DamageEventHandler.java (future)
public class DamageEventHandler implements Listener {
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // 1. Filter: only players
        if (event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        
        // 2. Get domain objects
        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(player);
        if (session == null) return;
        
        AManhuntRole role = session.getRole(player);
        if (!(role instanceof SpeedrunnerRole)) return;
        
        // 3. Invoke domain logic (Tell, Don't Ask)
        SpeedrunnerRole speedrunner = (SpeedrunnerRole) role;
        DamageOutcome outcome = speedrunner.handleDamage(event.getFinalDamage());
        
        // 4. Handle outcome (still infrastructure concern)
        if (outcome.isDied()) {
            session.decrementSpeedrunnerCount();
            
            // Check win conditions (domain logic)
            WinConditionEvaluator evaluator = getWinConditionEvaluator();
            Optional<WinOutcome> winOutcome = evaluator.evaluate(session);
            
            if (winOutcome.isPresent()) {
                // Trigger game end (infrastructure orchestrates)
                NewManhuntManager.getInstance().endGame(session, winOutcome.get());
            }
        }
    }
}
```

**Key Principles:**
1. Infrastructure **depends on** domain
2. Domain **never imports** Bukkit classes
3. Infrastructure **translates** events to domain operations
4. Infrastructure **orchestrates** multi-context operations
5. Domain remains **testable without Bukkit**

---

## Implementation Strategy

### Phase 1: Package Reorganization (LOW RISK)

**Goal:** Restructure packages without changing logic.

**Steps:**
1. Create new package structure (empty directories)
2. Move files to new packages (IDE refactoring)
3. Update import statements automatically
4. Run full test suite (69 existing tests should pass)
5. Commit: `refactor(contexts): reorganize packages into bounded contexts`

**Estimated Time:** 1-2 hours

**Risk:** Low - pure refactoring, no behavior changes

**Validation:**
- ✅ All tests pass
- ✅ No compilation errors
- ✅ All imports updated

---

### Phase 2: Extract Player Tracking Context (MEDIUM RISK)

**Goal:** Extract portal tracking from GameSession into dedicated context.

**TDD Steps:**

1. **Red:** Write test for `PortalTracker`
```java
@Test
public void shouldStoreAndRetrievePortalLocation() {
    PortalTracker tracker = new InMemoryPortalTracker();
    Player player = mock(Player.class);
    Location loc = mock(Location.class);
    
    tracker.setPortalLocation(player, World.Environment.NETHER, loc);
    Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);
    
    assertTrue(result.isPresent());
    assertEquals(loc, result.get());
}
```

2. **Green:** Implement `PortalTracker`
```java
public class InMemoryPortalTracker implements PortalTracker {
    private final Map<Player, Map<World.Environment, Location>> portalLocations = new HashMap<>();
    
    @Override
    public void setPortalLocation(Player player, World.Environment dim, Location loc) {
        portalLocations.computeIfAbsent(player, k -> new HashMap<>()).put(dim, loc);
    }
    
    @Override
    public Optional<Location> getPortalLocation(Player player, World.Environment dim) {
        return Optional.ofNullable(portalLocations.getOrDefault(player, new HashMap<>()).get(dim));
    }
    // ... other methods
}
```

3. **Refactor:** Extract portal map from `GameSession`
   - Add `PortalTracker` field to `GameSession`
   - Delegate portal methods to tracker
   - Remove internal portal map
   - Update `GameSession` tests

4. **Commit:** `feat(tracking): extract Player Tracking context with PortalTracker`

**Estimated Time:** 2-3 hours (with TDD)

**Risk:** Medium - touches GameSession aggregate root

**Validation:**
- ✅ All existing tests pass
- ✅ New `PortalTrackerTest` passes (10+ tests)
- ✅ No behavior changes in roles

---

### Phase 3: Create Win Condition Context (MEDIUM RISK)

**Goal:** Extract win condition logic into dedicated context.

**TDD Steps:**

1. **Red:** Write test for `AllSpeedrunnersDeadCondition`
```java
@Test
public void shouldDetectWinWhenNoSpeedrunnersRemain() {
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(0);
    
    WinCondition condition = new AllSpeedrunnersDeadCondition();
    
    assertTrue(condition.isMet(session));
    assertTrue(condition.getWinners().contains(ManhuntRoleType.HUNTER));
}
```

2. **Green:** Implement `AllSpeedrunnersDeadCondition`

3. **Refactor:** Extract from `NewManhuntManager`
   - Create `WinConditionEvaluator`
   - Replace inline checks with evaluator calls
   - Remove hardcoded win logic from manager

4. **Red:** Write test for `DragonKilledCondition`

5. **Green:** Implement `DragonKilledCondition`

6. **Refactor:** Extract from `NewManhuntLauncher`

7. **Commit:** `feat(wincondition): create Win Condition context with evaluator`

**Estimated Time:** 2-3 hours (with TDD)

**Risk:** Medium - changes game ending logic

**Validation:**
- ✅ All existing tests pass
- ✅ New `WinConditionTest` passes (8+ tests)
- ✅ Game ends correctly on speedrunner death
- ✅ Game ends correctly on dragon death

---

### Phase 4: Documentation & Context Map (LOW RISK)

**Goal:** Document bounded contexts and relationships.

**Steps:**
1. Create context diagram (ASCII or Mermaid)
2. Document ubiquitous language glossary per context
3. Update `REFACTORING_PROGRESS.md` with completion
4. Create context map documentation
5. Update `docs/architecture/` with new docs
6. Commit: `docs(contexts): add bounded context documentation and context map`

**Estimated Time:** 1 hour

**Risk:** Low - documentation only

**Deliverables:**
- ✅ Context map diagram
- ✅ Ubiquitous language glossary (6 contexts)
- ✅ Integration interfaces documented
- ✅ Migration notes for future priorities

---

## Success Criteria

### Functional Criteria
- ✅ All 6 bounded contexts clearly defined in packages
- ✅ Each context has documented ubiquitous language
- ✅ Context boundaries explicit in code structure
- ✅ Integration points defined with clear interfaces
- ✅ No circular dependencies between contexts
- ✅ Portal tracking extracted from GameSession
- ✅ Win condition logic extracted from managers

### Technical Criteria
- ✅ All 69+ existing tests still pass
- ✅ New tests added for PortalTracker (10+ tests)
- ✅ New tests added for WinCondition (8+ tests)
- ✅ No breaking changes to existing behavior
- ✅ Package structure matches design
- ✅ Import statements correct

### Documentation Criteria
- ✅ Context map documented with relationships
- ✅ Ubiquitous language glossary created
- ✅ Integration interfaces documented
- ✅ REFACTORING_PROGRESS.md updated
- ✅ Architecture docs updated

### Quality Criteria
- ✅ No domain code imports Bukkit classes
- ✅ Infrastructure depends on domain (not vice versa)
- ✅ Each context has single, clear responsibility
- ✅ Context boundaries respected in code

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Breaking existing tests | Medium | High | Run tests after each phase, commit frequently |
| Portal extraction introduces bugs | Medium | Medium | TDD, comprehensive tests, careful delegation |
| Win condition refactor changes behavior | Low | High | Extract logic carefully, integration tests |
| Import statement errors | Medium | Low | Use IDE refactoring tools, compile frequently |
| Circular dependencies | Low | Medium | Follow context map strictly, enforce boundaries |

**Overall Risk Level:** MEDIUM - Manageable with TDD and incremental approach

---

## Future Priorities Enabled by Bounded Contexts

This design enables clean implementation of:

- **Priority 4:** String-based type detection → Type-safe roles (Role Definition Context)
- **Priority 5:** Domain Events → Context boundaries make event sources clear
- **Priority 6:** Application Services → Can coordinate across contexts
- **Priority 7:** Factory refactoring → Contained in Role Definition Context
- **Priority 8:** State machine → Lives in Game Management Context
- **Priority 10:** Role distribution → Application service using Role Definition
- **Priority 11:** Win condition modeling → Already designed in this priority!
- **Priority 12:** Anti-corruption layer → Infrastructure Context pattern
- **Priority 13:** Role abstraction → Isolated in Role Behavior Context

---

## Design Decisions Log

### Decision 1: Split Role Context into Definition vs Behavior
**Rationale:** Role definitions (types, identifiers) change rarely, while role behavior (abilities, actions) changes frequently. Separating them allows independent evolution.

**Alternatives Considered:**
- Single Role Context → rejected (too large, mixed volatility)
- Three contexts (Definition, Behavior, Abilities) → rejected (over-engineered)

---

### Decision 2: Create Dedicated Win Condition Context
**Rationale:** Win conditions are strategically important, customizable, and complex enough to warrant their own context. They'll grow with custom game modes.

**Alternatives Considered:**
- Part of Game Management → rejected (win logic is specialized)
- Part of Role Behavior → rejected (not role-specific)

---

### Decision 3: Extract Player Tracking as Supporting Context
**Rationale:** Portal tracking is essential but not core domain logic. Extracting it clarifies GameSession responsibilities and enables independent testing.

**Alternatives Considered:**
- Keep in GameSession → rejected (aggregate root too large)
- Merge with Game Management → rejected (different responsibility)

---

### Decision 4: Use Direct Method Calls Between Contexts
**Rationale:** Simplicity. Domain events (Priority 5) would decouple contexts but require event infrastructure. Direct calls are simpler and sufficient for synchronous operations.

**Alternatives Considered:**
- Domain events → deferred to Priority 5
- Application services as coordinators → deferred to Priority 6

---

### Decision 5: Infrastructure as Anti-Corruption Layer
**Rationale:** Isolates domain from Bukkit framework. Domain remains testable without Minecraft server. Follows hexagonal architecture principles.

**Alternatives Considered:**
- Domain directly uses Bukkit → rejected (tight coupling)
- Adapter interfaces in domain → rejected (over-abstraction)

---

### Decision 6: GameSession Delegates to PortalTracker
**Rationale:** Maintains GameSession as the aggregate root and central access point. Roles don't access tracking directly, preserving encapsulation.

**Alternatives Considered:**
- Roles access PortalTracker directly → rejected (breaks encapsulation)
- Remove portal methods from GameSession → rejected (breaks existing API)

---

## Appendix A: Ubiquitous Language Glossary

### Game Management Context
| Term | Definition |
|------|------------|
| Session | A single instance of a Manhunt game |
| Session ID | UUID-based unique identifier |
| Configuration | Game rules and settings |
| Launch | Initialize and start a game |
| Terminate | End game and cleanup resources |
| Active Session | Currently running game |
| Player Membership | Players belonging to a session |

### Role Definition Context
| Term | Definition |
|------|------------|
| Role Type | High-level category (HUNTER, SPEEDRUNNER, ALLY) |
| Role Identifier | Specific role variant |
| Role Registry | Available roles catalog |
| Role Factory | Creates role instances |

### Role Behavior Context
| Term | Definition |
|------|------------|
| Role | Player's character with abilities |
| Ability | Action a role can perform |
| Damage Outcome | Result of taking damage |
| Compass Target | Location compass points to |
| Owner | Player who has this role |
| Cooldown | Time before ability reuse |
| Cross-Dimension | Navigation between worlds |

### Player Tracking Context
| Term | Definition |
|------|------------|
| Portal Location | Entry point to another dimension |
| Dimension | World environment (NORMAL, NETHER, THE_END) |
| Cross-Dimension Navigation | Moving/targeting between worlds |
| Player Position | Current location with world context |
| Portal Registry | Storage of portal locations |

### Win Condition Context
| Term | Definition |
|------|------------|
| Win Condition | Rule that determines victory |
| Evaluator | Checks if conditions are met |
| Outcome | Result of evaluation |
| Victory | Successful completion |
| Winner | Team that achieved victory |
| Condition Modifier | Custom logic altering conditions |

### Infrastructure Context
| Term | Definition |
|------|------------|
| Event Bridge | Translates Bukkit events to domain |
| Event Listener | Subscribes to Bukkit events |
| UI View | Player-facing interface |
| Command Handler | Processes player commands |
| Plugin Lifecycle | Enable/disable hooks |

---

## Appendix B: Context Comparison Matrix

| Aspect | Game Mgmt | Role Def | Role Behavior | Tracking | Win Condition | Infrastructure |
|--------|-----------|----------|---------------|----------|---------------|----------------|
| **Type** | Core | Core | Core | Supporting | Core | Technical |
| **Volatility** | Low | Very Low | High | Low | Medium | Medium |
| **Complexity** | Medium | Low | High | Low | Medium | Medium |
| **Dependencies** | 4 contexts | 0 | 2 contexts | 0 | 1 context | All domains |
| **Test Coverage** | High | High | High | High | High | Medium |
| **Size (files)** | 3 | 3 | 22 | 3 | 6 | 5+ |

---

## Appendix C: Migration Checklist

### Pre-Migration
- [ ] Read this design document
- [ ] Understand context boundaries
- [ ] Review current package structure
- [ ] Backup current branch
- [ ] Ensure all 69 tests pass

### Phase 1: Package Reorganization
- [ ] Create new package structure
- [ ] Move Game Management files (3)
- [ ] Move Role Definition files (3)
- [ ] Move Role Behavior files (22)
- [ ] Move Infrastructure files (1)
- [ ] Move Win Condition files (1)
- [ ] Update all imports
- [ ] Run full test suite
- [ ] Fix any compilation errors
- [ ] Commit changes

### Phase 2: Extract Player Tracking
- [ ] Create `PortalTracker` interface (TDD)
- [ ] Implement `InMemoryPortalTracker` (TDD)
- [ ] Write 10+ tests for PortalTracker
- [ ] Extract portal map from GameSession
- [ ] Add PortalTracker field to GameSession
- [ ] Delegate portal methods
- [ ] Update GameSession tests
- [ ] Run full test suite
- [ ] Commit changes

### Phase 3: Create Win Condition Context
- [ ] Create `WinCondition` interface
- [ ] Implement `AllSpeedrunnersDeadCondition` (TDD)
- [ ] Implement `DragonKilledCondition` (TDD)
- [ ] Create `WinConditionEvaluator` (TDD)
- [ ] Write 8+ tests for win conditions
- [ ] Extract logic from NewManhuntManager
- [ ] Extract logic from NewManhuntLauncher
- [ ] Update managers to use evaluator
- [ ] Run full test suite
- [ ] Commit changes

### Phase 4: Documentation
- [ ] Create context diagram
- [ ] Document ubiquitous language
- [ ] Update REFACTORING_PROGRESS.md
- [ ] Create architecture docs
- [ ] Update context map
- [ ] Commit documentation

### Post-Migration
- [ ] Run full test suite (all pass)
- [ ] Verify no behavior changes
- [ ] Review package structure
- [ ] Mark Priority 3 complete
- [ ] Plan Priority 4

---

**End of Design Document**
