# Role Distribution Strategy Pattern - Design Specification

**Priority:** 10  
**Date:** 2026-06-24  
**Status:** Approved  
**Effort Estimate:** 4-5 hours

---

## Overview

This specification describes the refactoring of the role distribution algorithm from a monolithic 174-line method into a composable Strategy pattern. The current implementation in `RoleDistributionService.performDistribution()` contains complex, untestable logic copied from the deprecated `GameRolesManagement` class. This refactoring extracts three distinct responsibilities into separate strategy interfaces, enabling better testability, maintainability, and future extensibility.

---

## Goals

1. **Separate Concerns**: Extract count determination, conflict resolution, and role assignment into distinct strategies
2. **Improve Testability**: Enable unit testing with injected RNG (seeded randomness)
3. **Simplify Logic**: Replace complex nested conditionals with clear, composable strategies
4. **Enable Extensibility**: Allow new distribution modes without modifying existing code
5. **Redesign Algorithm**: Create balanced distribution rules from scratch (not preserving old quirks)

---

## Non-Goals

- Backward compatibility with old `GameRolesManagement` API (already deprecated)
- Preserving exact behavior of old algorithm (intentionally redesigning)
- UI changes for configuration (command interface sufficient)
- Multiple distribution mode selection in UI (default balanced mode only)

---

## Architecture

### Composed Strategy Pattern

Three strategy interfaces work together, orchestrated by `RoleDistributionService`:

```
Input: DistributeRolesCommand
    ↓
RoleCountStrategy → RoleCounts
    ↓
ConflictResolutionStrategy → ConflictResolution  
    ↓
RoleAssignmentStrategy → Map<Player, ManhuntRoleIdentifier>
    ↓
Output: Role assignments + RolesDistributedEvent
```

**Why this approach**: Each strategy has a single, testable responsibility. Strategies can be mixed and matched (e.g., tiered counts with probabilistic assignment) and easily swapped via dependency injection.

---

## Component Design

### 1. Strategy Interfaces

#### 1.1 RoleCountStrategy

**Responsibility**: Determine how many speedrunners, allies, hunters, and neutrals should exist.

```java
public interface RoleCountStrategy {
    /**
     * Determines role counts based on player count and configuration.
     * 
     * @param playerCount Total number of players in the game
     * @param config Distribution configuration (may contain count hints)
     * @return Role counts for each type
     */
    RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config);
}
```

**Default Implementation**: `TieredRoleCountStrategy`

Uses configurable player count tiers:
- 4-7 players: 1 speedrunner, 0 allies, rest hunters
- 8-12 players: 2 speedrunners, 0 allies, rest hunters
- 13-16 players: 2 speedrunners, 1 ally, rest hunters
- 17+ players: 3 speedrunners, 1-2 allies (scales linearly), rest hunters

If admin specifies explicit counts (non-zero), those override tier logic.

#### 1.2 ConflictResolutionStrategy

**Responsibility**: Resolve conflicts when fixed role assignments don't match desired counts.

```java
public interface ConflictResolutionStrategy {
    /**
     * Resolves conflicts when fixed assignments don't match desired counts.
     * 
     * @param fixedAssignments Admin-defined player→role assignments
     * @param desiredCounts Target counts from RoleCountStrategy
     * @param allPlayers All players in the game
     * @return Resolution result (which assignments to keep, which counts to adjust)
     */
    ConflictResolution resolveConflicts(
        Map<Player, ManhuntRoleIdentifier> fixedAssignments,
        RoleCounts desiredCounts,
        List<Player> allPlayers
    );
}
```

**Default Implementation**: `OverwriteConflictResolution` (replaces "berzerk mode")

**Algorithm**:
1. Count fixed assignments by role type
2. If any type exceeds desired count → clear all fixed assignments (overwrite mode)
3. Otherwise → keep fixed assignments, adjust counts down by fixed amount
4. Return players needing assignment (all players - fixed players)

**Alternative Implementation**: `AdjustCountsConflictResolution`
- Always keep fixed assignments
- Adjust desired counts upward to accommodate fixed roles
- Only assign remaining players

#### 1.3 RoleAssignmentStrategy

**Responsibility**: Assign specific role identifiers to players based on counts.

```java
public interface RoleAssignmentStrategy {
    /**
     * Assigns specific roles to players.
     * 
     * @param resolution Result from conflict resolution
     * @param config Distribution configuration (special-only flag, probabilities)
     * @param rng Random number generator (injected for testability)
     * @return Complete player→role assignments
     */
    Map<Player, ManhuntRoleIdentifier> assignRoles(
        ConflictResolution resolution,
        RoleDistributionConfig config,
        Random rng
    );
}
```

**Default Implementation**: `ProbabilisticRoleAssignment`

**Algorithm**:
1. Shuffle players randomly (using injected RNG)
2. Build role pools for each type based on mode:
   - **Special-only mode**: Only select from special role variants
   - **Normal mode**: X% probability for special variant (default 30%), else simple role
3. Assign roles by popping from pools in order: speedrunners → allies → hunters
4. Return complete assignments

**Key improvements over old algorithm**:
- No hardcoded magic probabilities (50%, 20%, 30%)
- No "speedrunnersHitSpecial" flag affecting unrelated hunter logic
- Pool-based assignment (clearer than nested conditionals)
- Fixed shuffle algorithm (old one had potential index issues)
- Configurable special role probability (not hardcoded)

---

### 2. Value Objects

#### 2.1 RoleCounts

```java
public class RoleCounts {
    private final int speedrunners;
    private final int allies;
    private final int hunters;
    private final int neutrals;
    
    // Constructor validates non-negative counts
    public int total() { return speedrunners + allies + hunters + neutrals; }
    public RoleCounts subtract(RoleCounts other) { ... }
    public boolean exceedsAny(RoleCounts other) { ... }
}
```

**Invariants**:
- All counts must be non-negative
- Immutable (all fields final)

#### 2.2 ConflictResolution

```java
public class ConflictResolution {
    private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;
    private final RoleCounts adjustedCounts;
    private final List<Player> playersNeedingAssignment;
    
    // Constructor validates no player in both fixed and needing assignment
}
```

**Purpose**: Encapsulates conflict resolution output (what was kept, what was adjusted, who needs assignment).

#### 2.3 RoleDistributionConfig

```java
public class RoleDistributionConfig {
    private final int speedrunnerCount;      // 0 = auto-calculate
    private final int allyCount;             // 0 = auto-calculate
    private final boolean specialRolesOnly;
    private final double specialRoleProbability;  // 0.0-1.0, default 0.3
    private final List<PlayerCountTier> tiers;
    
    public static RoleDistributionConfig balanced() { ... }
    public static RoleDistributionConfig custom(int speedrunners, int allies) { ... }
}
```

**Factory methods**:
- `balanced()`: Uses default tiers, 30% special probability, auto-counts
- `custom(int, int)`: Fixed counts, 30% special probability
- `specialOnly()`: Auto-counts, 100% special roles

#### 2.4 PlayerCountTier

```java
public class PlayerCountTier {
    private final int minPlayers;
    private final int maxPlayers;
    private final int speedrunners;
    private final int allies;
    // hunters/neutrals calculated as remainder
    
    public boolean matches(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }
}
```

**Purpose**: Define role counts for a player count range.

---

### 3. Orchestration

#### RoleDistributionService (Refactored)

```java
@Singleton
public class RoleDistributionService {
    private final DomainEventPublisher eventPublisher;
    private final RoleCountStrategy countStrategy;
    private final ConflictResolutionStrategy conflictStrategy;
    private final RoleAssignmentStrategy assignmentStrategy;
    private final Random rng;
    
    @Inject
    public RoleDistributionService(
        DomainEventPublisher eventPublisher,
        RoleCountStrategy countStrategy,
        ConflictResolutionStrategy conflictStrategy,
        RoleAssignmentStrategy assignmentStrategy,
        Random rng
    ) { ... }
    
    public Map<Player, ManhuntRoleIdentifier> distributeRoles(DistributeRolesCommand command) {
        // 1. Build config from command
        RoleDistributionConfig config = buildConfigFromCommand(command);
        
        // 2. Determine desired counts
        RoleCounts desiredCounts = countStrategy.determineRoleCounts(
            command.getPlayers().size(), 
            config
        );
        
        // 3. Resolve conflicts with fixed assignments
        ConflictResolution resolution = conflictStrategy.resolveConflicts(
            command.getFixedAssignments(),
            desiredCounts,
            command.getPlayers()
        );
        
        // 4. Assign specific roles
        Map<Player, ManhuntRoleIdentifier> assignments = assignmentStrategy.assignRoles(
            resolution,
            config,
            rng
        );
        
        // 5. Validate and log
        validateAssignments(assignments, command.getPlayers());
        logDistributionSummary(assignments);
        
        // 6. Publish event
        eventPublisher.publish(new RolesDistributedEvent(command.getSessionId(), assignments));
        
        return assignments;
    }
    
    private RoleDistributionConfig buildConfigFromCommand(DistributeRolesCommand command) {
        // Extract config fields from command
        // Use defaults if fields not present
    }
}
```

**Key changes**:
- Replace `performDistribution()` monolith with strategy composition
- Inject strategies via constructor (Guice DI)
- Inject `Random` for testability
- Thin orchestration logic (no business logic)

---

### 4. Dependency Injection

#### ManhuntModule Updates

```java
@Provides
@Singleton
public RoleCountStrategy provideRoleCountStrategy() {
    return new TieredRoleCountStrategy();
}

@Provides
@Singleton
public ConflictResolutionStrategy provideConflictResolutionStrategy() {
    return new OverwriteConflictResolution();
}

@Provides
@Singleton
public RoleAssignmentStrategy provideRoleAssignmentStrategy() {
    return new ProbabilisticRoleAssignment();
}

@Provides
@Singleton
public Random provideRandom() {
    return Common.rng;  // Use existing global RNG
}
```

**Benefits**:
- Strategies easily swappable without changing service code
- Test module can provide different strategies
- `Random` injection enables deterministic tests with seeded RNG

---

## Error Handling

### Exception Hierarchy

```java
public class RoleDistributionException extends RuntimeException { ... }

public class InvalidConfigurationException extends RoleDistributionException { ... }
public class InsufficientPlayersException extends RoleDistributionException { ... }
public class InsufficientRolesException extends RoleDistributionException { ... }
```

### Validation Points

1. **RoleDistributionConfig validation** (constructor):
   - `specialRoleProbability` must be 0.0-1.0
   - Tier ranges must not overlap
   - Counts must be non-negative

2. **RoleCounts validation** (constructor):
   - All counts non-negative
   - Sum equals player count (when validated in service)

3. **ConflictResolution validation**:
   - No player in both `fixedAssignments` and `playersNeedingAssignment`
   - `adjustedCounts` achievable with remaining players

4. **Final assignment validation** (in service):
   - Every player has exactly one role
   - Role type counts match adjusted counts

### Logging Strategy

- **INFO**: Role distribution summary (X speedrunners, Y allies, Z hunters)
- **WARNING**: Conflict resolution triggered, fallback tier used
- **FINE**: Per-player assignment details (debug mode only)

**Cleanup**: Remove excessive logging from old algorithm (dice rolls, index selection).

---

## Testing Strategy

### Unit Tests

**Strategy Tests** (isolated with mocks):

1. **TieredRoleCountStrategyTest**:
   - Test each tier boundary (4, 7, 8, 12, 13, 16, 17+ players)
   - Test explicit count override
   - Test fallback tier
   - Test validation

2. **OverwriteConflictResolutionTest**:
   - Test no conflict path
   - Test conflict triggers overwrite
   - Test count adjustment
   - Test empty fixed assignments

3. **ProbabilisticRoleAssignmentTest**:
   - Test special-only mode
   - Test probability mode with seeded RNG (deterministic)
   - Test role pool exhaustion
   - Test shuffle fairness
   - Test assignment count correctness

**Value Object Tests**:
- `RoleCountsTest`: Validation, subtraction, comparison
- `RoleDistributionConfigTest`: Factory methods, validation
- `PlayerCountTierTest`: Matching logic
- `ConflictResolutionTest`: Value object construction

### Integration Tests

**RoleDistributionServiceTest**:
- Test full flow: command → count → conflict → assignment
- Test with fixed assignments (no conflict)
- Test with conflicting fixed assignments (overwrite)
- Test special-only mode end-to-end
- Test probability mode with seeded RNG
- Verify `RolesDistributedEvent` published

### Test Utilities

```java
public class RoleDistributionTestFixtures {
    public static List<Player> createMockPlayers(int count) { ... }
    public static RoleDistributionConfig balancedConfig() { ... }
    public static RoleDistributionConfig specialOnlyConfig() { ... }
    public static Random seededRandom(long seed) { return new Random(seed); }
}
```

### Seeded RNG for Deterministic Tests

**Key insight**: Injected `Random` enables reproducible tests.

```java
@Test
void testProbabilisticAssignment_deterministic() {
    Random seeded = new Random(12345L);
    ProbabilisticRoleAssignment strategy = new ProbabilisticRoleAssignment();
    
    Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(
        resolution, config, seeded
    );
    
    // With same seed, always same distribution
    assertThat(result.get(player1)).isEqualTo(SPEEDRUNNER_SIMPLE);
}
```

---

## Migration Path

### Phase 1: Create Strategy Infrastructure
1. Create `domain/role/distribution/` package
2. Implement three strategy interfaces
3. Implement value objects (RoleCounts, ConflictResolution, RoleDistributionConfig, PlayerCountTier)
4. Implement default strategies (TieredRoleCountStrategy, OverwriteConflictResolution, ProbabilisticRoleAssignment)
5. Write comprehensive unit tests

### Phase 2: Integrate into Service
1. Refactor `RoleDistributionService` to use strategies
2. Update `ManhuntModule` for dependency injection
3. Update `DistributeRolesCommand` to support new config fields (optional, with defaults)
4. Add integration tests

### Phase 3: Cleanup
1. Delete deprecated `GameRolesManagement` class
2. Remove old `performDistribution()`, `distributeRolesToPlayers()`, helper methods
3. Update `REFACTORING_PROGRESS.md` to mark Priority 10 complete
4. Create `docs/architecture/role-distribution.md` documentation

---

## Backward Compatibility

**None required**: `RoleDistributionService` is internal to application layer. Public API (`DistributeRolesCommand`) remains compatible with new optional fields:
- `specialRoleProbability` defaults to 0.3 (30%)
- `playerCountTiers` defaults to built-in tiers

Existing callers work unchanged.

---

## Package Structure

```
domain/role/distribution/
  ├── RoleCountStrategy.java
  ├── ConflictResolutionStrategy.java
  ├── RoleAssignmentStrategy.java
  ├── RoleCounts.java (value object)
  ├── ConflictResolution.java (value object)
  ├── RoleDistributionConfig.java (value object)
  ├── PlayerCountTier.java (value object)
  ├── RoleDistributionException.java
  ├── InvalidConfigurationException.java
  ├── InsufficientPlayersException.java
  ├── InsufficientRolesException.java
  └── strategies/
      ├── TieredRoleCountStrategy.java
      ├── OverwriteConflictResolution.java
      ├── AdjustCountsConflictResolution.java (future)
      └── ProbabilisticRoleAssignment.java

application/services/
  └── RoleDistributionService.java (refactored)

application/commands/
  └── DistributeRolesCommand.java (add optional fields)

infrastructure/di/
  └── ManhuntModule.java (add strategy providers)

test/
  └── domain/role/distribution/
      ├── TieredRoleCountStrategyTest.java
      ├── OverwriteConflictResolutionTest.java
      ├── ProbabilisticRoleAssignmentTest.java
      ├── RoleCountsTest.java
      ├── RoleDistributionConfigTest.java
      ├── PlayerCountTierTest.java
      ├── ConflictResolutionTest.java
      └── RoleDistributionTestFixtures.java
```

---

## Files to Delete

After migration complete:
- `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java` (already deprecated)

---

## Success Criteria

1. ✅ Three strategy interfaces implemented with default implementations
2. ✅ All value objects created with validation
3. ✅ `RoleDistributionService` refactored to use strategies
4. ✅ Dependency injection configured in `ManhuntModule`
5. ✅ Comprehensive unit tests (15+ test classes)
6. ✅ Integration tests pass
7. ✅ Old `GameRolesManagement` deleted
8. ✅ Documentation updated
9. ✅ All existing tests still pass
10. ✅ Strategies are swappable without code changes (via DI)

---

## Design Decisions

### Why Three Strategies Instead of One?

**Decision**: Separate count determination, conflict resolution, and assignment into three strategies.

**Rationale**: Each has a distinct responsibility and different reasons to change. Composing them enables mix-and-match (e.g., balanced counts with admin-defined assignment). Single strategy would force duplication across implementations.

### Why Redesign Algorithm Instead of Preserving?

**Decision**: Create new balanced distribution rules from scratch.

**Rationale**: Old algorithm has bugs (infinite loop in `diceAllyCount`, broken shuffle logic) and unclear business rules (magic probabilities, "speedrunnersHitSpecial" flag). Fresh start enables clean, testable, understandable logic.

### Why Inject Random Instead of Using Common.rng?

**Decision**: Inject `Random` via constructor.

**Rationale**: Enables deterministic unit tests with seeded RNG. Tests can verify exact role distributions without flakiness. Production code still uses `Common.rng` (injected by Guice).

### Why Probability-Based Instead of Count-Based for Special Roles?

**Decision**: Each role has X% probability of being special (default 30%).

**Rationale**: Simpler to understand and configure than "first N are special". Ensures special roles distributed evenly across player join order. Configurable probability gives flexibility.

### Why Overwrite Conflict Resolution as Default?

**Decision**: Default to clearing all fixed assignments on conflict (old "berzerk mode").

**Rationale**: Preserves existing behavior for current users. Alternative strategy (`AdjustCountsConflictResolution`) available for future use. Clear warning logged when triggered.

---

## Future Extensions

**Possible future implementations** (not in scope for Priority 10):

1. **Alternative Count Strategies**:
   - `FixedRoleCountStrategy`: Admin defines exact counts, no tiers
   - `RatioBasedRoleCountStrategy`: Maintain percentage ratios (15% speedrunners)
   - `RandomizedRoleCountStrategy`: Dice-based like old algorithm (for nostalgia)

2. **Alternative Conflict Strategies**:
   - `AdjustCountsConflictResolution`: Keep fixed, adjust counts up
   - `FailFastConflictResolution`: Throw exception on conflict
   - `PriorityConflictResolution`: Priority ordering for roles

3. **Alternative Assignment Strategies**:
   - `BalancedRoleAssignment`: Ensure even skill distribution
   - `AllSpecialRoleAssignment`: Guarantee special roles only
   - `CustomWeightedRoleAssignment`: Admin-defined role weights

4. **Configuration UI**:
   - UI to define custom tiers
   - UI to select conflict resolution strategy
   - UI to configure special role probability

---

## References

- **PROBLEMS_PRIORITY_SUMMARY.md**: Priority 10 original specification
- **REFACTORING_PROGRESS.md**: Progress tracking
- **Priority 6 Work**: `RoleDistributionService` creation (algorithm copied as-is)
- **Priority 4 Work**: `RoleTypeRegistry` for type-safe role lookups
- **Strategy Pattern**: Gang of Four Design Patterns

---

**End of Specification**
