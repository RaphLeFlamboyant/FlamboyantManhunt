# Priority 8: Explicit State Machine - Design Specification

**Date:** 2026-06-24  
**Priority:** 8  
**Status:** Design Approved  
**Estimated Effort:** 4-5 hours

## Overview

This design implements an explicit state machine for `GameSession` to replace the current implicit state management. The state machine will have two gameplay states (PREPARATION and ACTIVE) with validated transitions and event-driven phase changes.

### Problem Statement

Currently, game phases are implicit:
- No enum to represent current game state
- State transitions scattered across codebase
- No validation of legal transitions
- Difficult to test phase-specific behavior
- Poor debugging (can't inspect what phase game is in)

### Goals

1. Make game phases explicit with `GamePhase` enum
2. Add transition validation to prevent invalid state changes
3. Publish `PhaseChangedEvent` for audit trail
4. Trigger phase-specific behaviors via event handlers
5. Simplify game flow logic in `NewManhuntManager`

### Non-Goals

- Configuration phase (handled by launcher, outside game scope)
- Countdown phase (happens before session creation)
- ENDED state (session is destroyed when game ends)
- Post-game celebration/stats (outside game scope for now)

## Architecture Overview

### Core Components

1. **GamePhase enum** - Defines two gameplay states
2. **GameSession** - Enhanced with phase tracking and transition logic
3. **PhaseChangedEvent** - Domain event for phase transitions
4. **NewManhuntManager** - Handlers react to phase changes

### State Machine

```
[Session Created]
       ↓
  PREPARATION ──────→ ACTIVE ──────→ [Session Destroyed]
   (initial)      (only valid        (game ends)
                   transition)
```

**States:**
- **PREPARATION**: Roles assigned but dormant, no manhunt mechanics active
- **ACTIVE**: Manhunt begins, roles active, mechanics enabled

**Valid Transitions:**
- PREPARATION → ACTIVE (triggered by timer expiry or immediate mode)

**Invalid Transitions:**
- ACTIVE → PREPARATION (cannot go backwards)
- Same state transitions (PREPARATION → PREPARATION, ACTIVE → ACTIVE)

### Game Flow Modes

**Surprise Mode OFF:**
```
Create Session (PREPARATION)
       ↓
Immediately transitionTo(ACTIVE)
       ↓
Roles start, listeners register
       ↓
Play until game ends
```

**Surprise Mode ON:**
```
Create Session (PREPARATION)
       ↓
Players gather resources (vanilla gameplay)
       ↓
After roleRevealDelayInMinutes
       ↓
transitionTo(ACTIVE)
       ↓
Roles start, listeners register
       ↓
Play until game ends
```

## Component Design

### 1. GamePhase Enum

**Location:** `me.flamboyant.manhunt.domain.game.GamePhase`

```java
package me.flamboyant.manhunt.domain.game;

/**
 * Represents the current phase of a manhunt game session.
 */
public enum GamePhase {
    /**
     * Preparation phase - players gather resources without manhunt mechanics.
     * Roles are assigned but not active. No tracking, no role abilities.
     */
    PREPARATION,
    
    /**
     * Active phase - manhunt is in progress.
     * Roles are active, tracking enabled, all mechanics functional.
     * Game continues until dragon dies or all speedrunners die.
     */
    ACTIVE
}
```

**Characteristics:**
- Simple two-state enum
- JavaDoc explains each phase's purpose
- No behavior (just data)

---

### 2. GameSession Enhancements

**Location:** `me.flamboyant.manhunt.domain.game.GameSession`

**New Fields:**

```java
private GamePhase currentPhase = GamePhase.PREPARATION;

private static final Map<GamePhase, Set<GamePhase>> VALID_TRANSITIONS = Map.of(
    GamePhase.PREPARATION, Set.of(GamePhase.ACTIVE)
    // ACTIVE has no valid transitions - game ends instead
);
```

**New Methods:**

```java
/**
 * Get the current game phase.
 * 
 * @return current phase
 */
public GamePhase getCurrentPhase() {
    return currentPhase;
}

/**
 * Check if transition to target phase is valid from current phase.
 * 
 * @param targetPhase the phase to transition to
 * @return true if transition is allowed
 */
public boolean canTransitionTo(GamePhase targetPhase) {
    Set<GamePhase> validTargets = VALID_TRANSITIONS.get(currentPhase);
    return validTargets != null && validTargets.contains(targetPhase);
}

/**
 * Transition to a new game phase.
 * Validates the transition and publishes PhaseChangedEvent.
 * 
 * @param newPhase the phase to transition to
 * @throws IllegalStateException if transition is invalid
 */
public void transitionTo(GamePhase newPhase) {
    if (newPhase == null) {
        throw new IllegalArgumentException("New phase cannot be null");
    }
    
    if (!canTransitionTo(newPhase)) {
        throw new IllegalStateException(
            String.format("Invalid transition from %s to %s", currentPhase, newPhase)
        );
    }
    
    GamePhase oldPhase = currentPhase;
    currentPhase = newPhase;
    
    eventPublisher.publish(new PhaseChangedEvent(id, oldPhase, newPhase));
}
```

**Design Notes:**
- Initial phase is PREPARATION (set in field initializer)
- Transition validation uses static map for clarity
- `transitionTo()` is the only way to change phase (encapsulation)
- Event published after state change (new state already set when handlers react)

---

### 3. PhaseChangedEvent

**Location:** `me.flamboyant.manhunt.domain.event.PhaseChangedEvent`

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GamePhase;
import me.flamboyant.manhunt.domain.game.GameSessionId;

/**
 * Published when the game session transitions from one phase to another.
 * Allows handlers to react to phase changes and trigger phase-specific behavior.
 */
public class PhaseChangedEvent extends DomainEvent {
    private final GamePhase oldPhase;
    private final GamePhase newPhase;
    
    public PhaseChangedEvent(GameSessionId sessionId, GamePhase oldPhase, GamePhase newPhase) {
        super(sessionId);
        if (oldPhase == null) {
            throw new IllegalArgumentException("Old phase cannot be null");
        }
        if (newPhase == null) {
            throw new IllegalArgumentException("New phase cannot be null");
        }
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }
    
    public GamePhase getOldPhase() {
        return oldPhase;
    }
    
    public GamePhase getNewPhase() {
        return newPhase;
    }
    
    @Override
    public String toString() {
        return String.format("PhaseChangedEvent[session=%s, %s -> %s]", 
            getSessionId(), oldPhase, newPhase);
    }
}
```

**Design Notes:**
- Extends existing `DomainEvent` base class
- Immutable (no setters)
- Includes both old and new phase for handler context
- Null validation in constructor
- Useful `toString()` for debugging

---

### 4. NewManhuntManager Changes

**Location:** `me.flamboyant.manhunt.NewManhuntManager`

**New Handler Registration:**

```java
private void registerEventHandlers(GameSession session) {
    DomainEventPublisher publisher = session.getEventPublisher();
    
    // ... existing handlers ...
    
    // Phase change handler
    publisher.subscribe(PhaseChangedEvent.class, this::onPhaseChanged);
}
```

**New Handler Implementation:**

```java
/**
 * Handle phase changes by activating phase-specific behavior.
 * When entering ACTIVE phase: start roles, register listeners, reveal roles.
 */
private void onPhaseChanged(PhaseChangedEvent event) {
    if (event.getNewPhase() == GamePhase.ACTIVE) {
        // Start all roles
        for (AManhuntRole role : session.getAllRoles().values()) {
            role.start();
        }
        
        // Register Bukkit event listeners for manhunt mechanics
        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        
        // Publish roles revealed event
        session.notifyRolesRevealed();
    }
}
```

**Modified startGame() Method:**

```java
public boolean startGame(GameSession session, int roleRevealDelayInMinutes, boolean speedrunnerSurprise) {
    this.session = session;
    
    // Register domain event handlers for this session
    registerEventHandlers(session);
    
    // Count speedrunners
    session.setRemainingSpeedrunners(0);
    for (Player player : session.getPlayers()) {
        AManhuntRole role = session.getRole(player);
        if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            session.setRemainingSpeedrunners(session.getRemainingSpeedrunners() + 1);
        }
    }
    
    if (speedrunnerSurprise) {
        // Schedule transition to ACTIVE after preparation time
        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
            session.transitionTo(GamePhase.ACTIVE);
        }, (roleRevealDelayInMinutes * 60) * 20);
    } else {
        // Immediate transition to ACTIVE (no preparation phase)
        session.transitionTo(GamePhase.ACTIVE);
    }
    
    // Notify that game has started
    session.notifyGameStarted();
    
    return true;
}
```

**Key Changes:**
- Removed manual `role.start()` loop (now in phase handler)
- Removed conditional listener registration (now in phase handler)
- Replaced scattered logic with explicit `transitionTo()` calls
- Timer only schedules phase transition, not individual actions
- Speedrunner counting moved before phase transition

**Design Rationale:**
- Phase transition triggers all activation logic atomically
- Event handler centralizes ACTIVE phase entry behavior
- Clean separation: `startGame()` orchestrates, handler executes
- Timer responsibility reduced to single state transition

## Integration Points

### Backward Compatibility

**No breaking changes for:**
- External callers of `NewManhuntManager.startGame()` (signature unchanged)
- Domain events (only adds `PhaseChangedEvent`)
- Role implementations (still use `start()` and `stop()`)

**Internal changes:**
- `startGame()` implementation refactored (behavior unchanged from user perspective)
- Phase transition logic now centralized

### Event Flow

**Surprise Mode OFF:**
1. `startGame()` called
2. `registerEventHandlers()` subscribes to events
3. `transitionTo(ACTIVE)` called immediately
4. `PhaseChangedEvent(PREPARATION → ACTIVE)` published
5. `onPhaseChanged()` handler:
   - Starts all roles
   - Registers Bukkit listeners
   - Publishes `RolesRevealedEvent`
6. `GameStartedEvent` published
7. Game runs

**Surprise Mode ON:**
1. `startGame()` called
2. `registerEventHandlers()` subscribes to events
3. Timer scheduled for `roleRevealDelayInMinutes`
4. `GameStartedEvent` published
5. Players play vanilla Minecraft (PREPARATION phase)
6. Timer expires → `transitionTo(ACTIVE)` called
7. `PhaseChangedEvent(PREPARATION → ACTIVE)` published
8. `onPhaseChanged()` handler:
   - Starts all roles
   - Registers Bukkit listeners
   - Publishes `RolesRevealedEvent`
9. Game runs

### Dependencies

**GameSession depends on:**
- `GamePhase` enum (new)
- `PhaseChangedEvent` (new)
- Existing `DomainEventPublisher`

**NewManhuntManager depends on:**
- `GamePhase` enum (for handler logic)
- `PhaseChangedEvent` (for subscription)
- Existing domain events

**No new external dependencies.**

## Error Handling

### Invalid Transition Attempts

**Scenario:** Code tries to transition to invalid state

**Example:** `session.transitionTo(PREPARATION)` when already in ACTIVE

**Behavior:**
```java
throw new IllegalStateException("Invalid transition from ACTIVE to PREPARATION");
```

**Rationale:**
- This is a programming error, not user error
- Fail fast during development/testing
- No recovery needed (indicates broken game logic)

**Who catches it:**
- Not caught - let it propagate and terminate operation
- Will be caught during testing phase
- Logs will show stack trace for debugging

---

### Duplicate Transition Attempts

**Scenario:** `transitionTo(ACTIVE)` called when already ACTIVE

**Behavior:** Treated as invalid transition (throws `IllegalStateException`)

**Rationale:**
- Prevents accidental double-activation of roles/listeners
- Makes intent explicit (should check phase before transitioning)
- Idempotent operations should be handled by caller, not state machine

---

### Game Stopped During PREPARATION

**Scenario:** Admin calls `stopGame()` while in PREPARATION phase

**Current behavior preserved:**
```java
public void stopGame(String reason) {
    EntityDamageEvent.getHandlerList().unregister(this);
    Bukkit.broadcastMessage(ChatHelper.importantMessage(reason));
    
    for (AManhuntRole role : session.getAllRoles().values()) {
        role.stop();
    }
    
    session.clear();
    this.session = null;
}
```

**No changes needed:**
- Phase is informational, doesn't prevent cleanup
- Stopping game during PREPARATION is valid (cancels game)
- Pending scheduled transitions will be no-ops (session is null)

---

### Null Phase Validation

**Scenario:** `transitionTo(null)` called

**Behavior:**
```java
throw new IllegalArgumentException("New phase cannot be null");
```

**Rationale:**
- Fail fast on programmer error
- Null phase is never valid

---

### Phase Inspection for Debugging

**Use case:** Admin wants to know current game state

**Solution:** `session.getCurrentPhase()` returns current phase

**Future enhancements:**
- Add debug command `/manhunt phase` to show current phase
- Log phase transitions at INFO level
- Include phase in error messages

## Testing Strategy

### Unit Tests

**Test file:** `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionPhaseTest.java`

**Test cases:**

1. **testSessionStartsInPreparationPhase**
   ```java
   @Test
   void testSessionStartsInPreparationPhase() {
       GameSession session = new GameSession(GameSessionId.generate());
       assertEquals(GamePhase.PREPARATION, session.getCurrentPhase());
   }
   ```

2. **testValidTransitionFromPreparationToActive**
   ```java
   @Test
   void testValidTransitionFromPreparationToActive() {
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           new InMemoryEventPublisher()
       );
       
       session.transitionTo(GamePhase.ACTIVE);
       
       assertEquals(GamePhase.ACTIVE, session.getCurrentPhase());
   }
   ```

3. **testInvalidTransitionFromActiveToPreparation**
   ```java
   @Test
   void testInvalidTransitionFromActiveToPreparation() {
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           new InMemoryEventPublisher()
       );
       
       session.transitionTo(GamePhase.ACTIVE);
       
       assertThrows(IllegalStateException.class, () -> {
           session.transitionTo(GamePhase.PREPARATION);
       });
   }
   ```

4. **testCannotTransitionToSamePhase**
   ```java
   @Test
   void testCannotTransitionToSamePhase() {
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           new InMemoryEventPublisher()
       );
       
       assertThrows(IllegalStateException.class, () -> {
           session.transitionTo(GamePhase.PREPARATION);
       });
   }
   ```

5. **testCanTransitionToChecksValidTransitions**
   ```java
   @Test
   void testCanTransitionToChecksValidTransitions() {
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           new InMemoryEventPublisher()
       );
       
       assertTrue(session.canTransitionTo(GamePhase.ACTIVE));
       assertFalse(session.canTransitionTo(GamePhase.PREPARATION));
       
       session.transitionTo(GamePhase.ACTIVE);
       
       assertFalse(session.canTransitionTo(GamePhase.PREPARATION));
       assertFalse(session.canTransitionTo(GamePhase.ACTIVE));
   }
   ```

6. **testPhaseChangedEventPublished**
   ```java
   @Test
   void testPhaseChangedEventPublished() {
       InMemoryEventPublisher publisher = new InMemoryEventPublisher();
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           publisher
       );
       
       List<PhaseChangedEvent> events = new ArrayList<>();
       publisher.subscribe(PhaseChangedEvent.class, events::add);
       
       session.transitionTo(GamePhase.ACTIVE);
       
       assertEquals(1, events.size());
       PhaseChangedEvent event = events.get(0);
       assertEquals(GamePhase.PREPARATION, event.getOldPhase());
       assertEquals(GamePhase.ACTIVE, event.getNewPhase());
       assertEquals(session.getId(), event.getSessionId());
   }
   ```

7. **testTransitionToNullThrowsException**
   ```java
   @Test
   void testTransitionToNullThrowsException() {
       GameSession session = new GameSession(
           GameSessionId.generate(),
           new InMemoryPortalTracker(),
           new InMemoryEventPublisher()
       );
       
       assertThrows(IllegalArgumentException.class, () -> {
           session.transitionTo(null);
       });
   }
   ```

---

### Integration Tests

**Test file:** `src/test/java/me/flamboyant/manhunt/NewManhuntManagerPhaseTest.java`

**Test cases:**

1. **testSurpriseModeOffActivatesImmediately**
   ```java
   @Test
   void testSurpriseModeOffActivatesImmediately() {
       // Setup
       GameSession session = createSessionWithRoles();
       NewManhuntManager manager = createManager();
       
       // Execute
       manager.startGame(session, 5, false); // no surprise
       
       // Verify
       assertEquals(GamePhase.ACTIVE, session.getCurrentPhase());
       // Verify roles started (via mock)
       // Verify listeners registered (via mock)
   }
   ```

2. **testSurpriseModeOnStaysInPreparation**
   ```java
   @Test
   void testSurpriseModeOnStaysInPreparation() {
       // Setup
       GameSession session = createSessionWithRoles();
       NewManhuntManager manager = createManager();
       
       // Execute
       manager.startGame(session, 5, true); // with surprise
       
       // Verify
       assertEquals(GamePhase.PREPARATION, session.getCurrentPhase());
       // Verify roles NOT started yet
       // Verify listeners NOT registered yet
   }
   ```

3. **testPhaseChangeHandlerStartsRoles**
   ```java
   @Test
   void testPhaseChangeHandlerStartsRoles() {
       // Setup
       GameSession session = createSessionWithMockRoles();
       NewManhuntManager manager = createManager();
       manager.startGame(session, 5, true);
       
       // Execute - manually trigger transition (simulate timer)
       session.transitionTo(GamePhase.ACTIVE);
       
       // Verify
       // All roles received start() call
       // Listeners registered
       // RolesRevealedEvent published
   }
   ```

**Note:** Testing scheduled transitions requires Bukkit test framework or time manipulation. These tests verify immediate behavior; scheduled transition is covered by unit tests of the state machine itself.

---

### Test Coverage Goals

- **GamePhase enum**: 100% (trivial)
- **GameSession phase methods**: 100% (all branches tested)
- **PhaseChangedEvent**: 100% (simple data class)
- **NewManhuntManager phase handler**: 90%+ (mock-based verification)

## Implementation Checklist

### Phase 1: Core State Machine (TDD)
- [ ] Create `GamePhase` enum
- [ ] Write failing test: session starts in PREPARATION
- [ ] Add `currentPhase` field to `GameSession`
- [ ] Write failing test: valid transition PREPARATION → ACTIVE
- [ ] Implement `transitionTo()` method (no validation yet)
- [ ] Write failing test: invalid transition throws exception
- [ ] Implement transition validation with `VALID_TRANSITIONS` map
- [ ] Write failing test: `canTransitionTo()` checks validity
- [ ] Implement `canTransitionTo()` method
- [ ] Write test: null phase throws exception
- [ ] Add null validation to `transitionTo()`

### Phase 2: Phase Change Event (TDD)
- [ ] Write failing test: `PhaseChangedEvent` published on transition
- [ ] Create `PhaseChangedEvent` class
- [ ] Update `transitionTo()` to publish event
- [ ] Write test: event contains correct old/new phases
- [ ] Verify event implementation

### Phase 3: Manager Integration (TDD)
- [ ] Write failing test: phase handler starts roles
- [ ] Implement `onPhaseChanged()` handler in `NewManhuntManager`
- [ ] Register `PhaseChangedEvent` handler in `registerEventHandlers()`
- [ ] Write failing test: surprise OFF activates immediately
- [ ] Refactor `startGame()` to use `transitionTo()` for no-surprise mode
- [ ] Write test: surprise ON schedules transition
- [ ] Refactor `startGame()` to schedule `transitionTo()` for surprise mode
- [ ] Remove old manual role starting code
- [ ] Remove old manual listener registration code

### Phase 4: Verification & Documentation
- [ ] Run all tests (new + existing)
- [ ] Verify no regressions in `GameSessionTest`
- [ ] Manual testing in Minecraft:
  - [ ] Surprise OFF: roles activate immediately
  - [ ] Surprise ON: roles activate after timer
  - [ ] Both modes: game plays correctly
- [ ] Update `REFACTORING_PROGRESS.md` to mark Priority 8 complete
- [ ] Commit with message: "feat(domain): add explicit state machine (Priority 8)"

## File Changes Summary

### New Files
- `src/main/java/me/flamboyant/manhunt/domain/game/GamePhase.java`
- `src/main/java/me/flamboyant/manhunt/domain/event/PhaseChangedEvent.java`
- `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionPhaseTest.java`

### Modified Files
- `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`
  - Add `currentPhase` field
  - Add `VALID_TRANSITIONS` map
  - Add `getCurrentPhase()` method
  - Add `canTransitionTo()` method
  - Add `transitionTo()` method
- `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`
  - Add `onPhaseChanged()` handler
  - Register `PhaseChangedEvent` subscription in `registerEventHandlers()`
  - Refactor `startGame()` to use phase transitions
  - Remove manual role starting code
  - Remove conditional listener registration

### Documentation Files
- `docs/REFACTORING_PROGRESS.md` - Mark Priority 8 complete
- `docs/architecture/priority8-state-machine.md` - Architecture documentation (create after implementation)

## Future Enhancements

### Possible Additional States

If needed in the future, the state machine can easily be extended:

**PAUSED state:**
```java
enum GamePhase {
    PREPARATION, ACTIVE, PAUSED
}

// Valid transitions
ACTIVE → PAUSED
PAUSED → ACTIVE
```

**ENDED state:**
```java
enum GamePhase {
    PREPARATION, ACTIVE, ENDED
}

// Valid transitions
ACTIVE → ENDED
ENDED → PREPARATION (for rematch)
```

**DRAGON_FIGHT state:**
```java
enum GamePhase {
    PREPARATION, ACTIVE, DRAGON_FIGHT, ENDED
}

// Could enable special behaviors during final fight
```

### Phase-Specific Behavior

Once state machine exists, can add phase guards:

```java
public void someMethod() {
    if (currentPhase != GamePhase.ACTIVE) {
        throw new IllegalStateException("Can only do this during ACTIVE phase");
    }
    // ... implementation
}
```

### Phase Change Listeners

Could add observer pattern for external components:

```java
public interface GamePhaseListener {
    void onPhaseChange(GamePhase oldPhase, GamePhase newPhase);
}

// In GameSession
private List<GamePhaseListener> phaseListeners = new ArrayList<>();

public void addPhaseListener(GamePhaseListener listener) {
    phaseListeners.add(listener);
}
```

### Audit Trail

Could log all phase transitions:

```java
private void transitionTo(GamePhase newPhase) {
    // ... validation ...
    
    logger.info("Session {} transitioning from {} to {}", 
        id, currentPhase, newPhase);
    
    // ... rest of method
}
```

## Design Decisions Log

### Decision 1: Two States vs More States

**Options considered:**
- Two states (PREPARATION, ACTIVE)
- Four states (CONFIGURING, COUNTDOWN, PREPARATION, ACTIVE)
- Five states (add ENDED)

**Decision:** Two states (PREPARATION, ACTIVE)

**Rationale:**
- CONFIGURING happens before GameSession exists (launcher concern)
- COUNTDOWN happens before GameSession starts (5 seconds before creation)
- ENDED immediately destroys session (no need to model)
- YAGNI principle - start simple, extend if needed

---

### Decision 2: Where to Place Activation Logic

**Options considered:**
1. In `transitionTo()` method directly
2. In phase handler (`onPhaseChanged()`)
3. In phase objects (State Pattern)

**Decision:** Option 2 (phase handler)

**Rationale:**
- Keeps `GameSession` focused on state management, not side effects
- Follows existing event-driven architecture
- Easy to test (mock event handling)
- Activation logic (roles, listeners) belongs in manager, not aggregate

---

### Decision 3: Event Timing

**Options considered:**
1. Publish event before state change
2. Publish event after state change

**Decision:** After state change

**Rationale:**
- Handlers see consistent state (phase already changed)
- `getCurrentPhase()` returns new phase when handler runs
- Matches transaction semantics (change committed, then notify)

---

### Decision 4: Transition Validation Approach

**Options considered:**
1. Static map of valid transitions
2. Switch statement in `canTransitionTo()`
3. State Pattern with phase objects

**Decision:** Static map

**Rationale:**
- Declarative and easy to understand
- Easy to extend (add new states/transitions)
- Centralizes transition rules in one place
- No need for State Pattern complexity with only 2 states

---

### Decision 5: Initial State

**Options considered:**
1. No initial state (must call `transitionTo()` after construction)
2. Start in PREPARATION
3. Start in ACTIVE

**Decision:** Start in PREPARATION

**Rationale:**
- Sessions always begin before activation
- Consistent initialization (no null phase)
- Caller can immediately transition to ACTIVE if needed (surprise OFF)

## References

- **Priority 8 Problem Definition**: `docs/PROBLEMS_PRIORITY_SUMMARY.md` lines 371-418
- **Priority 8 Task Breakdown**: `docs/REFACTORING_PROGRESS.md` lines 491-567
- **Domain Events Design**: `docs/superpowers/specs/2026-06-19-domain-events-design.md`
- **GameSession Aggregate**: `docs/architecture/priority1-gamesession-aggregate.md`
- **Bounded Contexts**: `docs/superpowers/specs/2026-06-19-bounded-contexts-design.md`

## Success Criteria

- ✅ All game phases explicit (GamePhase enum)
- ✅ Transition validation prevents invalid states
- ✅ Phase-specific behavior triggered via events
- ✅ Easy to add new phases (extensible design)
- ✅ State machine fully tested (unit + integration)
- ✅ No regressions in existing game flow
- ✅ Code is simpler and more maintainable than before
