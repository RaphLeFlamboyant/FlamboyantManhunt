# Event Listener Lifecycle Management

**Status:** Implemented (Priority 9)  
**Date:** 2026-06-25

---

## Overview

Centralized management of Bukkit event handler lifecycles for game sessions. Prevents memory leaks by ensuring all role event handlers are automatically unregistered when a game session ends.

---

## Architecture

### Components

**EventHandlerRegistrationService (Application Layer)**
- Registers Bukkit event handlers with Bukkit's PluginManager
- Returns `HandlerRegistration` tracking object
- Unregisters handlers when given a `HandlerRegistration`
- Stateless, idempotent, injectable service

**GameSession (Domain Layer)**
- Stores its `HandlerRegistration` reference
- Provides getter/setter for handler registration
- Session lifecycle tied to handler lifecycle

**StartGameSaga (Application Layer)**
- Collects all role listeners after role assignment
- Registers handlers via `EventHandlerRegistrationService`
- Stores registration in GameSession

**GameLifecycleService (Application Layer)**
- Orchestrates session end flow
- Calls unregisterHandlers before session cleanup
- Ensures "end before remove" invariant
- Exception handling for unregistration failures

**Roles (Domain Layer)**
- Implement `Listener` interface
- Define `@EventHandler` methods for game events
- No longer manually register or unregister
- Simplified `doStart()` and `doStop()` methods

---

## Lifecycle Flow

### Game Start
1. User starts game via launcher
2. StartGameSaga.startGame(command)
3. GameLifecycleService creates GameSession
4. RoleDistributionService distributes roles
5. RoleAssignmentService creates role instances
6. **StartGameSaga collects all role listeners**
7. **EventHandlerRegistrationService.registerHandlers(sessionId, listeners[])**
8. **HandlerRegistration stored in GameSession**
9. Roles start (doStart() - no manual registration)
10. Game active - handlers respond to Bukkit events

### Game End
1. Win condition met OR manual stop
2. EndGameSaga.endGame(command)
3. WinConditionEvaluator determines outcome
4. GameLifecycleService.endSession(sessionId)
5. Stop all roles (doStop() - no manual unregistration)
6. **EventHandlerRegistrationService.unregisterHandlers(registration)**
7. **GameSession.end() cleans domain event handlers**
8. Session removed from GameSessionManager

---

## Key Design Decisions

### Why Application Service Manages Lifecycle
- Infrastructure concern (Bukkit API) should not be in domain
- Application services orchestrate cross-cutting concerns
- Centralized tracking enables auditing and debugging

### Why Store Registration in GameSession
- Ties handler lifecycle to session lifecycle
- Makes cleanup responsibility explicit
- Enables future enhancements (per-session handler auditing)

### Why Collect After Role Assignment
- Roles must exist before we can register their listeners
- Ensures all roles are ready to handle events when registered
- Simplifies error handling (compensation on failure)

### Exception Handling
- Unregistration failures logged but don't block cleanup
- Session always removed even if unregistration fails
- Prevents partial cleanup states

---

## Benefits

### Memory Leak Prevention
- Guaranteed cleanup when session ends
- No manual unregistration required
- Automatic cleanup even on error paths

### Code Simplification
- Removed 80-112 lines of boilerplate across 16 role classes
- Consistent pattern (no role-specific variations)
- Single source of truth for handler management

### Testability
- Application layer fully testable with mocks
- Integration tests verify end-to-end flow
- Roles testable without Bukkit event system

### Session Isolation
- Multiple concurrent games have independent handlers
- Ending one game doesn't affect others
- Clear ownership of handlers per session

---

## Testing

### Unit Tests
- `GameSessionTest` - handlerRegistration field
- `GameLifecycleServiceTest` - unregistration logic
- `StartGameSagaTest` - registration logic

### Integration Tests
- `RoleHandlerLifecycleIntegrationTest` - end-to-end flow
- Multiple session isolation tests
- Sequential game tests (no accumulation)

### Manual Testing
- Start/end game cycles (10+ iterations)
- Concurrent games
- Memory profiling (handler count tracking)

---

## Migration Notes

### Files Modified
- **Domain:** GameSession + 16 role classes
- **Application:** StartGameSaga, GameLifecycleService
- **Tests:** 4 test files modified, 1 new integration test

### Backward Compatibility
- No breaking changes to game functionality
- All existing tests pass
- Roles still implement Listener and use @EventHandler

---

## Future Enhancements

### Possible Improvements
- Debug command to list active handlers per session
- Handler registration audit log
- Dynamic role addition (add handler after game start)

### Out of Scope
- Configuration phase handler management (FlamboyantTools)
- Domain event handler management (InMemoryEventPublisher)
- Automatic listener discovery via reflection

---

## References

- **Spec:** `docs/superpowers/specs/2026-06-25-event-listener-lifecycle-design.md`
- **Plan:** `docs/superpowers/plans/2026-06-25-event-listener-lifecycle.md`
- **Progress:** `docs/REFACTORING_PROGRESS.md` - Priority 9
- **Priority Summary:** `docs/PROBLEMS_PRIORITY_SUMMARY.md` - Priority 9
