# Task 8: Handler Registration in NewManhuntManager - Implementation Report

## Status: DONE

## Commits
**Range:** e86f44a..5e7dcb7
- `5e7dcb7` - feat(domain): add event handler registration to NewManhuntManager (Priority 5)

## Implementation Summary

Successfully implemented Task 8 by adding event handler registration to `NewManhuntManager`. The manager now subscribes to domain events published by `GameSession` and reacts appropriately to game lifecycle changes.

### Changes Made

**File Modified:** `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

1. **Added Imports**
   - `import me.flamboyant.manhunt.domain.event.*;`

2. **Added Private Method: `registerEventHandlers(GameSession session)`**
   - Subscribes to four event types using the session's event publisher
   - Registers method references for each handler
   - Session-scoped handlers (auto-cleanup on session end)

3. **Added Event Handler Methods (4 handlers):**

   a. `onSpeedrunnerDied(SpeedrunnerDiedEvent event)`
   - Evaluates all win conditions using `winConditionEvaluator`
   - If conditions met, calls `session.notifyGameEnded()` with outcome and message

   b. `onDragonKilled(DragonKilledEvent event)`
   - Marks dragon killed in `dragonKilledCondition`
   - Evaluates win conditions
   - If conditions met, calls `session.notifyGameEnded()` with appropriate message
   - Message includes killer's name if available

   c. `onWinConditionMet(WinConditionMetEvent event)`
   - Broadcasts win outcome description using `ChatHelper.importantMessage()`

   d. `onGameEnded(GameEndedEvent event)`
   - Stops all roles in the session
   - Unregisters Bukkit event listeners
   - Broadcasts end reason using `ChatHelper.importantMessage()`
   - Calls `session.end()` to trigger cleanup (including `eventPublisher.unsubscribeAll()`)

4. **Modified `startGame()` Method**
   - Calls `registerEventHandlers(session)` immediately after `this.session = session;`
   - Calls `session.notifyGameStarted()` at the end before returning true

### Architecture Notes

**Event Flow:**
1. `startGame()` registers handlers, then publishes `GameStartedEvent`
2. Bukkit events (player death, dragon killed) will trigger domain events
3. Domain events flow through registered handlers
4. Handlers evaluate conditions and may trigger cascading events
5. `GameEndedEvent` triggers comprehensive cleanup

**Handler Responsibilities:**
- `onSpeedrunnerDied`: Win condition evaluation only
- `onDragonKilled`: Condition marking + win evaluation
- `onWinConditionMet`: Presentation layer (broadcast)
- `onGameEnded`: Cleanup orchestration

**Separation of Concerns:**
- Event publishing: `GameSession` responsibility (Task 7)
- Event handling: `NewManhuntManager` responsibility (Task 8)
- Event bridge: Bukkit listeners (Tasks 9-10)

### Integration Points

**Dependencies Verified:**
- `session.getEventPublisher()` - Available from Task 6
- `session.notifyGameStarted()` - Available from Task 7
- `session.notifyGameEnded(outcome, reason)` - Available from Task 7
- `session.end()` - Existing method
- `winConditionEvaluator` - Existing field
- `dragonKilledCondition` - Existing field
- `ChatHelper.importantMessage()` - Existing utility

**Next Tasks:**
- Task 9: Bukkit event bridge for speedrunner death
- Task 10: Bukkit event bridge for dragon killed
- These will publish domain events that trigger these handlers

### Testing Considerations

**Manual Test Scenarios:**
1. Start game → verify `GameStartedEvent` published
2. Speedrunner dies → verify handler evaluates win conditions
3. Dragon killed → verify condition marked and win evaluated
4. Win condition met → verify broadcast message
5. Game ended → verify roles stopped, listeners unregistered, cleanup completed

**Edge Cases Handled:**
- Dragon killed without killer (event.hasKiller() check)
- Multiple speedrunners dying (each death re-evaluates)
- Event handler cleanup via `session.end()`

## Concerns

None. Implementation follows the plan precisely and integrates cleanly with existing code.

## Verification

- Code follows plan steps 1-7 exactly
- All required methods implemented with correct signatures
- Handler registration occurs at proper lifecycle point
- Event notification occurs at game start
- JavaDoc comments included for all new methods
- Existing dependencies verified present in codebase
