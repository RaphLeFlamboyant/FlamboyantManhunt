# Bounded Contexts Map

**Last Updated:** 2026-06-19
**Status:** Implemented (Priority 3)

## Overview

The Manhunt plugin is organized into **6 bounded contexts** following Domain-Driven Design principles. This document describes each context, their relationships, and integration points.

## Context Diagram

```
+------------------------------------------------------------------+
|                   Infrastructure Context                          |
|             (Events, UI, Commands, Bukkit)                        |
|                  [Anti-Corruption Layer]                          |
+--------+---------------------------------------------------------+
         |     (translates events to domain operations)
         |
         v
    +--------------------------------------------+
    |     Game Management Context (CORE)         |
    |    (Session lifecycle, coordination)       |
    |            [Aggregate Root]                |
    +-+------+----------+------------+-----------+
      |      |          |            |
      |      |          |            |
  +---v---+  |      +---v----+   +---v--------------+
  | Role  |  |      | Player |   | Win Condition    |
  | Def.  |  |      |Tracking|   |   Context        |
  |(CORE) |  |      |(SUPP.) |   |   (CORE)         |
  +---+---+  |      +--------+   +------------------+
      |      |
      |   +--v--------+
      +-->|  Role     |
          |Behavior   |
          | (CORE)    |
          +-----------+
```

## The Six Contexts

### 1. Game Management Context
**Package:** `me.flamboyant.manhunt.domain.game`
**Type:** Core Domain
**Responsibility:** Session lifecycle and coordination

**Key Classes:**
- `GameSession` (Aggregate Root)
- `GameSessionId` (Value Object)
- `GameSessionManager` (Application Service)

**Relationships:**
- Uses Role Definition to create roles
- Uses Role Behavior to store role instances
- Uses Player Tracking for portal management
- Uses Win Condition to check game end

---

### 2. Role Definition Context
**Package:** `me.flamboyant.manhunt.domain.role.definition`
**Type:** Core Domain
**Responsibility:** Define available role types and identifiers

**Key Classes:**
- `ManhuntRoleType` (Enum)
- `ManhuntRoleIdentifier` (Enum)
- `ManhuntRoleFactory` (Factory)

**Relationships:**
- No dependencies on other contexts (independent)
- Used by Game Management and Role Behavior

---

### 3. Role Behavior Context
**Package:** `me.flamboyant.manhunt.domain.role.behavior`
**Type:** Core Domain
**Responsibility:** Implement role actions and abilities

**Key Classes:**
- `AManhuntRole` (Abstract Entity)
- `SpeedrunnerRole`, `HunterRole` (Concrete Entities)
- 15+ specialized role implementations
- `DamageOutcome`, `CompassTarget` (Value Objects)

**Relationships:**
- Conforms to Role Definition (uses types/identifiers)
- Uses Player Tracking through Game Management (portal locations)

---

### 4. Player Tracking Context
**Package:** `me.flamboyant.manhunt.domain.tracking`
**Type:** Supporting Domain
**Responsibility:** Track portal locations and player positions

**Key Classes:**
- `PortalTracker` (Interface)
- `InMemoryPortalTracker` (Implementation)

**Relationships:**
- No dependencies (supporting context)
- Used by Game Management and Role Behavior

---

### 5. Win Condition Context
**Package:** `me.flamboyant.manhunt.domain.wincondition`
**Type:** Core Domain
**Responsibility:** Evaluate victory conditions

**Key Classes:**
- `WinCondition` (Interface)
- `AllSpeedrunnersDeadCondition` (Implementation)
- `DragonKilledCondition` (Implementation)
- `WinConditionEvaluator` (Service)
- `WinOutcome` (Value Object)
- `IHunterWinConditionModifier` (Interface)

**Relationships:**
- Reads from Game Management (session state)
- Used by Infrastructure to trigger game end

---

### 6. Infrastructure Context
**Package:** `me.flamboyant.manhunt.infrastructure`
**Type:** Technical
**Responsibility:** Bukkit integration and UI

**Key Classes:**
- `PlayerSelectionView` (UI)
- `Main` (Plugin entry)
- Event handlers in managers (to be extracted)

**Relationships:**
- Depends on all domain contexts
- Anti-Corruption Layer pattern

---

## Integration Patterns

### Game Management -> Role Definition (Customer/Supplier)
```java
// Game Management uses Role Definition to create roles
AManhuntRole role = ManhuntRoleFactory.createRole(player, roleId);
session.assignRole(player, role);
```

### Game Management <-> Player Tracking (Delegation)
```java
// Game Management delegates portal tracking
public class GameSession {
    private final PortalTracker portalTracker;

    public void recordPortalEntry(Player p, Location l, Environment e) {
        portalTracker.setPortalLocation(p, e, l);
    }
}
```

### Infrastructure -> Win Condition (Anti-Corruption)
```java
// Infrastructure translates Bukkit events to domain operations
dragonCondition.markDragonKilled();
manager.checkWinConditions();
```

---

## Ubiquitous Language

### Game Management Context
- **Session**: An active game instance with players and their roles
- **Configuration**: Game settings (timings, counts, options)
- **Launch**: Starting a new game session

### Role Definition Context
- **RoleType**: Category of role (SPEEDRUNNER, HUNTER, ALLY, NEUTRAL)
- **RoleIdentifier**: Specific role variant (SPEEDRUNNER, HUNTER, ELF_SPEEDRUNNER, etc.)
- **Factory**: Creates role instances from identifiers

### Role Behavior Context
- **Role**: Entity representing a player's game role with abilities
- **Ability**: An action or power available to a role
- **DamageOutcome**: Result of damage calculation (died, survived)
- **CompassTarget**: Target location for hunter compass

### Player Tracking Context
- **PortalLocation**: Stored coordinates where player entered a portal
- **Dimension**: World environment (NORMAL, NETHER, THE_END)
- **CrossDimension**: Tracking across different worlds

### Win Condition Context
- **WinCondition**: Rule that determines game end
- **Evaluator**: Service that checks all conditions
- **Outcome**: Result when condition is met (winners, description)
- **Victory**: Game end state with winning team

### Infrastructure Context
- **EventBridge**: Translates Bukkit events to domain operations
- **UIView**: Visual interface for player interaction
- **CommandHandler**: CLI command processing

---

## Migration Notes

**From:** Mixed concerns, no clear boundaries
**To:** 6 well-defined contexts with explicit interfaces

**Key Achievements:**
- Package structure reflects context boundaries
- Portal tracking extracted from GameSession
- Win conditions extracted from managers
- All tests passing
- No behavior changes

**Next Steps:**
- Priority 4: Fix primitive obsession (string-based type detection)
- Priority 5: Implement domain events for decoupling
- Priority 6: Create application services layer

---

**References:**
- Design Spec: `docs/superpowers/specs/2026-06-19-bounded-contexts-design.md`
- Implementation Plan: `docs/superpowers/plans/2026-06-19-bounded-contexts.md`
