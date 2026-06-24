# Role Distribution Architecture

**Last Updated:** 2026-06-24  
**Priority:** 10 (DDD Refactoring)

---

## Overview

The role distribution system assigns roles (Speedrunner, Ally, Hunter, Neutral) to players at game start. It uses a composed Strategy pattern with three focused interfaces:

1. **RoleCountStrategy**: Determines how many of each role type
2. **ConflictResolutionStrategy**: Handles conflicts between admin-fixed assignments and desired counts
3. **RoleAssignmentStrategy**: Assigns specific role identifiers to players

---

## Architecture

### Strategy Composition

```
DistributeRolesCommand
    ↓
RoleDistributionConfig (built from command)
    ↓
RoleCountStrategy → RoleCounts
    ↓
ConflictResolutionStrategy → ConflictResolution
    ↓
RoleAssignmentStrategy → Map<Player, ManhuntRoleIdentifier>
    ↓
RolesDistributedEvent
```

Strategies are injected via Guice and orchestrated by `RoleDistributionService`.

---

## Default Strategies

### TieredRoleCountStrategy

**Player Count Tiers:**
- 4-7 players: 1 speedrunner, 0 allies
- 8-12 players: 2 speedrunners, 0 allies
- 13-16 players: 2 speedrunners, 1 ally
- 17+ players: 3 speedrunners, 1-2 allies

Admin can override with explicit counts.

### OverwriteConflictResolution

**Conflict Detection:**
- If fixed assignments exceed desired counts → clear all fixed (berzerk mode)
- Otherwise → keep fixed, adjust counts down

**Example:** Want 1 speedrunner, but 3 players have fixed speedrunner roles → clear all fixed assignments and redistribute.

### ProbabilisticRoleAssignment

**Special Role Selection:**
- **Normal mode**: 30% probability for special variant (configurable)
- **Special-only mode**: Only select special role variants

**Algorithm:**
1. Shuffle players randomly
2. Build role pools (speedrunners, allies, hunters)
3. Assign roles in order from pools

---

## Value Objects

### RoleCounts
- Immutable counts per role type
- Validation: non-negative counts
- Operations: `total()`, `subtract()`, `exceedsAny()`

### ConflictResolution
- Fixed assignments (kept after conflict resolution)
- Adjusted counts (desired - fixed)
- Players needing assignment (not in fixed)

### RoleDistributionConfig
- Speedrunner/ally count hints (0 = auto-calculate)
- Special-only flag
- Special role probability (0.0-1.0, default 0.3)
- Player count tiers

### PlayerCountTier
- Player count range (min-max)
- Role counts for that range
- Matching predicate

---

## Testability

### Injectable Random

`Random` is injected via Guice, enabling deterministic tests with seeded RNG:

```java
Random seeded = new Random(12345L);
Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(..., seeded);
// Same seed = same distribution every time
```

### Strategy Isolation

Each strategy is unit tested independently with mocks. Integration tests verify full orchestration.

---

## Extensibility

New strategies can be added without modifying existing code:

**Example: Ratio-Based Count Strategy**
```java
public class RatioBasedRoleCountStrategy implements RoleCountStrategy {
    public RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config) {
        int speedrunners = (int) (playerCount * 0.15); // 15% speedrunners
        int allies = (int) (playerCount * 0.05); // 5% allies
        int hunters = playerCount - speedrunners - allies;
        return new RoleCounts(speedrunners, allies, hunters, 0);
    }
}
```

Swap in `ManhuntModule`:
```java
@Provides
@Singleton
public RoleCountStrategy provideRoleCountStrategy() {
    return new RatioBasedRoleCountStrategy();
}
```

---

## Migration from Old System

**Before (Priority 6-9):**
- Monolithic `performDistribution()` method (174 lines)
- Complex nested conditionals
- Hardcoded probabilities (50%, 20%, 30%)
- Untestable (global `Common.rng`)
- Bugs: infinite loop in `diceAllyCount`, broken shuffle

**After (Priority 10):**
- Three focused strategies (~60 lines each)
- Clear responsibilities
- Configurable probabilities
- Injectable RNG (deterministic tests)
- No known bugs (redesigned from scratch)

---

## See Also

- `docs/superpowers/specs/2026-06-24-role-distribution-strategy-design.md`: Design specification
- `docs/PROBLEMS_PRIORITY_SUMMARY.md`: Priority 10 original problem description
- `REFACTORING_PROGRESS.md`: Implementation tracking
