# Manhunt Plugin - DDD Refactoring Progress Tracker

**Project:** Manhunt Plugin - Domain-Driven Design Refactoring  
**Started:** 2026-06-18  
**Current Phase:** Pre-Implementation  
**Target Completion:** TBD

---

## Overview

This document tracks progress on the DDD refactoring of the Manhunt plugin. Each priority from `PROBLEMS_PRIORITY_SUMMARY.md` is tracked with detailed sub-tasks, completion status, and notes.

**Status Legend:**
- ⬜ Not Started
- 🟦 In Progress
- ✅ Completed
- ⏸️ Blocked
- ❌ Cancelled

---

## Phase 1: Core Domain (Foundation)

### Priority 1: Create GameSession Aggregate Root ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 6-8 hours  
**Actual Effort:** ~8 hours

#### Tasks
- [x] 1.1 Design GameSession class structure
  - Define fields (player-role map, portal locations, speedrunner count)
  - Define methods (encapsulated access, no public fields)
  - Determine identity (session ID generation)
  
- [x] 1.2 Create GameSession class
  - ✅ Created GameSession.java with full encapsulation
  - ✅ Private fields with controlled access methods
  - ✅ Portal location tracking by environment
  - ✅ Speedrunner counter management

- [x] 1.3 Create GameSessionManager (singleton/service)
  - ✅ Created GameSessionManager.java
  - ✅ Singleton pattern implementation
  - ✅ Session lifecycle management
  - ✅ Player lookup across sessions

- [x] 1.4 Add adapter methods to GameData (bridge pattern)
  - ✅ Bridge methods delegating to GameSession
  - ✅ Fallback to old fields when no session
  - ✅ All fields marked @Deprecated
  - ✅ Documentation of migration path

- [x] 1.5 Update NewManhuntManager to use GameSession
  - ✅ Accept GameSession parameter in startGame()
  - ✅ Replaced GameData.playerClassList with session.getAllRoles()
  - ✅ Replaced GameData.remainingSpeedrunner with session methods
  - ✅ Added null check guards

- [x] 1.6 Update NewManhuntLauncher to create GameSession
  - ✅ Create session via GameSessionManager
  - ✅ Populate with player-role assignments
  - ✅ Pass session to manager
  - ✅ Cleanup on stop()

- [x] 1.7 Update role implementations
  - ✅ SpeedrunnerRole uses session from GameSessionManager
  - ✅ HunterRole uses session from GameSessionManager
  - ✅ All special role variants updated
  - ✅ Portal tracking via session methods

- [x] 1.8 Write tests
  - ✅ GameSessionIdTest (7 tests)
  - ✅ GameSessionTest (24 tests)
  - ✅ GameSessionManagerTest (18 tests)
  - ✅ GameDataTest (13 tests - adapter)
  - ✅ GameSessionIntegrationTest (7 tests)
  - ✅ Total: 69 tests

- [x] 1.9 Remove GameData adapter layer
  - ✅ Deleted GameData.java
  - ✅ Deleted GameDataTest.java
  - ✅ All references migrated to GameSession
  - ✅ Direct session usage throughout codebase

- [x] 1.10 Update documentation
  - ✅ Created docs/architecture/gamesession-aggregate.md
  - ✅ Documented architecture and design decisions
  - ✅ Updated REFACTORING_PROGRESS.md
  - ✅ Implementation plan maintained

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Used GameSessionId as value object for session identity
  - Singleton pattern for GameSessionManager (acceptable for game context)
  - Portal locations keyed by World.Environment enum (cleaner than separate maps)
  - Session retrieved via getActiveSessionForPlayer() in roles (cleaner than passing through constructors)
- **Questions:** None
- **Commits:**
  - 6782545: Initial foundation (Tasks 1-3)
  - 30d731d: GameData adapter layer (Task 4)
  - bf86817: NewManhuntManager migration (Task 5)
  - fec352c: NewManhuntLauncher migration (Task 6)
  - 91c5380: Role implementations migration (Task 7)
  - 1cbe908: Adapter removal (Task 8-9)

#### Success Criteria
- ✅ GameSession encapsulates all game state - ACHIEVED
- ✅ No direct access to GameData - ACHIEVED (GameData deleted)
- ✅ Can create multiple GameSession instances - ACHIEVED (tested)
- ✅ All tests pass - ACHIEVED (69 tests)
- ✅ No static mutable state remains - ACHIEVED (all state in sessions)

---

### Priority 2: Enrich Domain Model (Rich Entities) ✅
**Status:** Completed  
**Assigned To:** Claude (TDD)  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 8-10 hours  
**Actual Effort:** ~4 hours (TDD approach)

#### Tasks
- [x] 2.1 Identify anemic entities
  - ✅ Identified roles acting as data containers
  - ✅ Found business logic scattered in managers and event handlers

- [x] 2.2 Design rich Role behavior
  - ✅ Defined handleDamage() for death handling
  - ✅ Defined calculateCompassTarget() for compass logic
  - ✅ Behavior encapsulated in role methods

- [x] 2.3 Extract business logic into Role entities
  - ✅ Moved death handling from NewManhuntManager to SpeedrunnerRole
  - ✅ Moved compass logic from event handlers into AManhuntRole
  - ✅ Both SpeedrunnerRole and HunterRole use shared calculateCompassTarget()

- [x] 2.4 Create Value Objects
  - ✅ CompassTarget (location, dimension, cross-dimension handling)
  - ✅ DamageOutcome (died, remaining health)
  - ⚠️ RoleAbility - deferred (not critical for current refactoring)

- [x] 2.5 Implement Tell, Don't Ask pattern
  - ✅ Replaced `if (role.getRoleType() == SPEEDRUNNER)` with `role.handleDamage()`
  - ✅ Roles make decisions instead of external code asking their state
  - ✅ Manager tells role to handle damage, doesn't implement logic itself

- [x] 2.6 Add domain invariants
  - ✅ DamageOutcome.of() enforces immutability
  - ✅ CompassTarget validates non-null location
  - ✅ Factory methods ensure valid construction

- [x] 2.7 Write unit tests for rich entities
  - ✅ SpeedrunnerRoleTest with 5 tests (death and compass scenarios)
  - ✅ Tests written FIRST (TDD Red-Green-Refactor)
  - ✅ Mock Bukkit dependencies
  - ✅ Test death outcomes for various damage scenarios
  - ✅ Test compass target calculation for same/cross dimension

- [x] 2.8 Refactor managers to use rich entities
  - ✅ NewManhuntManager uses role.handleDamage()
  - ✅ Managers orchestrate, entities decide
  - ✅ Business logic removed from managers

#### Notes
- **Blockers:** None (Priority 1 completed)
- **Decisions Made:**
  - Used TDD discipline throughout (Red-Green-Refactor)
  - Extracted calculateCompassTarget to AManhuntRole (shared by both role types)
  - Value objects are immutable with factory methods
  - instanceof check acceptable for role-specific behavior in manager
- **Questions:** None
- **Commits:** Pending commit with all changes

#### Success Criteria
- ✅ Roles contain domain behavior, not just data - ACHIEVED
- ✅ Business logic lives in domain entities - ACHIEVED
- ✅ Managers only orchestrate, don't decide - ACHIEVED
- ✅ Value objects replace primitives where appropriate - ACHIEVED
- ✅ Unit tests cover domain logic - ACHIEVED

---

### Priority 3: Define Bounded Contexts ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 4-6 hours  
**Actual Effort:** ~3 hours

#### Tasks
- [x] 3.1 Identify subdomains
  - Core domain: Game Management, Role Definition, Role Behavior, Win Condition
  - Supporting: Player Tracking
  - Generic: Infrastructure (UI, Bukkit integration)

- [x] 3.2 Define bounded contexts
  - **Game Management Context:** Session lifecycle, configuration
  - **Role Definition Context:** Role types, identifiers, factory
  - **Role Behavior Context:** Role implementations, abilities
  - **Player Tracking Context:** Portal locations, player positions
  - **Win Condition Context:** Victory evaluation, outcomes
  - **Infrastructure Context:** Bukkit integration, UI, events

- [x] 3.3 Create context map
  - Documented relationships between contexts
  - Defined Customer/Supplier, Anti-Corruption Layer patterns
  - Created visual context diagram

- [x] 3.4 Establish ubiquitous language per context
  - Game Management: Session, Configuration, Launch
  - Role Definition: RoleType, RoleIdentifier, Factory
  - Role Behavior: Role, Ability, DamageOutcome, CompassTarget
  - Player Tracking: PortalLocation, Dimension, CrossDimension
  - Win Condition: WinCondition, Evaluator, Outcome, Victory
  - Infrastructure: EventBridge, UIView, CommandHandler

- [x] 3.5 Restructure packages by context
  ```
  domain/
    game/           # Game Management Context
    role/
      definition/   # Role Definition Context
      behavior/     # Role Behavior Context
    tracking/       # Player Tracking Context
    wincondition/   # Win Condition Context
  infrastructure/
    ui/             # Infrastructure Context
  ```

- [x] 3.6 Define context interfaces
  - Game Management -> Role Definition: ManhuntRoleFactory
  - Game Management -> Player Tracking: PortalTracker delegation
  - Game Management -> Win Condition: WinConditionEvaluator
  - Infrastructure -> Domain: Anti-Corruption Layer

- [x] 3.7 Document context boundaries
  - Created bounded-contexts-map.md
  - Documented integration points
  - Created context diagram

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Split Role Context into Definition (stable) vs Behavior (volatile)
  - Extracted Win Condition as separate context (strategic importance)
  - Expanded from 4 planned contexts to 6 for better separation
  - Used direct method calls between contexts (simpler than events for now)
  - Extracted PortalTracker from GameSession via delegation
- **Questions:** None
- **Commits:**
  - Tasks 1-7: Package reorganization commits
  - b604894: PortalTracker creation with tests
  - 1d1d81b: Portal extraction from GameSession
  - c9d5981: WinCondition interface and WinOutcome
  - 915f8b5: AllSpeedrunnersDeadCondition
  - 120f695: DragonKilledCondition
  - bb398bc: WinConditionEvaluator
  - 6142afe: Evaluator integration into managers

#### Success Criteria
- All 6 contexts identified and documented - ACHIEVED
- Packages organized by context - ACHIEVED
- Ubiquitous language defined per context - ACHIEVED
- Context map created - ACHIEVED
- Integration points defined - ACHIEVED

---

### Priority 4: Fix Primitive Obsession (String Types) ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 2-3 hours  
**Actual Effort:** ~2.5 hours

#### Tasks
- [x] 4.1 Add getRoleType() to ManhuntRoleIdentifier enum
  - ✅ Added constructor with ManhuntRoleType parameter
  - ✅ Added private final roleType field
  - ✅ Added public getRoleType() method
  - ✅ All 22 role identifiers have explicit type associations

- [x] 4.2 Add static grouping methods
  - ✅ Created RoleTypeRegistry utility class
  - ✅ Implemented getRolesOfType() for efficient lookups
  - ✅ Precomputed EnumMap for O(1) performance

- [x] 4.3 Find all string-based type checks
  - ✅ Found 4 instances in GameRolesManagement
  - ✅ Found 1 instance in NewManhuntLauncher
  - ✅ SWORD item-type checks correctly identified as non-role-type

- [x] 4.4 Replace with type-safe calls
  - ✅ Replaced roleId.toString().contains() with roleId.getRoleType() == ManhuntRoleType.X
  - ✅ All call sites migrated in Tasks 3-6

- [x] 4.5 Update GameRolesManagement
  - ✅ countRolesByType() uses RoleTypeRegistry
  - ✅ filterRolesByType() uses RoleTypeRegistry
  - ✅ countHunterRoles() uses RoleTypeRegistry

- [x] 4.6 Write tests
  - ✅ ManhuntRoleIdentifierTest (7 tests)
  - ✅ RoleTypeRegistryTest (10 tests)
  - ✅ Total: 17 new unit tests

- [x] 4.7 Remove comment about "bad design"
  - ✅ Removed offensive comment from ManhuntRoleIdentifier.java

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Created separate RoleTypeRegistry class instead of static methods on enum (better separation of concerns)
  - Used EnumMap for O(1) lookups instead of streaming every time
  - Kept SWORD item-type checks unchanged (not role-type detection)
  - Followed TDD approach with comprehensive test coverage
- **Questions:** None
- **Commits:**
  - Tasks 1-2: Enum enhancement and registry creation with tests
  - Tasks 3-6: Call site migrations (counting, filtering, launcher)
  - Task 7: Comment removal and documentation updates

#### Success Criteria
- ✅ No string-based role type detection remains - VERIFIED via grep
- ✅ All role identifiers have explicit type association - ACHIEVED (22 roles)
- ✅ Compile-time type safety enforced - ACHIEVED
- ✅ IDE refactoring works correctly - ACHIEVED
- ✅ Tests validate type mappings - ACHIEVED (17 tests)

---

## Phase 2: Domain Events & Services

### Priority 5: Implement Domain Events ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 6-8 hours  
**Actual Effort:** ~7 hours

#### Tasks
- [x] 5.1 Design domain event base class
- [x] 5.2 Identify domain events
- [x] 5.3 Create event classes
- [x] 5.4 Create event publisher
- [x] 5.5 Implement event handlers
- [x] 5.6 Refactor managers to publish events
- [x] 5.7 Create event bridge (infrastructure)
- [x] 5.8 Write tests

#### Notes
- **Blockers:** None (Priority 1 completed)
- **Decisions Made:**
  - Chose lightweight in-memory event bus (Approach 1 from spec)
  - Synchronous event delivery for simplicity
  - Session-scoped handlers prevent memory leaks
  - Hybrid Bukkit strategy: domain events for business logic, keep Bukkit events for UI
  - Manual handler registration in NewManhuntManager.startGame()
  - Exception handling: log and continue to next handler
- **Questions:** None
- **Commits:**
  - cf7ac18: Task 1: Core infrastructure base classes
  - 57e1e73: Task 2: InMemoryEventPublisher implementation
  - a190172: Task 3: Game lifecycle events
  - fc004de: Task 4: Role and player events
  - 707de59: Task 5: Win condition events
  - 62622d0: Task 6: GameSession event publisher integration
  - e86f44a: Task 7: Event publishing methods
  - 5e7dcb7: Task 8: Handler registration in NewManhuntManager
  - f053853: Task 9: Speedrunner death Bukkit bridge
  - a9828fd: Task 10: Dragon death Bukkit bridge
  - af45b61: Task 11: Test utilities and integration tests
  - (Task 12: Documentation update)

#### Success Criteria
- ✅ All significant domain changes publish events - ACHIEVED (7 event types)
- ✅ Domain events decoupled from infrastructure events - ACHIEVED (bridge pattern)
- ✅ Event handlers contain domain logic - ACHIEVED (win condition checking in handlers)
- ✅ Domain can be tested without Bukkit - ACHIEVED (MockEventPublisher, 20+ tests)
- ✅ Event flow documented - ACHIEVED (design spec + code comments)

---

### Priority 6: Create Application Services Layer ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-22  
**Completed:** 2026-06-23  
**Estimated Effort:** 5-7 hours  
**Actual Effort:** ~6 hours

#### Tasks
- [x] 6.1 Identify use cases
  - ✅ Start Game (via StartGameSaga)
  - ✅ End Game (via EndGameSaga)
  - ✅ Assign Roles (via RoleAssignmentService)
  - ✅ Distribute Roles (via RoleDistributionService)
  - ✅ Game Lifecycle (via GameLifecycleService)
  - ✅ Event Handler Registration (via EventHandlerRegistrationService)

- [x] 6.2 Create application service interfaces
  - ✅ Services use command pattern with validation
  - ✅ Sagas orchestrate complex workflows
  - ✅ Services are thin orchestration layers

- [x] 6.3 Implement application services
  - ✅ GameLifecycleService (session lifecycle management)
  - ✅ RoleDistributionService (role assignment logic)
  - ✅ RoleAssignmentService (role creation and registration)
  - ✅ EventHandlerRegistrationService (event handler setup)
  - ✅ StartGameSaga (orchestrates game start workflow)
  - ✅ EndGameSaga (orchestrates game end workflow)

- [x] 6.4 Define command objects (DTOs)
  - ✅ StartGameCommand (players, config)
  - ✅ EndGameCommand (sessionId, winOutcome)
  - ✅ DistributeRolesCommand (sessionId, players, config)
  - ✅ AssignRolesCommand (sessionId, playerRoles)
  - ✅ RegisterEventHandlersCommand (sessionId)
  - ✅ All commands with validation and factory methods

- [x] 6.5 Refactor managers to use services
  - ✅ NewManhuntLauncher uses StartGameSaga
  - ✅ NewManhuntManager uses EndGameSaga
  - ✅ Both use constructor injection

- [x] 6.6 Add transaction boundaries
  - ✅ Sagas define workflow transaction boundaries
  - ✅ Each service operation is atomic
  - ✅ Event publishing ensures consistency

- [x] 6.7 Remove singleton managers
  - ✅ Replaced with Guice dependency injection
  - ✅ Services are singletons managed by DI container
  - ✅ Constructor injection throughout

- [x] 6.8 Write tests
  - ✅ GameLifecycleServiceTest (14 tests)
  - ✅ RoleDistributionServiceTest (9 tests)
  - ✅ RoleAssignmentServiceTest (8 tests)
  - ✅ EventHandlerRegistrationServiceTest (5 tests)
  - ✅ StartGameSagaTest (8 tests)
  - ✅ EndGameSagaTest (6 tests)
  - ✅ Total: 50 tests

#### Notes
- **Blockers:** None (Dependencies Priority 1, 2, 5 complete)
- **Decisions Made:**
  - Used Google Guice 5.1.0 for dependency injection (Java 8 compatible)
  - Implemented Saga pattern for complex workflows (StartGame, EndGame)
  - Command pattern with validation for all service operations
  - Value objects (SessionId, WinOutcome) for type safety
  - Custom exceptions (SessionNotFoundException, InvalidCommandException)
  - RoleDistributionService contains complex algorithm as-is (refactored in Priority 10)
  - Event-driven architecture with domain events published by services
  - Comprehensive test coverage with mocking via Mockito
- **Questions:** None
- **Commits:**
  - 39b9981: Task 1: Add Google Guice dependency
  - 6e6becd: Task 2: Add application layer domain events
  - be24463: Task 3: Add exception classes and value objects
  - 399a1fa: Task 4: Add command objects with validation
  - 7282c40: Task 5: Add GameLifecycleService
  - e2ac538: Task 6: Add RoleDistributionService
  - 016d499: Task 7: Add RoleAssignmentService and EventHandlerRegistrationService
  - c2ec9b3: Task 8: Add StartGameSaga
  - 09375dc: Task 9: Add EndGameSaga
  - a1a3f1d: Task 10: Add Guice DI module
  - 2ae1ce6: Task 11: Integrate Guice in Main.java
  - 78dde36: Task 12: Migrate NewManhuntLauncher
  - c32da38: Task 13: Migrate NewManhuntManager

#### Success Criteria
- ✅ Use cases clearly defined - ACHIEVED (6 use cases identified and implemented)
- ✅ Application services orchestrate domain - ACHIEVED (thin orchestration, no business logic)
- ✅ Commands/queries separated - ACHIEVED (command objects with validation)
- ✅ No business logic in services (only orchestration) - ACHIEVED (logic in domain/services)
- ✅ Services testable in isolation - ACHIEVED (50 tests with comprehensive mocking)

---

### Priority 8: Add Explicit State Machine ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-24  
**Completed:** 2026-06-24  
**Estimated Effort:** 4-5 hours  
**Actual Effort:** ~4 hours

#### Tasks
- [x] 8.1 Define game phases enum
- [x] 8.2 Add phase to GameSession
- [x] 8.3 Define valid transitions
- [x] 8.4 Implement transition validation
- [x] 8.5 Add phase change listeners (via event handlers)
- [x] 8.6 Implement phase-specific behavior
- [x] 8.7 Replace implicit state with explicit phase checks
- [x] 8.8 Write tests

#### Notes
- **Blockers:** ~~Depends on Priority 1 (GameSession)~~ - RESOLVED
- **Decisions Made:** 
  - Two states only (PREPARATION, ACTIVE)
  - No ENDED state (session destroyed on game end)
  - Event-driven phase transitions
  - Static map for transition validation
- **Questions:** -
- **Commits:** 
  - feat(domain): add GamePhase enum for state machine (Priority 8)
  - feat(domain): add currentPhase field to GameSession
  - feat(domain): add transition validation to GameSession
  - feat(domain): add PhaseChangedEvent
  - feat(domain): implement transitionTo() with validation and event publishing
  - feat(domain): add phase change handler to NewManhuntManager
  - refactor(domain): use phase transitions in startGame()

#### Success Criteria
- ✅ All game phases explicit
- ✅ Transition validation prevents invalid states
- ✅ Phase-specific behavior clear
- ✅ Easy to add new phases
- ✅ State machine documented

---

## Phase 3: Tactical Patterns

### Priority 7: Refactor Factory (Open/Closed) ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-23  
**Completed:** 2026-06-23  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** ~3 hours

#### Tasks
- [x] 7.1 Create RoleFactory interface
  ```java
  @FunctionalInterface
  public interface RoleFactory {
      AManhuntRole create(Player owner);
  }
  ```

- [x] 7.2 Create RoleRegistry
  ```java
  public class RoleRegistry {
      private Map<ManhuntRoleIdentifier, RoleFactory> factories;
      public void register(ManhuntRoleIdentifier id, RoleFactory factory);
      public AManhuntRole createRole(ManhuntRoleIdentifier id, Player owner);
  }
  ```

- [x] 7.3 Register all roles in ManhuntModule
  ```java
  RoleRegistry registry = RoleRegistry.getInstance();
  registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
  registry.register(HUNTER_SIMPLE, HunterRole::new);
  // ... etc
  ```

- [x] 7.4 Replace ManhuntRoleFactory with RoleRegistry
  - Update all calls to use registry
  - Keep old factory as deprecated adapter temporarily

- [x] 7.5 (Optional) Add annotation-based registration - SKIPPED (manual registration chosen)
  ```java
  @RegisterRole(SPEEDRUNNER_CHECKPOINT)
  public class CheckpointSpeedrunnerRole extends SpeedrunnerRole { }
  ```

- [x] 7.6 Remove old ManhuntRoleFactory
  - Delete switch statement
  - Remove deprecated adapter

- [x] 7.7 Write tests
  - Test registration
  - Test role creation
  - Test unknown role handling

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Manual registration in ManhuntModule chosen over annotation-based (simpler, no reflection)
  - RoleRegistry uses HashMap for O(1) lookup performance
  - Parameter order: createRole(identifier, owner) differs from old factory for consistency
  - Functional interface enables method references (SpeedrunnerRole::new)
- **Questions:** None
- **Commits:**
  - Task 1: RoleFactory interface creation
  - Task 2: RoleRegistry with comprehensive tests
  - Task 3: ManhuntModule provider method
  - Task 4: RoleAssignmentService migration
  - Task 5: ManhuntRoleFactory deletion
  - Task 6: Documentation update

#### Success Criteria
- ✅ No switch statement in factory - ACHIEVED (replaced with Map lookup)
- ✅ Can register roles dynamically - ACHIEVED (public register() method)
- ✅ Adding role doesn't require modifying factory - ACHIEVED (one line in provider)
- ✅ Registry is extensible - ACHIEVED (public API allows plugin registration)
- ✅ Tests cover registration and creation - ACHIEVED (8 unit tests)

---

### Priority 10: Extract Role Distribution Strategy ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 4-5 hours  
**Actual Effort:** -

#### Tasks
- [ ] 10.1 Create strategy interface
  ```java
  public interface RoleDistributionStrategy {
      Map<Player, ManhuntRoleIdentifier> distribute(
          List<Player> players,
          RoleDistributionConfig config
      );
  }
  ```

- [ ] 10.2 Create configuration value object
  ```java
  public class RoleDistributionConfig {
      private final int speedrunnerCount;
      private final int allyCount;
      private final boolean specialRolesOnly;
      private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;
  }
  ```

- [ ] 10.3 Extract current logic to RandomRoleDistribution
  - Copy GameRolesManagement.distributeRoles() logic
  - Refactor to use config object
  - Make testable

- [ ] 10.4 Simplify distribution algorithm
  - Extract helper methods
  - Clarify probabilities
  - Document business rules

- [ ] 10.5 (Optional) Create alternative strategies
  - BalancedRoleDistribution (ensures team balance)
  - CustomRoleDistribution (admin-defined rules)

- [ ] 10.6 Update RoleDistributionService
  - Accept strategy as parameter
  - Default to RandomRoleDistribution

- [ ] 10.7 Write tests
  - Unit test RandomRoleDistribution
  - Test various configurations
  - Test edge cases (not enough players, etc.)

#### Notes
- **Blockers:** Depends on Priority 4 (type-safe roles) - COMPLETE ✅
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -
- **Related Work:** 
  - RoleDistributionService created in Priority 6 at `src/main/java/me/flamboyant/manhunt/application/services/RoleDistributionService.java`
  - Current implementation contains complex distribution algorithm copied from GameRolesManagement.distributeRoles()
  - Algorithm is intentionally left as-is in Priority 6 and will be refactored into Strategy pattern in Priority 10
  - See RoleDistributionService.java line 21-24 for inline documentation about this refactoring path

#### Success Criteria
- ✅ Distribution logic extracted and testable
- ✅ Strategy pattern enables swapping algorithms
- ✅ Configuration explicit
- ✅ Business rules documented
- ✅ Complex logic simplified

---

### Priority 11: Model Win Conditions ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** -

#### Tasks
- [ ] 11.1 Create WinCondition interface
  ```java
  public interface WinCondition {
      boolean isMet(GameSession session);
      Set<ManhuntRoleType> getWinners();
      String getDescription();
  }
  ```

- [ ] 11.2 Implement concrete win conditions
  - AllSpeedrunnersDeadCondition
  - DragonKilledCondition
  - CompositeWinCondition (AND/OR logic)

- [ ] 11.3 Create WinConditionRegistry
  ```java
  public class WinConditionRegistry {
      public void register(WinCondition condition);
      public Optional<WinConditionResult> checkWinConditions(GameSession session);
  }
  ```

- [ ] 11.4 Extract win condition checks from managers
  - Remove hardcoded win checks
  - Use registry.checkWinConditions()

- [ ] 11.5 Implement IHunterWinConditionModifier pattern
  - Convert to composable win conditions
  - Remove static list

- [ ] 11.6 Add win condition to game configuration
  - Allow custom win conditions per game
  - Default to standard conditions

- [ ] 11.7 Write tests
  - Test each win condition in isolation
  - Test composite conditions
  - Test win condition evaluation

#### Notes
- **Blockers:** Depends on Priority 1 (GameSession)
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -

#### Success Criteria
- ✅ Win conditions explicit and testable
- ✅ Win logic extracted from managers
- ✅ Composable win conditions
- ✅ Easy to add custom win conditions
- ✅ Win conditions documented

---

## Phase 4: Infrastructure & Polish

### Priority 9: Fix Event Listener Lifecycle ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** -

#### Tasks
- [ ] 9.1 Create RoleEventManager
  ```java
  public class RoleEventManager {
      private Map<AManhuntRole, List<Listener>> roleListeners;
      public void registerRoleListeners(AManhuntRole role, Listener... listeners);
      public void unregisterRoleListeners(AManhuntRole role);
      public void unregisterAll();
  }
  ```

- [ ] 9.2 Integrate with GameSession lifecycle
  ```java
  class GameSession {
      private RoleEventManager eventManager;
      
      public void end() {
          eventManager.unregisterAll();
      }
  }
  ```

- [ ] 9.3 Update AManhuntRole
  - Add abstract method: `protected abstract List<Listener> createListeners();`
  - Remove manual registration from roles

- [ ] 9.4 Update role implementations
  - Return listeners from createListeners()
  - Remove manual register/unregister calls

- [ ] 9.5 Audit all event registrations
  - Find all `registerEvents()` calls
  - Find all `unregister()` calls
  - Ensure all go through RoleEventManager

- [ ] 9.6 Write tests
  - Test listener registration
  - Test listener cleanup
  - Test no leaks after multiple games

#### Notes
- **Blockers:** Depends on Priority 1 (GameSession)
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -

#### Success Criteria
- ✅ Centralized listener management
- ✅ Automatic cleanup on game end
- ✅ No manual registration in roles
- ✅ No listener leaks
- ✅ Memory profiling confirms cleanup

---

### Priority 12: Add Anti-Corruption Layer (Framework) ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 6-8 hours  
**Actual Effort:** -

#### Tasks
- [ ] 12.1 Identify framework touchpoints
  - UI/configuration system
  - Parameter system
  - Plugin lifecycle hooks

- [ ] 12.2 Create domain interfaces
  ```java
  public interface GameConfigurationProvider {
      GameConfiguration getConfiguration(Player initiator);
  }
  ```

- [ ] 12.3 Create framework adapters
  ```java
  public class FlamboyantConfigAdapter implements GameConfigurationProvider {
      // Translates FlamboyantPluginTools to domain concepts
  }
  ```

- [ ] 12.4 Create test implementations
  ```java
  public class MockConfigProvider implements GameConfigurationProvider {
      // In-memory for testing
  }
  ```

- [ ] 12.5 Update Main to use adapters
  - Inject adapter based on framework availability
  - Default to mock for tests

- [ ] 12.6 Remove direct framework imports from domain
  - Domain should not import me.flamboyant.utils
  - Only infrastructure layer imports framework

- [ ] 12.7 Create fallback implementations
  - Chat-based config if framework unavailable
  - Simple UI alternatives

- [ ] 12.8 Write tests
  - Test domain with mock adapter
  - Test framework adapter integration
  - Test without framework dependency

#### Notes
- **Blockers:** Depends on Priority 3 (bounded contexts)
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -

#### Success Criteria
- ✅ Domain code doesn't import framework
- ✅ Framework dependencies isolated to infrastructure
- ✅ Can test domain without framework
- ✅ Can swap framework implementations
- ✅ Anti-corruption layer documented

---

### Priority 13: Improve Role Abstraction ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 2-3 hours  
**Actual Effort:** -

#### Tasks
- [ ] 13.1 Identify common patterns in roles
  - Cooldown management
  - Item handling
  - Event filtering
  - Ability activation

- [ ] 13.2 Create helper classes
  - CooldownManager
  - AbilityManager
  - ItemHelper (already exists?)

- [ ] 13.3 Refactor AManhuntRole base class
  ```java
  public abstract class AManhuntRole {
      protected final CooldownManager cooldowns;
      protected final AbilityManager abilities;
      
      protected void registerAbility(Ability ability);
  }
  ```

- [ ] 13.4 Create Ability abstraction
  ```java
  public interface Ability {
      boolean canActivate(Player player);
      void activate(Player player);
      int getCooldownSeconds();
  }
  ```

- [ ] 13.5 Refactor roles to use abstractions
  - Replace manual cooldown code
  - Replace manual event filtering
  - Use template methods

- [ ] 13.6 Reduce duplication
  - Extract common event handler patterns
  - Share cooldown logic
  - Share item handling

- [ ] 13.7 Write tests
  - Test cooldown management
  - Test ability system
  - Test role-specific logic

#### Notes
- **Blockers:** Should wait until after Priority 2 (rich entities)
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -

#### Success Criteria
- ✅ Less duplication across roles
- ✅ Common patterns abstracted
- ✅ Easier to implement new roles
- ✅ Template method pattern used appropriately
- ✅ Boilerplate minimized

---

## Overall Progress Summary

### Phase Completion
- [x] Phase 1: Core Domain (4/4 complete) - **Priorities 1, 2, 3, 4 DONE** ✅
- [x] Phase 2: Domain Events & Services (3/3 complete) - **Priorities 5, 6, 8 DONE** ✅
- [ ] Phase 3: Tactical Patterns (1/3 complete) - **Priority 7 DONE** ✅
- [ ] Phase 4: Infrastructure & Polish (0/3 complete)

### Total Progress: 8/13 priorities completed (62%)

### Time Tracking
- **Estimated Total:** 57-76 hours
- **Actual Total:** 0 hours
- **Remaining:** 57-76 hours

---

## Milestones

### Milestone 1: GameSession Foundation ✅
**Target Date:** 2026-06-19  
**Dependencies:** Priority 1  
**Deliverable:** GameSession aggregate root implemented, all state encapsulated  
**Status:** COMPLETED - All state now encapsulated in GameSession, adapter removed, 69 tests passing

### Milestone 2: Rich Domain Model ✅
**Target Date:** 2026-06-19  
**Dependencies:** Milestone 1, Priority 2  
**Deliverable:** Domain entities contain behavior, business logic in domain layer  
**Status:** COMPLETED - Rich entities with behavior, value objects, Tell-Don't-Ask pattern

### Milestone 3: Bounded Contexts Defined ✅
**Target Date:** 2026-06-19  
**Dependencies:** Milestone 2, Priority 3  
**Deliverable:** Clear context boundaries, packages organized, ubiquitous language documented  
**Status:** COMPLETED - 6 bounded contexts defined, packages organized, Win Condition context with evaluator

### Milestone 4: Event-Driven Domain ✅
**Target Date:** 2026-06-19  
**Dependencies:** Milestone 2, Priority 5  
**Deliverable:** Domain events implemented, domain decoupled from infrastructure  
**Status:** COMPLETED - 7 domain event types, event publisher, Bukkit bridge, 20+ tests

### Milestone 5: Application Services Layer ✅
**Target Date:** 2026-06-23  
**Dependencies:** Milestone 4, Priority 6  
**Deliverable:** Use cases orchestrated by application services, singletons removed  
**Status:** COMPLETED - 6 services, Saga pattern, Guice DI, command pattern, 50 tests

### Milestone 6: Tactical Patterns Complete ⬜
**Target Date:** TBD  
**Dependencies:** Milestone 5, Priorities 7-11  
**Deliverable:** Factory, state machine, distribution, win conditions refactored

### Milestone 7: Infrastructure Decoupled ⬜
**Target Date:** TBD  
**Dependencies:** Milestone 6, Priorities 9, 12, 13  
**Deliverable:** Clean infrastructure layer, no framework leaks, resource management solid

### Milestone 8: DDD Refactoring Complete ⬜
**Target Date:** TBD  
**Dependencies:** All milestones  
**Deliverable:** All priorities complete, tests passing, documentation updated

---

## Blockers & Issues

### Current Blockers
- None (pre-implementation)

### Resolved Blockers
- None yet

---

## Decisions Log

### Decision 1: Follow DDD Approach
**Date:** 2026-06-18  
**Decision:** Adopt Domain-Driven Design principles for refactoring  
**Rationale:** Current architecture lacks clear domain model and boundaries. DDD provides proven patterns for complex domain logic.  
**Alternatives Considered:** MVC, Service-Oriented, Transaction Script  
**Impact:** Requires learning DDD concepts, more upfront design, but better long-term maintainability

---

## Notes & Learnings

### 2026-06-18: Initial Analysis
- Identified 13 priorities ranging from CRITICAL to LOW severity
- Estimated 57-76 hours total effort
- Key insight: GameSession aggregate root is foundational - blocks most other patterns
- String-based role detection is quick win (2-3 hours) but lower priority
- Framework coupling is lower priority - functionality over independence initially

---

## References

- **PROBLEMS_PRIORITY_SUMMARY.md** - Detailed problem descriptions and DDD mappings
- **ARCHITECTURE_ANALYSIS.md** - Original comprehensive analysis with code examples
- **DDD Reference:** Eric Evans "Domain-Driven Design" (Blue Book)
- **DDD Reference:** Vaughn Vernon "Implementing Domain-Driven Design" (Red Book)

---

## How to Use This Document

1. **Before starting work:** Review the priority you're implementing
2. **While working:** Check off sub-tasks as you complete them
3. **Track time:** Update "Actual Effort" field
4. **Note decisions:** Document important decisions in "Notes" section
5. **Update blockers:** Mark when blocked or unblocked
6. **Commit references:** Add commit SHAs to track changes
7. **After completion:** Update status to ✅, mark completion date, update phase progress

---

**Last Updated By:** Initial creation  
**Next Review Date:** TBD
