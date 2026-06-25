# Event Listener Lifecycle Management - Design Specification

**Priority:** 9  
**Date:** 2026-06-25  
**Status:** Design Approved  
**Effort Estimate:** 3-4 hours

---

## Problem Statement

Roles currently manually register and unregister Bukkit event listeners, leading to:
- **Inconsistent patterns** across 16+ role implementations
- **Boilerplate code** in every `doStart()` and `doStop()` method
- **Memory leak risk** if unregistration is forgotten or fails
- **No centralized tracking** of which handlers are active for a session
- **Unclear ownership** of handler lifecycle management

Example of current problematic pattern:
```java
// SpeedrunnerRole.java
protected boolean doStart() {
    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    // ... role initialization
    return true;
}

protected boolean doStop() {
    EntityPortalEnterEvent.getHandlerList().unregister(this);
    EntityDamageEvent.getHandlerList().unregister(this);
    owner.setCooldown(Material.COMPASS, 0);
    return true;
}
```

This pattern is repeated across all role classes with slight variations, making it error-prone and hard to audit for correctness.

---

## Goals

### Primary Goals
1. **Centralize handler lifecycle management** - Single source of truth for Bukkit event handler registration/unregistration
2. **Automatic cleanup** - Handlers automatically unregistered when GameSession ends
3. **Simplify roles** - Remove manual registration/unregistration boilerplate from all role classes
4. **Prevent memory leaks** - Guarantee no handlers remain after session ends
5. **Session isolation** - Multiple concurrent games have independent handler lifecycles

### Non-Goals
- ❌ Configuration phase handler management (FlamboyantTools responsibility)
- ❌ Domain event handler management (already handled by InMemoryEventPublisher)
- ❌ Changing how roles define event handlers (still use @EventHandler annotations)
- ❌ Automatic listener discovery via reflection (explicit registration preferred)

---

## Context & Constraints

### Current Architecture
- **FlamboyantTools** (external dependency) manages configuration phase handlers
- **EventHandlerRegistrationService** already exists for application-level handler registration
- **HandlerRegistration** value object tracks registered listeners
- **GameSession** is the aggregate root with clear lifecycle (PREPARATION → ACTIVE → END)
- **Roles** implement Bukkit `Listener` interface and use `@EventHandler` annotations

### Lifecycle Phases
1. **Configuration Phase** (pre-game) - Launcher UI, parameters, player selection. **No GameSession exists.**
2. **Game Creation** - GameSession created with GamePhase.PREPARATION
3. **Game Active** - Roles start (GamePhase.ACTIVE), handlers process events
4. **Game End** - GameSession.end() called, cleanup triggered

### Key Constraint
Configuration handler management is separate and handled by FlamboyantTools. Our design focuses **only on session-scoped handlers** (roles and game-related Bukkit listeners).

---

## Solution Overview

**Approach:** Extend the existing `EventHandlerRegistrationService` to manage role handler lifecycles.

**Core Principle:** Session-scoped handlers live and die with the GameSession. When `GameSession.end()` is called, all associated Bukkit event handlers are automatically unregistered.

**Key Design Decision:** The `EventHandlerRegistrationService` becomes the single source of truth for Bukkit handler lifecycle. Roles implement `Listener` but never call `registerEvents()` or `unregister()` themselves.

**Lifecycle Flow:**
```
Game Start → Register all role handlers → Store HandlerRegistration in GameSession
    ↓
Game Running → Roles handle events
    ↓
Game End → GameSession.end() → Unregister all handlers → Clean session state
```

---

## Architecture

### Component Responsibilities

**EventHandlerRegistrationService (Application Layer)**
- Registers Bukkit event handlers with Bukkit's PluginManager
- Returns `HandlerRegistration` tracking object
- Unregisters handlers when given a `HandlerRegistration`
- Already exists - no changes needed

**GameSession (Domain Layer)**
- Stores its `HandlerRegistration` reference
- Triggers handler cleanup in `end()` method
- Maintains session lifecycle integrity

**StartGameSaga (Application Layer)**
- Collects all role listeners after role assignment
- Registers handlers via `EventHandlerRegistrationService`
- Stores registration in GameSession

**GameLifecycleService (Application Layer)**
- Orchestrates session end flow
- Calls unregisterHandlers before session cleanup
- Ensures "end before remove" invariant

**AManhuntRole and Subclasses (Domain Layer)**
- Implement `Listener` interface
- Define `@EventHandler` methods
- **No longer** call registerEvents() or unregister()
- Simplified `doStart()` and `doStop()` methods

### Dependency Flow

```
StartGameSaga
    ↓ uses
EventHandlerRegistrationService
    ↓ creates
HandlerRegistration
    ↓ stored in
GameSession
    ↓ retrieved by
GameLifecycleService
    ↓ uses
EventHandlerRegistrationService.unregisterHandlers()
```

**Key Insight:** Application services orchestrate infrastructure concerns. The domain (GameSession, roles) remains focused on game logic.

---

## Detailed Design

### 1. GameSession Enhancements

**New Field:**
```java
private HandlerRegistration handlerRegistration;
```

**New Methods:**
```java
public void setHandlerRegistration(HandlerRegistration registration) {
    this.handlerRegistration = registration;
}

public HandlerRegistration getHandlerRegistration() {
    return handlerRegistration;
}
```

**Enhanced end() Method:**
```java
public void end() {
    // Note: Actual unregistration happens in GameLifecycleService
    // GameSession just holds the registration reference
    eventPublisher.unsubscribeAll();
    clear();
}
```

**Design Note:** GameSession doesn't directly call the service to avoid domain → application coupling. The service retrieves the registration and performs cleanup.

---

### 2. EventHandlerRegistrationService

**Current Implementation (No Changes Needed):**
```java
@Singleton
public class EventHandlerRegistrationService {
    private final Plugin plugin;
    private final DomainEventPublisher eventPublisher;

    public HandlerRegistration registerHandlers(GameSessionId sessionId, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        eventPublisher.publish(new HandlersRegisteredEvent(sessionId));
        return new HandlerRegistration(sessionId, Arrays.asList(listeners));
    }

    public void unregisterHandlers(HandlerRegistration registration) {
        if (registration == null) {
            return; // Idempotent
        }
        for (Listener listener : registration.getListeners()) {
            HandlerList.unregisterAll(listener);
        }
    }
}
```

**Status:** Already correctly designed for our needs. The service is stateless, idempotent, and injectable.

---

### 3. StartGameSaga Enhancement

**New Logic After Role Assignment:**
```java
public GameSessionId startGame(StartGameCommand command) throws GameStartException {
    try {
        // Existing: Create session
        GameSessionId sessionId = lifecycleService.createSession();
        GameSession session = lifecycleService.getSession(sessionId);
        
        // Existing: Distribute and assign roles
        DistributeRolesCommand distributeCommand = DistributeRolesCommand.create(
            sessionId, command.getPlayers(), config
        );
        Map<Player, ManhuntRoleIdentifier> playerRoles = 
            roleDistributionService.distributeRoles(distributeCommand);
        
        AssignRolesCommand assignCommand = AssignRolesCommand.create(sessionId, playerRoles);
        roleAssignmentService.assignRoles(assignCommand);
        
        // NEW: Collect all role listeners
        Collection<AManhuntRole> roles = session.getAllRoles().values();
        List<Listener> roleListeners = new ArrayList<>();
        for (AManhuntRole role : roles) {
            if (role instanceof Listener) {
                roleListeners.add((Listener) role);
            }
        }
        
        // NEW: Register all handlers at once
        HandlerRegistration registration = eventHandlerRegistrationService.registerHandlers(
            sessionId,
            roleListeners.toArray(new Listener[0])
        );
        
        // NEW: Store registration in session for cleanup
        session.setHandlerRegistration(registration);
        
        // Existing: Register event handlers for win conditions
        eventHandlerRegistrationService.registerEventHandlers(sessionId);
        
        // Existing: Transition to ACTIVE phase and start roles
        session.transitionTo(GamePhase.ACTIVE);
        
        for (AManhuntRole role : roles) {
            role.start();
        }
        
        session.notifyGameStarted();
        return sessionId;
        
    } catch (Exception e) {
        // Existing: Compensation logic
        throw new GameStartException(e.getMessage(), e, CompensationStatus.UNKNOWN);
    }
}
```

**Key Points:**
- Collect listeners after assignment (roles exist)
- Register before calling `role.start()` (handlers active when roles need them)
- Store registration for cleanup
- instanceof check allows non-listener roles (future flexibility)

---

### 4. GameLifecycleService Enhancement

**Enhanced endSession() Method:**
```java
public void endSession(GameSessionId sessionId) {
    GameSession session = sessions.get(sessionId);
    if (session == null) {
        return; // Idempotent
    }
    
    // Stop roles first (clears role-specific state)
    for (AManhuntRole role : session.getAllRoles().values()) {
        role.stop();
    }
    
    // NEW: Unregister Bukkit handlers before session cleanup
    HandlerRegistration registration = session.getHandlerRegistration();
    if (registration != null) {
        eventHandlerRegistrationService.unregisterHandlers(registration);
    }
    
    // Clean up session (domain event handlers + state)
    session.end();
    
    // Remove from manager
    sessions.remove(sessionId);
    
    eventPublisher.publish(new GameSessionDestroyedEvent(sessionId));
}
```

**Order Matters:**
1. Stop roles (clear role state, cooldowns, etc.)
2. Unregister Bukkit handlers (prevent events from reaching stopped roles)
3. End session (cleanup domain event handlers)
4. Remove from manager (session no longer accessible)

---

### 5. AManhuntRole Simplification

**Before (SpeedrunnerRole):**
```java
protected boolean doStart() {
    // Manual registration
    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    
    // Role initialization
    session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
    if (session == null) {
        return false;
    }
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);
    trackView = new PlayerSelectionView(/* ... */);
    return true;
}

protected boolean doStop() {
    // Manual unregistration
    EntityPortalEnterEvent.getHandlerList().unregister(this);
    EntityDamageEvent.getHandlerList().unregister(this);
    
    owner.setCooldown(Material.COMPASS, 0);
    return true;
}
```

**After (Simplified):**
```java
protected boolean doStart() {
    // No manual registration - handled by saga
    
    // Role initialization only
    session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
    if (session == null) {
        return false;
    }
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);
    trackView = new PlayerSelectionView(/* ... */);
    return true;
}

protected boolean doStop() {
    // No manual unregistration - handled by lifecycle service
    
    // Cleanup role-specific state only
    owner.setCooldown(Material.COMPASS, 0);
    return true;
}
```

**Impact:** This pattern applies to all ~16 role subclasses, removing 2-4 lines of boilerplate per role.

---

## Data Flow Sequences

### Sequence 1: Game Start

```
User clicks start
    ↓
NewManhuntLauncher.start()
    ↓
StartGameSaga.startGame(command)
    ↓
GameLifecycleService.createSession()
    → GameSession created with PREPARATION phase
    ↓
RoleDistributionService.distributeRoles()
    → Returns Map<Player, RoleIdentifier>
    ↓
RoleAssignmentService.assignRoles()
    → Creates role instances
    → Calls session.assignRole() for each
    ↓
StartGameSaga collects role listeners
    → Filters roles by instanceof Listener
    → Builds Listener[] array
    ↓
EventHandlerRegistrationService.registerHandlers(sessionId, listeners)
    → Registers with Bukkit PluginManager
    → Returns HandlerRegistration
    ↓
session.setHandlerRegistration(registration)
    → Stores for later cleanup
    ↓
session.transitionTo(ACTIVE)
    → Publishes PhaseChangedEvent
    ↓
For each role: role.start()
    → Calls doStart() (no manual registration)
    → Initializes role-specific state
    ↓
session.notifyGameStarted()
    → Game active, handlers responding to events
```

### Sequence 2: Game End (Normal)

```
Win condition met (e.g., dragon killed)
    ↓
NewManhuntManager detects win
    ↓
EndGameSaga.endGame(command)
    ↓
WinConditionEvaluator.evaluate(session)
    → Determines outcome (SPEEDRUNNERS_WIN, etc.)
    ↓
EndGameSaga publishes results
    ↓
GameLifecycleService.endSession(sessionId)
    ↓
For each role: role.stop()
    → Calls doStop() (no manual unregistration)
    → Clears cooldowns, state
    ↓
eventHandlerRegistrationService.unregisterHandlers(registration)
    → Calls HandlerList.unregisterAll() for each listener
    → Handlers no longer receive events
    ↓
session.end()
    → eventPublisher.unsubscribeAll() (domain events)
    → session.clear() (portals, roles)
    ↓
sessions.remove(sessionId)
    → Session destroyed
    ↓
GameSessionDestroyedEvent published
```

### Sequence 3: Multiple Concurrent Games

```
Game 1 starts
    → Session A created
    → Roles A1, A2, A3 registered
    → HandlerRegistration A stored
    ↓
Game 2 starts (concurrent)
    → Session B created
    → Roles B1, B2, B3 registered
    → HandlerRegistration B stored
    ↓
Event occurs (e.g., player damage)
    → Bukkit dispatches to ALL registered handlers
    → Roles A1-A3 receive (check if their owner)
    → Roles B1-B3 receive (check if their owner)
    → Each role filters by owner
    ↓
Game 1 ends
    → unregisterHandlers(Registration A)
    → Roles A1-A3 no longer receive events
    → Roles B1-B3 still active
    ↓
Game 2 continues
    → Only B handlers active
    ↓
Game 2 ends
    → unregisterHandlers(Registration B)
    → No handlers remain
```

---

## Error Handling & Edge Cases

### 1. Partial Registration Failure

**Scenario:** `registerHandlers()` fails after registering some listeners but not all.

**Current Behavior:** Bukkit throws exception if registration fails.

**Handling:**
- Exception propagates to `StartGameSaga`
- Saga compensation logic triggers
- Session is destroyed (calls `endSession()`)
- Already-registered listeners cleaned up by `unregisterHandlers()`

**Decision:** No special handling needed - existing compensation covers this.

---

### 2. Cleanup During Active Game

**Scenario:** `GameSession.end()` called while Bukkit events are being processed.

**Bukkit Guarantees:**
- `HandlerList.unregisterAll()` is thread-safe
- In-flight events may still reach handlers during unregistration
- After unregistration completes, no new events dispatched

**Role Defense:**
- Roles check `isRunning()` flag before processing critical operations
- Handlers that fire after `stop()` exit early

**Example:**
```java
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (!isRunning()) return; // Exit if role stopped
    if (owner != event.getPlayer()) return;
    // ... handle event
}
```

**Decision:** Rely on existing `isRunning()` checks in roles. No additional synchronization needed.

---

### 3. Multiple end() Calls

**Scenario:** `GameSession.end()` called multiple times (e.g., bug in calling code).

**Safety Mechanisms:**
- `eventHandlerRegistrationService.unregisterHandlers()` checks for null (idempotent)
- `eventPublisher.unsubscribeAll()` is idempotent (clears already-empty map)
- `session.clear()` is idempotent (clears already-empty collections)

**Decision:** Already safe - no changes needed.

---

### 4. Role Without Listeners

**Scenario:** A role class doesn't implement `Listener` interface.

**Handling:**
```java
if (role instanceof Listener) {
    roleListeners.add((Listener) role);
}
```

**Behavior:** Role is skipped during registration.

**Decision:** Expected and correct - not all roles need Bukkit events.

---

### 5. HandlerRegistration is null

**Scenario:** Game ends before handlers were registered (early failure during start).

**Safety Mechanisms:**
```java
if (registration != null) {
    eventHandlerRegistrationService.unregisterHandlers(registration);
}
```

**Decision:** Null check prevents NPE. Safe to proceed with rest of cleanup.

---

### 6. Session Removed Without Calling end()

**Scenario:** Bug causes `sessions.remove(sessionId)` without calling `session.end()`.

**Prevention:**
- All session removal paths go through `GameLifecycleService.endSession()`
- endSession() enforces "cleanup before remove" invariant

**Detection:**
- Add logging in endSession() to verify cleanup steps
- Integration tests verify handlers are unregistered

**Mitigation (if it happens):**
- Handlers remain registered but reference dead session
- Handlers lookup session via GameSessionManager (returns null)
- Handlers exit early when session not found
- Memory leak: HandlerRegistration and Listener instances remain

**Decision:** Enforce invariant via architecture. Make endSession() the ONLY way to destroy a session.

---

### 7. Exception During Unregistration

**Scenario:** `HandlerList.unregisterAll()` throws exception.

**Handling:**
- Exception propagates up to `GameLifecycleService.endSession()`
- Remaining cleanup steps (session.end(), remove) might be skipped
- Partial cleanup state

**Decision:**
```java
try {
    eventHandlerRegistrationService.unregisterHandlers(registration);
} catch (Exception e) {
    Bukkit.getLogger().severe("Failed to unregister handlers: " + e.getMessage());
    e.printStackTrace();
    // Continue with cleanup anyway
}
```

Add try-catch in endSession() to ensure cleanup continues even if unregistration fails.

---

## Testing Strategy

### Unit Tests

**GameSessionTest (enhanced):**
```java
@Test
public void testSetHandlerRegistration() {
    HandlerRegistration registration = mock(HandlerRegistration.class);
    session.setHandlerRegistration(registration);
    assertEquals(registration, session.getHandlerRegistration());
}

@Test
public void testEndWithNullHandlerRegistration() {
    session.setHandlerRegistration(null);
    session.end(); // Should not throw
}

@Test
public void testEndMultipleTimes() {
    session.end();
    session.end(); // Should be idempotent
}
```

**EventHandlerRegistrationServiceTest (enhanced):**
```java
@Test
public void testRegisterMultipleRoleListeners() {
    Listener role1 = mock(Listener.class);
    Listener role2 = mock(Listener.class);
    
    HandlerRegistration registration = service.registerHandlers(
        sessionId,
        role1, role2
    );
    
    assertNotNull(registration);
    assertEquals(2, registration.getListeners().size());
}

@Test
public void testUnregisterNullRegistration() {
    service.unregisterHandlers(null); // Should not throw
}
```

**GameLifecycleServiceTest (enhanced):**
```java
@Test
public void testEndSessionUnregistersHandlers() {
    GameSession session = service.createSession();
    HandlerRegistration registration = mock(HandlerRegistration.class);
    session.setHandlerRegistration(registration);
    
    service.endSession(session.getId());
    
    verify(eventHandlerService).unregisterHandlers(registration);
}

@Test
public void testEndSessionWithNullRegistration() {
    GameSession session = service.createSession();
    session.setHandlerRegistration(null);
    
    service.endSession(session.getId()); // Should not throw
}
```

---

### Integration Tests

**RoleHandlerLifecycleIntegrationTest (new):**
```java
@Test
public void testRoleHandlersRegisteredAndUnregistered() {
    // Setup: Create mock roles implementing Listener
    TestRole role1 = new TestRole(player1);
    TestRole role2 = new TestRole(player2);
    
    // Start game (saga registers handlers)
    StartGameCommand command = StartGameCommand.create(
        Arrays.asList(player1, player2),
        config
    );
    GameSessionId sessionId = startGameSaga.startGame(command);
    
    // Verify: Handlers are registered
    assertTrue(role1.isRegistered());
    assertTrue(role2.isRegistered());
    
    // Trigger event: Verify handlers receive it
    PlayerInteractEvent event = createMockEvent(player1);
    Bukkit.getPluginManager().callEvent(event);
    assertTrue(role1.receivedEvent());
    
    // End game
    EndGameCommand endCommand = new EndGameCommand(sessionId, "Test end");
    endGameSaga.endGame(endCommand);
    
    // Verify: Handlers are unregistered
    assertFalse(role1.isRegistered());
    assertFalse(role2.isRegistered());
    
    // Trigger event again: Verify handlers DON'T receive it
    role1.resetEventFlag();
    Bukkit.getPluginManager().callEvent(event);
    assertFalse(role1.receivedEvent());
}

@Test
public void testEarlyGameEndCleansUpPartialRegistration() {
    // Setup: Game start that will fail after partial registration
    // (This tests compensation logic)
    
    // Verify: All handlers cleaned up despite failure
}
```

**MultipleSessionLifecycleTest (new):**
```java
@Test
public void testConcurrentSessionsHaveIsolatedHandlers() {
    // Start game 1
    GameSessionId session1 = startGameSaga.startGame(command1);
    
    // Start game 2 (concurrent)
    GameSessionId session2 = startGameSaga.startGame(command2);
    
    // Trigger event
    PlayerInteractEvent event = createMockEvent(player1);
    Bukkit.getPluginManager().callEvent(event);
    
    // Verify: Both sessions' handlers receive event
    // (filtered by owner)
    
    // End game 1
    endGameSaga.endGame(new EndGameCommand(session1, "End 1"));
    
    // Trigger event
    Bukkit.getPluginManager().callEvent(event);
    
    // Verify: Only game 2 handlers receive event
    
    // End game 2
    endGameSaga.endGame(new EndGameCommand(session2, "End 2"));
    
    // Verify: No handlers remain
}

@Test
public void testSequentialGamesDoNotLeakHandlers() {
    for (int i = 0; i < 5; i++) {
        GameSessionId sessionId = startGameSaga.startGame(command);
        // ... play game
        endGameSaga.endGame(new EndGameCommand(sessionId, "End " + i));
        
        // Verify: Handler count doesn't grow
        assertEquals(0, getRegisteredHandlerCount());
    }
}
```

---

### Manual Testing Checklist

**Basic Flow:**
- [ ] Start game with 4 players
- [ ] Play normally (compass, damage, portals work)
- [ ] End game via win condition
- [ ] Check logs - no errors
- [ ] Repeat 3 times - verify no accumulation

**Edge Cases:**
- [ ] Start game, immediately stop - no leaks
- [ ] Start game, kill process mid-game, restart - next game works
- [ ] Start 2 concurrent games - both work independently
- [ ] End game 1 while game 2 active - game 2 unaffected

**Memory Monitoring:**
- [ ] Start game, note Bukkit handler list size
- [ ] End game, check handler list size (should return to baseline)
- [ ] Repeat 10 times, check handler list size (should not grow)
- [ ] Use Java profiling to check for Listener leaks

---

## Migration Strategy

### Phase 1: Foundation (No Breaking Changes)

**Goal:** Add infrastructure without changing behavior.

**Tasks:**
1. Add `handlerRegistration` field to GameSession
2. Add `setHandlerRegistration()` and `getHandlerRegistration()` methods
3. Enhance `GameLifecycleService.endSession()` to call unregisterHandlers
4. Add try-catch around unregisterHandlers for safety
5. Add unit tests for new GameSession methods

**Verification:** All existing tests pass. No behavior change yet.

**Estimated Time:** 30 minutes

---

### Phase 2: Integration (Wire Up Saga)

**Goal:** Make saga register handlers and store registration.

**Tasks:**
1. Add handler collection logic to `StartGameSaga.startGame()`
2. Call `eventHandlerRegistrationService.registerHandlers()`
3. Store registration in GameSession
4. Add integration test for registration flow
5. Add logging to verify handlers are registered

**Verification:** 
- Handlers registered twice (saga + roles) - both work
- Game functions normally
- Logs show handler registration

**Estimated Time:** 1 hour

---

### Phase 3: Role Cleanup (Remove Boilerplate)

**Goal:** Remove manual registration/unregistration from roles.

**Tasks:**
1. Remove `Common.server.getPluginManager().registerEvents()` from all role `doStart()` methods
2. Remove `HandlerList.unregister()` calls from all role `doStop()` methods
3. Update role tests if they verify registration behavior
4. Run full test suite

**Affected Files:** ~16 role classes

**Verification:**
- All tests pass
- Game functions normally
- Handlers only registered once (via saga)

**Estimated Time:** 1 hour

---

### Phase 4: Verification & Documentation

**Goal:** Ensure no regressions and document changes.

**Tasks:**
1. Run all unit tests
2. Run all integration tests
3. Manual testing checklist
4. Memory leak verification (10+ game cycles)
5. Update REFACTORING_PROGRESS.md
6. Update architecture documentation

**Verification:**
- All tests green
- No memory leaks detected
- Documentation reflects new design

**Estimated Time:** 1 hour

---

### Rollback Strategy

**Safe Rollback Point:** After Phase 2

If issues arise in Phase 3:
- Roles have both manual registration AND saga registration (redundant but safe)
- Can rollback Phase 3 changes without breaking Phase 2
- Handlers continue to work via manual registration

**How to Rollback Phase 3:**
```bash
git revert <phase-3-commits>
```

Game continues to work with both registration paths active.

---

## Impact Analysis

### Files Modified

**Domain Layer (4 files + 16 role subclasses):**
- `GameSession.java` - Add handlerRegistration field and methods
- `AManhuntRole.java` - No code changes (documentation only)
- `SpeedrunnerRole.java` - Remove registration/unregistration
- `HunterRole.java` - Remove registration/unregistration
- `SuperHunterRole.java` - Remove registration/unregistration
- ... (13 more role classes) - Remove registration/unregistration

**Application Layer (2 files):**
- `StartGameSaga.java` - Add handler collection and registration logic
- `GameLifecycleService.java` - Add unregisterHandlers call

**Test Layer (4 files modified + 2 new):**
- `GameSessionTest.java` - Add tests for handlerRegistration field
- `GameLifecycleServiceTest.java` - Add tests for cleanup flow
- `EventHandlerRegistrationServiceTest.java` - Add tests for multiple listeners
- `StartGameSagaTest.java` - Update for new registration logic
- `RoleHandlerLifecycleIntegrationTest.java` (new) - End-to-end tests
- `MultipleSessionLifecycleTest.java` (new) - Concurrent session tests

**Total:** ~24 files modified, 2 new test files

---

### Code Complexity Impact

**Before (per role):**
```java
// doStart() - 3 lines of boilerplate
Common.server.getPluginManager().registerEvents(this, Common.plugin);

// doStop() - 2-4 lines of boilerplate per event type
EntityPortalEnterEvent.getHandlerList().unregister(this);
EntityDamageEvent.getHandlerList().unregister(this);
```

**After (per role):**
```java
// doStart() - 0 lines of boilerplate
// (registration handled externally)

// doStop() - 0 lines of boilerplate
// (unregistration handled externally)
```

**Net Change:** 
- Remove 5-7 lines per role × 16 roles = **80-112 lines removed**
- Add ~20 lines in StartGameSaga
- Add ~10 lines in GameLifecycleService
- Net reduction: **50-82 lines of code**

**Complexity Reduction:**
- Manual lifecycle management spread across 16 classes → Centralized in 2 classes
- Inconsistent patterns → Single consistent pattern
- Error-prone manual cleanup → Automatic guaranteed cleanup

---

## Success Criteria

### Functional Requirements

✅ **All role Bukkit event handlers automatically unregistered when game ends**
- Verified by integration tests
- Manual verification: handlers don't fire after game end

✅ **Multiple consecutive games don't accumulate listeners**
- Verified by sequential game test (10+ iterations)
- Handler count returns to baseline after each game

✅ **Concurrent games have isolated handler lifecycles**
- Verified by concurrent session test
- Ending one game doesn't affect other active games

✅ **Roles simplified by removing manual registration/unregistration**
- 16 role classes simplified
- 50-82 lines of boilerplate removed

✅ **No memory leaks detectable after 10+ game sessions**
- Manual testing with memory profiler
- Handler list size doesn't grow over time

---

### Non-Functional Requirements

✅ **No breaking changes to existing game functionality**
- All existing tests pass
- Game mechanics unchanged (compass, damage, portals work)

✅ **All existing tests pass**
- Unit tests: 69+ existing + 10 new = 79+ total
- Integration tests: Existing + 2 new test classes

✅ **New integration tests cover lifecycle scenarios**
- Handler registration/unregistration
- Concurrent sessions
- Sequential sessions

✅ **Code complexity reduced**
- 50-82 lines removed
- Manual lifecycle management centralized
- Consistent pattern across all roles

✅ **Clear ownership: application services manage infrastructure concerns**
- EventHandlerRegistrationService owns Bukkit handler lifecycle
- GameLifecycleService orchestrates cleanup
- Roles focus on game logic only

---

### Anti-Goals (Confirmed Out of Scope)

❌ Configuration phase handler management (FlamboyantTools responsibility)  
❌ Domain event handler management (already handled by InMemoryEventPublisher)  
❌ Changing how roles define their event handlers (still use @EventHandler)  
❌ Adding automatic listener discovery via reflection (explicit registration)

---

## Open Questions

**Q1:** Should we add a debug command to list all registered handlers?

**A1:** Out of scope for this priority. Could be added later as development tooling.

---

**Q2:** Should we track which handlers are registered per session for auditing?

**A2:** The `HandlerRegistration` object already tracks listeners. If detailed auditing is needed, enhance the logging in `registerHandlers()` and `unregisterHandlers()`.

---

**Q3:** What happens if a role is added to a session after game start?

**A3:** Current design doesn't support dynamic role addition. If this becomes a requirement, would need to:
- Add `GameSession.addRoleListener(Listener)` method
- Track individual role registrations separately
- Out of scope for Priority 9

---

## References

- **Priority Document:** `docs/PROBLEMS_PRIORITY_SUMMARY.md` - Priority 9
- **Progress Tracker:** `docs/REFACTORING_PROGRESS.md` - Priority 9 section
- **Existing Architecture:**
  - `EventHandlerRegistrationService` - Current implementation
  - `HandlerRegistration` - Value object
  - `GameSession` - Aggregate root
  - `StartGameSaga` - Game start orchestration
  - `GameLifecycleService` - Session lifecycle management

---

## Appendix A: Bukkit Handler Lifecycle

Bukkit's event system works as follows:

1. **Registration:** `PluginManager.registerEvents(Listener, Plugin)` adds listener to internal HandlerList
2. **Event Dispatch:** When event occurs, Bukkit iterates registered listeners and calls matching @EventHandler methods
3. **Unregistration:** `HandlerList.unregisterAll(Listener)` removes listener from all event types
4. **Thread Safety:** Registration/unregistration is thread-safe, but in-flight events may still reach handlers during unregistration

**Key Insight:** Bukkit doesn't track "which plugin registered this listener" for cleanup. If we don't unregister, handlers remain active forever, even after game ends.

---

## Appendix B: Alternative Approaches Considered

### Alternative 1: Roles Return Listeners (Declarative)

**Approach:** Roles implement `getListeners()` method returning list of listeners.

**Pros:** Roles are declarative (say what they need, not how to register)

**Cons:** 
- Roles must implement new method
- Most roles just return `Collections.singletonList(this)`
- Extra layer of indirection for no benefit

**Decision:** Rejected - Too much ceremony for marginal benefit. instanceof check is simpler.

---

### Alternative 2: Create Dedicated RoleEventManager

**Approach:** New domain service tracks role → listeners mapping.

**Pros:** Clear domain responsibility for role events

**Cons:**
- Duplicates functionality of existing EventHandlerRegistrationService
- Adds new component to maintain
- Doesn't fit current architecture (application services manage infrastructure)

**Decision:** Rejected - Violates DRY, adds unnecessary complexity.

---

### Alternative 3: Annotation-Based Auto-Registration

**Approach:** Scan roles for @EventHandler annotations and auto-register via reflection.

**Pros:** Fully automatic, zero boilerplate

**Cons:**
- Reflection overhead
- Hidden registration (harder to debug)
- Breaks explicit-is-better-than-implicit principle
- Roles still implement Listener, so instanceof check works anyway

**Decision:** Rejected - Over-engineered for the problem. Explicit registration via instanceof is clear and simple.

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| 2026-06-25 | Claude Sonnet 4.5 | Initial design specification |
