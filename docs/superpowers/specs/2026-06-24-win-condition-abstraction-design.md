# Win Condition Abstraction - Design Specification

**Priority:** 11  
**Date:** 2026-06-24  
**Status:** Approved  
**Effort Estimate:** 3-4 hours

---

## Problem Statement

Win conditions are partially implemented but incomplete:

**What exists:**
- ✅ `WinCondition` interface with base conditions (AllSpeedrunnersDead, DragonKilled)
- ✅ `WinConditionEvaluator` for checking conditions
- ✅ Integration into game flow

**What's broken:**
- ❌ `IHunterWinConditionModifier` uses static list anti-pattern (memory leak risk)
- ❌ `HunterRole.winconModifiers` is a global static list
- ❌ Role result messaging scattered in role classes
- ❌ No clean way to block or enhance win conditions

**Impact:**
- Memory leaks from static lists across games
- Win condition logic fragmented between roles and managers
- Hard to test win scenarios
- Unclear extension point for custom win conditions

---

## Goals

1. **Eliminate static list anti-pattern** - Replace with evaluator-bound modifiers
2. **Support modifier patterns** - Both blocking/gating and alternative wins
3. **Centralize result messaging** - Move from roles to application layer
4. **Maintain backward compatibility** - Existing win conditions continue working
5. **Enable extensibility** - Clean architecture for future custom conditions

---

## Architecture Overview

### Core Strategy: Evaluator-Centric Modifiers

Enhance `WinConditionEvaluator` to support modifiers registered directly on the evaluator instance. Modifiers can both gate base win conditions and provide alternative win paths.

**Lifecycle:** Modifiers → Evaluator → Session → Destroyed together (no leaks)

### Key Components

1. **WinConditionModifier** (new interface)
   - Replaces `IHunterWinConditionModifier`
   - Two capabilities: blocking and alternative wins

2. **Enhanced WinConditionEvaluator**
   - Maintains existing base condition checking
   - Adds modifier registration and evaluation
   - Instance-scoped (session-bound)

3. **Migration Path**
   - Remove static `HunterRole.winconModifiers` list
   - Convert `SuperHunterRole` to new modifier interface
   - Move role result messaging to `EndGameSaga`

---

## Component Design

### WinConditionModifier Interface

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import java.util.Optional;

/**
 * Modifies win condition evaluation by blocking or providing alternative wins.
 * Registered on WinConditionEvaluator instances (session-scoped).
 */
public interface WinConditionModifier {
    
    /**
     * Check if this modifier allows the given win condition to trigger.
     * 
     * @param condition The win condition being evaluated
     * @param session Current game session
     * @return false to block the win, true to allow it
     */
    boolean allowsWin(WinCondition condition, GameSession session);
    
    /**
     * Provide an alternative win condition if applicable.
     * Called after base conditions are evaluated.
     * 
     * @param session Current game session
     * @return Optional containing an alternative WinCondition, or empty
     */
    Optional<WinCondition> getAlternativeWin(GameSession session);
}
```

**Design Notes:**
- Most modifiers only implement one method (blocking OR alternative)
- Return `true` from `allowsWin()` for conditions you don't want to gate
- Return `Optional.empty()` from `getAlternativeWin()` if no alternative

### Enhanced WinConditionEvaluator

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates win conditions and applies registered modifiers.
 * Instance-scoped to GameSession (modifiers cleared on session end).
 */
public class WinConditionEvaluator {

    private final List<WinCondition> baseConditions;
    private final List<WinConditionModifier> modifiers;

    /**
     * Create an evaluator with base win conditions.
     *
     * @param conditions The base win conditions to check
     */
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.baseConditions = List.copyOf(Objects.requireNonNull(conditions));
        this.modifiers = new ArrayList<>();
    }

    /**
     * Register a modifier that can block or provide alternative wins.
     * Modifiers are session-scoped and cleared when evaluator is destroyed.
     *
     * @param modifier The modifier to register
     */
    public void registerModifier(WinConditionModifier modifier) {
        Objects.requireNonNull(modifier, "Modifier cannot be null");
        this.modifiers.add(modifier);
    }

    /**
     * Evaluate all win conditions with modifier support.
     * 
     * Flow:
     * 1. Check base conditions (AllSpeedrunnersDead, DragonKilled)
     * 2. Apply modifiers - any can block
     * 3. Check alternative wins from modifiers
     * 4. Return first match or empty
     *
     * @param session The game session to evaluate
     * @return Optional containing WinOutcome if any condition is met
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        Objects.requireNonNull(session, "Session cannot be null");

        // 1. Check base conditions
        for (WinCondition condition : baseConditions) {
            if (condition.isMet(session)) {
                // 2. Apply modifiers - check if allowed
                if (isWinAllowedByModifiers(condition, session)) {
                    return Optional.of(WinOutcome.of(condition));
                }
            }
        }

        // 3. Check alternative wins from modifiers
        return checkAlternativeWins(session);
    }

    /**
     * Check if all modifiers allow this win condition.
     * ANY modifier returning false will block the win.
     */
    private boolean isWinAllowedByModifiers(WinCondition condition, GameSession session) {
        for (WinConditionModifier modifier : modifiers) {
            if (!modifier.allowsWin(condition, session)) {
                return false; // Blocked by this modifier
            }
        }
        return true; // All modifiers allow it
    }

    /**
     * Check if any modifier provides an alternative win.
     * Returns first alternative found.
     */
    private Optional<WinOutcome> checkAlternativeWins(GameSession session) {
        for (WinConditionModifier modifier : modifiers) {
            Optional<WinCondition> alternative = modifier.getAlternativeWin(session);
            if (alternative.isPresent()) {
                WinCondition condition = alternative.get();
                if (condition.isMet(session)) {
                    return Optional.of(WinOutcome.of(condition));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get the base conditions being evaluated.
     *
     * @return Immutable list of base win conditions
     */
    public List<WinCondition> getConditions() {
        return baseConditions;
    }
    
    /**
     * Get currently registered modifiers (for testing).
     *
     * @return List of registered modifiers
     */
    public List<WinConditionModifier> getModifiers() {
        return List.copyOf(modifiers);
    }
}
```

---

## Data Flow & Integration

### Evaluation Flow

```
Session state changes
    ↓
NewManhuntManager checks for win
    ↓
Calls evaluator.evaluate(session)
    ↓
Evaluator checks base conditions
    ↓ (condition met)
Evaluator applies modifiers
    ↓ (all allow = true)
Returns WinOutcome
    ↓
EndGameSaga handles game end
    ↓
Broadcasts role results
```

### How Roles Register Modifiers

Roles that modify win conditions register during startup:

```java
// In SuperHunterRole.doStart()
@Override
protected boolean doStart() {
    // Register as modifier
    GameSession session = GameSessionManager.getInstance()
        .getActiveSessionForPlayer(owner);
    if (session != null) {
        session.getWinConditionEvaluator().registerModifier(this);
    }
    
    return super.doStart();
}

// No need for doStop() unregister - evaluator destroyed with session
```

### SuperHunterRole Migration

**Before (static list pattern):**
```java
public class SuperHunterRole extends HunterRole 
    implements IHunterWinConditionModifier {
    
    @Override
    protected boolean doStart() {
        HunterRole.winconModifiers.add(this); // STATIC LIST
        return super.doStart();
    }
    
    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().runTaskLater(
            Common.plugin, 
            () -> HunterRole.winconModifiers.remove(this), 
            20
        ); // MANUAL CLEANUP
        return super.doStop();
    }
    
    @Override
    public boolean isHunterWinPossible() {
        return speedRunnerKillCount >= 3;
    }
}
```

**After (evaluator-bound modifier):**
```java
public class SuperHunterRole extends HunterRole 
    implements WinConditionModifier {
    
    @Override
    protected boolean doStart() {
        // Register with evaluator (session-scoped)
        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(owner);
        if (session != null) {
            session.getWinConditionEvaluator().registerModifier(this);
        }
        return super.doStart();
    }
    
    // No doStop() override needed - automatic cleanup
    
    @Override
    public boolean allowsWin(WinCondition condition, GameSession session) {
        // Only gate AllSpeedrunnersDead (hunter win)
        if (condition instanceof AllSpeedrunnersDeadCondition) {
            return speedRunnerKillCount >= 3;
        }
        return true; // Don't block other win types
    }
    
    @Override
    public Optional<WinCondition> getAlternativeWin(GameSession session) {
        return Optional.empty(); // No alternative win path
    }
}
```

### Role Result Messaging Migration

**Remove from HunterRole:**
```java
// DELETE this entire method
@Override
protected void broadcastPlayerResultMessage() {
    boolean wincon = session != null 
        && session.getRemainingSpeedrunners() == 0;

    for (IHunterWinConditionModifier modifier : winconModifiers) {
        wincon &= modifier.isHunterWinPossible();
    }

    Bukkit.broadcastMessage(ChatHelper.feedback(
        owner.getDisplayName() + ", qui était " + getName() 
        + " a " + (wincon ? "gagné" : "perdu") + " !"
    ));
}
```

**Add to EndGameSaga:**
```java
public GameSession endGame(EndGameCommand command) {
    // ... existing game end logic ...
    
    // NEW: Broadcast results for each player based on WinOutcome
    GameSession session = lifecycleService.getSession(command.getSessionId());
    WinOutcome outcome = command.getWinOutcome();
    
    session.getAllRoles().forEach((player, role) -> {
        boolean playerWon = outcome.getWinners().contains(role.getRoleType());
        String result = String.format(
            "%s, qui était %s a %s !",
            player.getDisplayName(),
            role.getName(),
            playerWon ? "gagné" : "perdu"
        );
        Bukkit.broadcastMessage(ChatHelper.feedback(result));
    });
    
    return session;
}
```

---

## Error Handling & Edge Cases

### Modifier Conflicts

**Scenario:** Multiple modifiers block the same win condition.

**Resolution:** If ANY modifier returns `false` from `allowsWin()`, the win is blocked. All modifiers must allow it (AND logic).

```java
// In isWinAllowedByModifiers()
for (WinConditionModifier modifier : modifiers) {
    if (!modifier.allowsWin(condition, session)) {
        return false; // First blocker wins
    }
}
return true; // All must allow
```

### Multiple Alternative Wins

**Scenario:** Multiple modifiers provide alternative win conditions.

**Resolution:** Return the first alternative win found. Order of registration matters, but conflicts should be rare in practice (different roles provide different alternatives).

### Null Safety

- `registerModifier()` validates non-null modifier
- `getAlternativeWin()` must return non-null Optional (empty if none)
- `allowsWin()` must return boolean (never null)
- `evaluate()` handles null session gracefully (throws NPE with clear message)

### Backward Compatibility

Existing win condition evaluation continues working:
- Base conditions evaluated first (unchanged behavior)
- If no modifiers registered, behaves identically to old implementation
- Existing tests should pass without modification

---

## Testing Strategy

### Unit Tests

**WinConditionModifier Interface:**
- Create mock modifier that blocks wins
- Create mock modifier that allows wins
- Create mock modifier that provides alternative win
- Create mock modifier that does both

**Enhanced WinConditionEvaluator:**
- Test base condition evaluation (existing behavior)
- Test single modifier blocking a win
- Test multiple modifiers (all must allow)
- Test modifier allows a win
- Test alternative win selection
- Test multiple alternative wins (first wins)
- Test modifier registration
- Test evaluation with no modifiers (backward compatibility)
- Test null safety (null session, null modifier)

**SuperHunterRole Migration:**
- Test modifier blocks hunter win when kill count < 3
- Test modifier allows hunter win when kill count >= 3
- Test modifier doesn't block speedrunner wins
- Test modifier registration on doStart()
- Test kill count tracking (existing behavior)

**EndGameSaga Result Messaging:**
- Test broadcasts correct result for winning role
- Test broadcasts correct result for losing role
- Test handles all role types
- Test handles multiple players with different roles

### Integration Tests

**Full Game Scenarios:**
- SuperHunter blocks hunter win until requirement met
- All speedrunners die but SuperHunter requirement not met → game continues
- SuperHunter meets requirement, then speedrunners die → hunters win
- Multiple modifiers active simultaneously
- Dragon killed while modifiers active → speedrunner wins (not blocked)

**Lifecycle Tests:**
- Session ends → evaluator destroyed → modifiers cleared
- Multiple games → no modifier leaks between sessions
- Verify static list is gone

**Migration Validation:**
- Verify `IHunterWinConditionModifier` deleted
- Verify `HunterRole.winconModifiers` removed
- Verify no roles call `broadcastPlayerResultMessage()`
- Run ALL existing win condition tests (regression check)

### Test Count Estimate

- Unit tests: ~15
- Integration tests: ~5
- Migration validation: ~3

**Total: ~23 tests**

---

## Implementation Checklist

### Phase 1: Create New Abstractions
- [ ] Create `WinConditionModifier` interface
- [ ] Write unit tests for modifier interface (mock implementations)
- [ ] Enhance `WinConditionEvaluator` with modifier support
- [ ] Write unit tests for enhanced evaluator
- [ ] Update `GameSession` to expose evaluator (if not already accessible)

### Phase 2: Migrate SuperHunterRole
- [ ] Implement `WinConditionModifier` in `SuperHunterRole`
- [ ] Update `doStart()` to register with evaluator
- [ ] Remove `doStop()` modifier cleanup
- [ ] Write unit tests for SuperHunterRole modifier behavior
- [ ] Integration test: SuperHunter blocks win correctly

### Phase 3: Migrate Result Messaging
- [ ] Remove `broadcastPlayerResultMessage()` from `HunterRole`
- [ ] Add result messaging to `EndGameSaga`
- [ ] Write unit tests for EndGameSaga messaging
- [ ] Integration test: Role results broadcast correctly

### Phase 4: Remove Legacy Code
- [ ] Delete `IHunterWinConditionModifier.java`
- [ ] Remove `HunterRole.winconModifiers` static list
- [ ] Remove any other references to old modifier pattern
- [ ] Update imports across codebase

### Phase 5: Validation & Documentation
- [ ] Run full test suite (all tests pass)
- [ ] Memory leak validation (multiple games, no leaks)
- [ ] Update architecture documentation
- [ ] Update `REFACTORING_PROGRESS.md` (mark Priority 11 complete)

---

## Success Criteria

- ✅ Static `HunterRole.winconModifiers` list removed
- ✅ `IHunterWinConditionModifier` deleted
- ✅ Modifiers are session-scoped (no leaks)
- ✅ SuperHunterRole correctly blocks hunter wins until requirement met
- ✅ Role result messaging centralized in `EndGameSaga`
- ✅ All existing win condition tests pass
- ✅ All new tests pass (23 tests)
- ✅ No memory leaks across multiple game sessions
- ✅ Architecture supports future custom win conditions

---

## Future Extensibility

### Custom Win Conditions (Future)

The architecture supports adding custom win conditions without code changes:

```java
// Future: Custom condition via configuration
GameConfiguration config = GameConfiguration.builder()
    .addWinCondition(new TimeBasedWinCondition(30 * 60)) // 30 min time limit
    .build();
```

### Composite Win Conditions (Deferred)

While the design could support AND/OR composition, the modifier pattern is sufficient for current needs. Composite conditions can be added later if needed:

```java
// Future: If needed
new CompositeWinCondition.and(
    new AllSpeedrunnersDeadCondition(),
    new TimeExpiredCondition()
);
```

**Decision:** YAGNI - modifiers handle composition needs for now.

---

## Alternatives Considered

### Alternative 1: Two-Phase Evaluation

**Idea:** Keep evaluator simple, add separate `WinConditionGate` class.

**Rejected because:** More classes/complexity for minimal benefit. Evaluator already encapsulates win logic, enhancing it is more natural.

### Alternative 2: Modifier Registry Service

**Idea:** Global `WinConditionModifierRegistry` service with session filtering.

**Rejected because:** More indirection, lifecycle split across classes. Evaluator-bound modifiers are simpler and lifecycle is automatic.

### Alternative 3: Keep Static List, Add Cleanup

**Idea:** Keep static list but add proper cleanup logic.

**Rejected because:** Still a global leak risk, doesn't follow DDD principles. Evaluator-bound modifiers eliminate the anti-pattern entirely.

---

## References

- **PROBLEMS_PRIORITY_SUMMARY.md** - Priority 11 description
- **Priority 3 Implementation** - Existing WinCondition foundation
- **Priority 7 Implementation** - Registry pattern precedent
- **bounded-contexts-map.md** - Win Condition context definition

---

**Design Status:** ✅ Approved  
**Next Step:** Invoke `writing-plans` skill to create implementation plan
