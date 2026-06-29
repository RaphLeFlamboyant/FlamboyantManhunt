# Application Services Layer - Design Specification

**Priority:** 6  
**Date:** 2026-06-22  
**Status:** Design Approved  
**Effort Estimate:** 5-7 hours  
**Dependencies:** Priorities 1-5 (GameSession, Rich Entities, Bounded Contexts, Type Safety, Domain Events)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Command Objects](#command-objects)
4. [Application Services](#application-services)
5. [Saga Coordinators](#saga-coordinators)
6. [Dependency Injection](#dependency-injection)
7. [Error Handling & Compensation](#error-handling--compensation)
8. [Testing Strategy](#testing-strategy)
9. [Migration Path](#migration-path)
10. [Success Criteria](#success-criteria)

---

## Overview

### Goal

Create an application services layer that orchestrates domain use cases, eliminating singleton managers and establishing proper dependency injection using Google Guice. Services are fine-grained, single-responsibility components that coordinate domain operations through commands and saga pattern orchestration.

### Problem Statement

Currently, the codebase has three singleton managers (`NewManhuntManager`, `NewManhuntLauncher`, `GameRolesManagement`) that contain scattered use case logic. There is no clear orchestration layer, no transaction boundaries, and no explicit compensation on failures. Business logic is fragmented across multiple classes with tight coupling and hidden dependencies.

### Solution Approach

**Event-Driven Saga Architecture** with:
- **Fine-grained services** - single responsibility per service
- **Rich command objects** - validation at the boundary
- **Saga coordinators** - explicit multi-step workflow orchestration
- **Google Guice DI** - eliminates singletons, enables testability
- **Fail-fast compensation** - explicit rollback on errors

### Key Benefits

- ✅ Clear use case boundaries and orchestration
- ✅ Testable services with dependency injection
- ✅ Explicit compensation logic for consistency
- ✅ Leverages existing domain events infrastructure
- ✅ Single responsibility per service
- ✅ No singleton managers

---

## Architecture

### Architectural Layers

```
┌─────────────────────────────────────────────┐
│   Infrastructure Layer                       │
│   (Main.java, NewManhuntLauncher UI,        │
│    Bukkit event listeners)                   │
└────────────────┬────────────────────────────┘
                 │ sends commands
                 ↓
┌─────────────────────────────────────────────┐
│   Application Layer (NEW - Priority 6)      │
│                                              │
│   Commands → Services → Sagas               │
│   - StartGameCommand                        │
│   - GameLifecycleService                    │
│   - StartGameSaga                           │
└────────────────┬────────────────────────────┘
                 │ uses & publishes
                 ↓
┌─────────────────────────────────────────────┐
│   Domain Layer (Priorities 1-5)             │
│   - GameSession (aggregate)                 │
│   - Roles (entities)                        │
│   - Domain Events                           │
│   - Win Conditions                          │
└─────────────────────────────────────────────┘
```

### Design Principles

1. **Infrastructure sends commands** to application services
2. **Services perform single operations** (create session, distribute roles, etc.)
3. **Sagas coordinate multi-step workflows** by listening to domain events
4. **All domain events flow through DomainEventPublisher** from Priority 5
5. **Services are stateless** - all state in domain or commands
6. **Services guarantee atomicity** - all-or-nothing with explicit compensation
7. **Google Guice manages lifecycle** - no singleton pattern

### Package Structure

```
me.flamboyant.manhunt.application/
  commands/
    StartGameCommand.java
    EndGameCommand.java
    DistributeRolesCommand.java
    AssignRolesCommand.java
  
  services/
    GameLifecycleService.java
    RoleDistributionService.java
    RoleAssignmentService.java
    EventHandlerRegistrationService.java
  
  sagas/
    StartGameSaga.java
    EndGameSaga.java
  
  injection/
    ManhuntModule.java          (Guice configuration)
  
  exceptions/
    GameStartException.java
    CompensationStatus.java
```

---

## Command Objects

Commands are immutable DTOs that encapsulate all parameters for a use case. They validate invariants in their constructors, ensuring invalid configurations never reach services.

### StartGameCommand

**Purpose:** Encapsulates all parameters needed to start a game.

**Fields:**
```java
public class StartGameCommand {
    private final List<Player> players;
    private final Map<Player, ManhuntRoleIdentifier> fixedRoleAssignments;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final boolean resetPlayerStuff;
    private final boolean surpriseSpeedrunner;
    private final int minutesBeforeRoleReveal;
}
```

**Validation Rules:**
- Players list cannot be null or empty
- `speedrunnerCount + allyCount <= players.size()`
- `minutesBeforeRoleReveal` must be 0-60
- Fixed assignments must reference players in the players list
- Counts must be non-negative

**Construction:**
```java
StartGameCommand command = StartGameCommand.builder()
    .players(List.of(player1, player2))
    .speedrunnerCount(1)
    .allyCount(0)
    .specialRolesOnly(false)
    .resetPlayerStuff(true)
    .surpriseSpeedrunner(false)
    .minutesBeforeRoleReveal(10)
    .fixedRoleAssignments(Map.of(player1, ManhuntRoleIdentifier.HUNTER_SIMPLE))
    .build();
```

**Exception on Invalid:**
```java
try {
    StartGameCommand command = builder.build();
} catch (IllegalArgumentException e) {
    // Show user error message
    Bukkit.broadcastMessage(ChatHelper.errorMessage(e.getMessage()));
}
```

### EndGameCommand

**Purpose:** Encapsulates parameters to end a game.

**Fields:**
```java
public class EndGameCommand {
    private final GameSessionId sessionId;
    private final String reason;
}
```

**Validation:**
- Session ID cannot be null
- Reason defaults to "Game ended" if null

### DistributeRolesCommand

**Purpose:** Encapsulates parameters for role distribution algorithm.

**Fields:**
```java
public class DistributeRolesCommand {
    private final List<Player> players;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;
}
```

**Validation:**
- Players list not null or empty
- Counts within valid range
- Fixed assignments valid

### AssignRolesCommand

**Purpose:** Encapsulates role assignments to a session.

**Fields:**
```java
public class AssignRolesCommand {
    private final GameSessionId sessionId;
    private final Map<Player, ManhuntRoleIdentifier> roleAssignments;
}
```

**Validation:**
- Session ID not null
- Role assignments not null or empty

### Command Characteristics

- **Immutable** - all fields final, defensive copies of collections
- **Self-validating** - constructor throws `IllegalArgumentException` on invalid input
- **Builder pattern** - for commands with many optional parameters (StartGameCommand)
- **Simple constructor** - for commands with few required parameters (EndGameCommand)
- **No business logic** - just data + validation

---

## Application Services

Services are fine-grained, stateless components with single responsibilities. They interact with the domain, publish events, and are injected via Guice.

### GameLifecycleService

**Responsibility:** Manage session creation, starting, and ending.

**Dependencies:**
- `GameSessionManager` - session storage
- `DomainEventPublisher` - event publishing

**Operations:**

**1. createSession(List<Player> players): GameSessionId**
- Creates new game session via GameSessionManager
- Publishes `GameSessionCreatedEvent`
- Returns session ID

**2. startSession(GameSessionId, minutesBeforeReveal, surpriseSpeedrunner): void**
- Starts a game session (activates roles, schedules reveal)
- Throws `IllegalArgumentException` if session not found
- Publishes `GameStartedEvent`
- Compensation: publishes `GameStartFailedEvent` on error

**3. endSession(EndGameCommand): void**
- Ends game session
- Removes session from manager
- Domain publishes `GameEndedEvent`

**Signature:**
```java
@Singleton
public class GameLifecycleService {
    private final GameSessionManager sessionManager;
    private final DomainEventPublisher eventPublisher;
    
    @Inject
    public GameLifecycleService(GameSessionManager sessionManager, 
                                DomainEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
    }
    
    public GameSessionId createSession(List<Player> players) { ... }
    public void startSession(GameSessionId sessionId, int minutesBeforeReveal, 
                            boolean surpriseSpeedrunner) { ... }
    public void endSession(EndGameCommand command) { ... }
}
```

### RoleDistributionService

**Responsibility:** Distribute roles to players based on configuration.

**Note:** This service moves the existing 174-line `GameRolesManagement.distributeRoles()` algorithm as-is for Priority 6. The algorithm will be refactored in **Priority 10** (Extract Role Distribution Strategy). Document the new location in REFACTORING_PROGRESS.md for Priority 10 follow-up.

**Dependencies:**
- `DomainEventPublisher` - event publishing

**Operations:**

**distributeRoles(DistributeRolesCommand): Map<Player, ManhuntRoleIdentifier>**
- Executes role distribution algorithm
- Preserves existing "berzerk mode", probabilities, special role selection
- Returns map of player → role identifier
- Publishes `RolesDistributedEvent`

**Signature:**
```java
@Singleton
public class RoleDistributionService {
    private final DomainEventPublisher eventPublisher;
    
    @Inject
    public RoleDistributionService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public Map<Player, ManhuntRoleIdentifier> distributeRoles(DistributeRolesCommand command) {
        Map<Player, ManhuntRoleIdentifier> assignments = performDistribution(command);
        eventPublisher.publish(new RolesDistributedEvent(assignments));
        return assignments;
    }
    
    private Map<Player, ManhuntRoleIdentifier> performDistribution(DistributeRolesCommand command) {
        // Copy existing GameRolesManagement.distributeRoles() logic here
        // Keep berzerk mode, probabilities, special role selection intact
    }
}
```

### RoleAssignmentService

**Responsibility:** Assign distributed roles to a game session.

**Dependencies:**
- `GameSessionManager` - retrieve session
- `ManhuntRoleFactory` - create role instances
- `DomainEventPublisher` - event publishing

**Operations:**

**assignRoles(AssignRolesCommand): void**
- Creates role instances via factory
- Assigns roles to session
- Throws `IllegalArgumentException` if session not found
- Publishes `RolesAssignedEvent`

**Signature:**
```java
@Singleton
public class RoleAssignmentService {
    private final GameSessionManager sessionManager;
    private final ManhuntRoleFactory roleFactory;
    private final DomainEventPublisher eventPublisher;
    
    @Inject
    public RoleAssignmentService(GameSessionManager sessionManager,
                                  ManhuntRoleFactory roleFactory,
                                  DomainEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.roleFactory = roleFactory;
        this.eventPublisher = eventPublisher;
    }
    
    public void assignRoles(AssignRolesCommand command) {
        GameSession session = sessionManager.getSession(command.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + command.getSessionId());
        }
        
        for (Map.Entry<Player, ManhuntRoleIdentifier> entry : 
             command.getRoleAssignments().entrySet()) {
            AManhuntRole role = roleFactory.createRole(entry.getKey(), entry.getValue());
            session.assignRole(entry.getKey(), role);
        }
        
        eventPublisher.publish(new RolesAssignedEvent(command.getSessionId()));
    }
}
```

### EventHandlerRegistrationService

**Responsibility:** Register and unregister Bukkit event handlers.

**Dependencies:**
- `Plugin` - Bukkit plugin instance for handler registration

**Operations:**

**1. registerHandlers(GameSessionId, Listener...): HandlerRegistration**
- Registers Bukkit event listeners
- Returns registration handle for cleanup
- Publishes `HandlersRegisteredEvent`

**2. unregisterHandlers(HandlerRegistration): void**
- Unregisters handlers
- Idempotent - safe to call multiple times

**Signature:**
```java
@Singleton
public class EventHandlerRegistrationService {
    private final Plugin plugin;
    private final DomainEventPublisher eventPublisher;
    
    @Inject
    public EventHandlerRegistrationService(@Named("plugin") Plugin plugin,
                                           DomainEventPublisher eventPublisher) {
        this.plugin = plugin;
        this.eventPublisher = eventPublisher;
    }
    
    public HandlerRegistration registerHandlers(GameSessionId sessionId, 
                                                 Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        
        eventPublisher.publish(new HandlersRegisteredEvent(sessionId));
        return new HandlerRegistration(sessionId, Arrays.asList(listeners));
    }
    
    public void unregisterHandlers(HandlerRegistration registration) {
        for (Listener listener : registration.getListeners()) {
            HandlerList.unregisterAll(listener);
        }
    }
}

public class HandlerRegistration {
    private final GameSessionId sessionId;
    private final List<Listener> listeners;
    
    // Constructor, getters
}
```

### Service Characteristics

- **Stateless** - no instance fields except injected dependencies
- **Single responsibility** - each service does one thing
- **Thin orchestration** - delegates to domain, doesn't contain business logic
- **Event-driven** - publishes events for saga coordination
- **Constructor injection** - all dependencies via @Inject
- **@Singleton scope** - safe because stateless
- **Fail-fast** - throws exceptions on errors, sagas handle compensation

---

## Saga Coordinators

Sagas orchestrate multi-step workflows by listening to domain events and coordinating service calls. Each saga maintains in-flight workflow state and handles explicit compensation on failure.

### StartGameSaga

**Responsibility:** Orchestrate the complete "start game" workflow.

**Workflow Steps:**
1. Create game session
2. Distribute roles to players
3. Assign roles to session
4. Register Bukkit event handlers
5. Start session (activate roles, schedule reveal)

**Dependencies:**
- `GameLifecycleService`
- `RoleDistributionService`
- `RoleAssignmentService`
- `EventHandlerRegistrationService`
- `NewManhuntManager` (for handler registration)
- `DomainEventPublisher`

**Event Flow:**

```
Infrastructure calls StartGameSaga.start(command)
  ↓
Step 1: GameLifecycleService.createSession()
  ↓
GameSessionCreatedEvent published
  ↓
Step 2: StartGameSaga.onSessionCreated() → RoleDistributionService.distributeRoles()
  ↓
RolesDistributedEvent published
  ↓
Step 3: StartGameSaga.onRolesDistributed() → RoleAssignmentService.assignRoles()
  ↓
RolesAssignedEvent published
  ↓
Step 4: StartGameSaga.onRolesAssigned() → EventHandlerRegistrationService.registerHandlers()
  ↓
HandlersRegisteredEvent published
  ↓
Step 5: StartGameSaga.onHandlersRegistered() → GameLifecycleService.startSession()
  ↓
GameStartedEvent published (workflow complete)
```

**Compensation Logic:**

On failure at any step, rollback in reverse order:
1. Unregister handlers (if registered)
2. Clear role assignments (if assigned)
3. Delete session (if created)
4. Publish `GameStartFailedEvent` with compensation status

**Implementation Structure:**

```java
@Singleton
public class StartGameSaga {
    private final GameLifecycleService lifecycleService;
    private final RoleDistributionService distributionService;
    private final RoleAssignmentService assignmentService;
    private final EventHandlerRegistrationService handlerService;
    private final NewManhuntManager manhuntManager;
    private final GameSessionManager sessionManager;
    private final DomainEventPublisher eventPublisher;
    
    // Track in-flight workflows for compensation
    private final Map<GameSessionId, StartGameWorkflow> activeWorkflows = new ConcurrentHashMap<>();
    
    @Inject
    public StartGameSaga(...) { /* inject all services */ }
    
    /**
     * Entry point - initiates workflow.
     * Called by infrastructure (NewManhuntLauncher).
     */
    public GameSessionId start(StartGameCommand command) {
        StartGameWorkflow workflow = new StartGameWorkflow(command);
        
        try {
            // Step 1: Create session
            GameSessionId sessionId = lifecycleService.createSession(command.getPlayers());
            workflow.setSessionId(sessionId);
            activeWorkflows.put(sessionId, workflow);
            
            // Subsequent steps driven by events
            return sessionId;
            
        } catch (Exception e) {
            compensate(workflow, e);
            throw new GameStartException("Failed to start game", e);
        }
    }
    
    /**
     * Event handler: GameSessionCreatedEvent → distribute roles
     */
    @Subscribe
    public void onSessionCreated(GameSessionCreatedEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) return;
        
        try {
            DistributeRolesCommand distCmd = workflow.buildDistributeCommand();
            Map<Player, ManhuntRoleIdentifier> assignments = 
                distributionService.distributeRoles(distCmd);
            workflow.setRoleAssignments(assignments);
            
        } catch (Exception e) {
            compensate(workflow, e);
        }
    }
    
    /**
     * Event handler: RolesDistributedEvent → assign roles
     */
    @Subscribe
    public void onRolesDistributed(RolesDistributedEvent event) {
        StartGameWorkflow workflow = findWorkflowForEvent(event);
        if (workflow == null) return;
        
        try {
            AssignRolesCommand assignCmd = workflow.buildAssignCommand();
            assignmentService.assignRoles(assignCmd);
            
        } catch (Exception e) {
            compensate(workflow, e);
        }
    }
    
    /**
     * Event handler: RolesAssignedEvent → register handlers
     */
    @Subscribe
    public void onRolesAssigned(RolesAssignedEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) return;
        
        try {
            HandlerRegistration registration = 
                handlerService.registerHandlers(event.getSessionId(), manhuntManager);
            workflow.setHandlerRegistration(registration);
            
        } catch (Exception e) {
            compensate(workflow, e);
        }
    }
    
    /**
     * Event handler: HandlersRegisteredEvent → start session
     */
    @Subscribe
    public void onHandlersRegistered(HandlersRegisteredEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) return;
        
        try {
            lifecycleService.startSession(
                event.getSessionId(),
                workflow.getMinutesBeforeReveal(),
                workflow.isSurpriseSpeedrunner()
            );
            
            // Workflow complete
            activeWorkflows.remove(event.getSessionId());
            
        } catch (Exception e) {
            compensate(workflow, e);
        }
    }
    
    /**
     * Compensation - rollback in reverse order.
     */
    private void compensate(StartGameWorkflow workflow, Exception cause) {
        CompensationStatus status = new CompensationStatus();
        
        try {
            // Rollback step 4: unregister handlers
            if (workflow.getHandlerRegistration() != null) {
                handlerService.unregisterHandlers(workflow.getHandlerRegistration());
                status.markHandlersUnregistered();
            }
        } catch (Exception e) {
            logger.warn("Failed to unregister handlers during compensation", e);
        }
        
        try {
            // Rollback step 3: clear roles
            if (workflow.getSessionId() != null) {
                GameSession session = sessionManager.getSession(workflow.getSessionId());
                if (session != null) {
                    session.clear();
                    status.markRolesCleared();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to clear roles during compensation", e);
        }
        
        try {
            // Rollback step 1: delete session
            if (workflow.getSessionId() != null) {
                sessionManager.removeSession(workflow.getSessionId());
                status.markSessionDeleted();
            }
        } catch (Exception e) {
            logger.warn("Failed to delete session during compensation", e);
        }
        
        // Publish failure event
        eventPublisher.publish(new GameStartFailedEvent(
            workflow.getSessionId(), 
            cause, 
            status
        ));
        
        activeWorkflows.remove(workflow.getSessionId());
    }
    
    /**
     * Inner class - tracks workflow state.
     */
    private static class StartGameWorkflow {
        private final StartGameCommand command;
        private GameSessionId sessionId;
        private Map<Player, ManhuntRoleIdentifier> roleAssignments;
        private HandlerRegistration handlerRegistration;
        
        public StartGameWorkflow(StartGameCommand command) {
            this.command = command;
        }
        
        // Getters/setters
        
        public DistributeRolesCommand buildDistributeCommand() {
            return new DistributeRolesCommand(
                command.getPlayers(),
                command.getSpeedrunnerCount(),
                command.getAllyCount(),
                command.isSpecialRolesOnly(),
                command.getFixedRoleAssignments()
            );
        }
        
        public AssignRolesCommand buildAssignCommand() {
            return new AssignRolesCommand(sessionId, roleAssignments);
        }
    }
}
```

### EndGameSaga

**Responsibility:** Orchestrate game end workflow.

**Workflow Steps:**
1. End session via GameLifecycleService
2. Domain publishes `GameEndedEvent`
3. Saga performs application-layer cleanup

**Implementation:**

```java
@Singleton
public class EndGameSaga {
    private final GameLifecycleService lifecycleService;
    
    @Inject
    public EndGameSaga(GameLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }
    
    /**
     * Entry point - end game.
     */
    public void end(EndGameCommand command) {
        try {
            lifecycleService.endSession(command);
        } catch (Exception e) {
            // End game is best-effort - log but don't fail
            logger.error("Error ending game", e);
        }
    }
    
    /**
     * Event handler: GameEndedEvent (from domain)
     * Perform application-layer cleanup.
     */
    @Subscribe
    public void onGameEnded(GameEndedEvent event) {
        // Cleanup any saga state
        // Unregister any remaining handlers
    }
}
```

### Saga Characteristics

- **Event-driven** - listens to domain events via @Subscribe
- **Stateful** - maintains workflow state in activeWorkflows map
- **Explicit compensation** - rollback in reverse order on failure
- **Idempotent compensation** - safe to retry
- **Non-throwing compensation** - failures logged, not thrown
- **Thread-safe** - ConcurrentHashMap for active workflows
- **Workflow encapsulation** - inner class tracks state per workflow

---

## Dependency Injection

All services and sagas are managed by Google Guice, eliminating singleton pattern and enabling testability.

### Google Guice Module

**File:** `me.flamboyant.manhunt.application.injection.ManhuntModule`

```java
package me.flamboyant.manhunt.application.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.bukkit.plugin.Plugin;

public class ManhuntModule extends AbstractModule {
    private final Plugin plugin;
    
    public ManhuntModule(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    protected void configure() {
        // Bind plugin instance
        bind(Plugin.class).annotatedWith(Names.named("plugin")).toInstance(plugin);
        
        // Application services (all @Singleton scope)
        bind(GameLifecycleService.class);
        bind(RoleDistributionService.class);
        bind(RoleAssignmentService.class);
        bind(EventHandlerRegistrationService.class);
        
        // Sagas
        bind(StartGameSaga.class);
        bind(EndGameSaga.class);
        
        // Domain services
        bind(WinConditionEvaluator.class);
        bind(ManhuntRoleFactory.class);
        
        // Infrastructure (transition from singleton)
        bind(GameSessionManager.class).toProvider(GameSessionManagerProvider.class);
        
        // Event publisher
        bind(DomainEventPublisher.class).toProvider(EventPublisherProvider.class);
    }
    
    @Provides
    @Singleton
    public NewManhuntManager provideNewManhuntManager(
            WinConditionEvaluator evaluator,
            DragonKilledCondition dragonCondition) {
        return new NewManhuntManager(evaluator, dragonCondition);
    }
    
    @Provides
    @Singleton
    public AllSpeedrunnersDeadCondition provideAllSpeedrunnersDeadCondition() {
        return new AllSpeedrunnersDeadCondition();
    }
    
    @Provides
    @Singleton
    public DragonKilledCondition provideDragonKilledCondition() {
        return new DragonKilledCondition();
    }
}
```

### Main.java Integration

**Changes to Main.java:**

```java
public class Main extends JavaPlugin {
    private Injector injector;
    private StartGameSaga startGameSaga;
    private EndGameSaga endGameSaga;
    
    @Override
    public void onEnable() {
        // Create Guice injector
        injector = Guice.createInjector(new ManhuntModule(this));
        
        // Get saga instances
        startGameSaga = injector.getInstance(StartGameSaga.class);
        endGameSaga = injector.getInstance(EndGameSaga.class);
        
        // Register saga event handlers with domain event publisher
        DomainEventPublisher publisher = injector.getInstance(DomainEventPublisher.class);
        registerSagaEventHandlers(publisher);
        
        // Get launcher (now uses constructor injection)
        NewManhuntLauncher launcher = injector.getInstance(NewManhuntLauncher.class);
        
        // Rest of plugin initialization
        getLogger().info("Manhunt plugin enabled with DI");
    }
    
    private void registerSagaEventHandlers(DomainEventPublisher publisher) {
        // Register StartGameSaga handlers
        publisher.subscribe(GameSessionCreatedEvent.class, startGameSaga::onSessionCreated);
        publisher.subscribe(RolesDistributedEvent.class, startGameSaga::onRolesDistributed);
        publisher.subscribe(RolesAssignedEvent.class, startGameSaga::onRolesAssigned);
        publisher.subscribe(HandlersRegisteredEvent.class, startGameSaga::onHandlersRegistered);
        
        // Register EndGameSaga handlers
        publisher.subscribe(GameEndedEvent.class, endGameSaga::onGameEnded);
    }
    
    @Override
    public void onDisable() {
        // Cleanup handled by services and sagas
        getLogger().info("Manhunt plugin disabled");
    }
}
```

### NewManhuntLauncher Migration

**Changes to NewManhuntLauncher:**

```java
public class NewManhuntLauncher implements ILaunchablePlugin {
    private final StartGameSaga startGameSaga;
    private final EndGameSaga endGameSaga;
    private GameSessionId currentSessionId;
    
    // No more getInstance() - constructor injection
    @Inject
    public NewManhuntLauncher(StartGameSaga startGameSaga, EndGameSaga endGameSaga) {
        this.startGameSaga = startGameSaga;
        this.endGameSaga = endGameSaga;
    }
    
    @Override
    public boolean start() {
        if (running) {
            return false;
        }
        
        try {
            // Build command from UI parameters
            StartGameCommand command = StartGameCommand.builder()
                .players(getOnlinePlayers())
                .speedrunnerCount(speedrunnerCountParameter.getValue())
                .allyCount(allyCountParameter.getValue())
                .specialRolesOnly(specialRolesOnlyParameter.getValue())
                .resetPlayerStuff(resetPlayersStuffParameter.getValue())
                .surpriseSpeedrunner(surpriseSpeedrunnerParameter.getValue())
                .minutesBeforeRoleReveal(minutesBeforeRolesParameter.getValue())
                .fixedRoleAssignments(buildFixedAssignments())
                .build();
            
            // Start game via saga
            currentSessionId = startGameSaga.start(command);
            running = true;
            
            // Start optional plugins
            for (ILaunchablePlugin plugin : optionalPlugin) {
                plugin.start();
            }
            
            return true;
            
        } catch (IllegalArgumentException e) {
            // Command validation failed
            Bukkit.broadcastMessage(ChatHelper.errorMessage(e.getMessage()));
            return false;
        } catch (GameStartException e) {
            // Saga failed
            Bukkit.broadcastMessage(ChatHelper.errorMessage("Failed to start game: " + e.getMessage()));
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        if (!running) {
            return false;
        }
        
        // Stop optional plugins
        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.stop();
        }
        
        // End game via saga
        EndGameCommand command = new EndGameCommand(currentSessionId, "Game stopped by admin");
        endGameSaga.end(command);
        
        currentSessionId = null;
        running = false;
        return true;
    }
    
    private List<Player> getOnlinePlayers() {
        return new ArrayList<>(Common.server.getOnlinePlayers());
    }
    
    private Map<Player, ManhuntRoleIdentifier> buildFixedAssignments() {
        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        for (Map.Entry<Player, EnumParameter<ManhuntRoleIdentifier>> entry : playerRoles.entrySet()) {
            ManhuntRoleIdentifier roleId = entry.getValue().getSelectedValue();
            if (roleId != null) {
                fixed.put(entry.getKey(), roleId);
            }
        }
        return fixed;
    }
}
```

### NewManhuntManager Migration

**Changes to NewManhuntManager:**

```java
public class NewManhuntManager implements Listener {
    private GameSession session;
    private final WinConditionEvaluator winConditionEvaluator;
    private final DragonKilledCondition dragonKilledCondition;
    
    // No more getInstance() - constructor injection
    @Inject
    public NewManhuntManager(WinConditionEvaluator evaluator, 
                             DragonKilledCondition dragonCondition) {
        this.winConditionEvaluator = evaluator;
        this.dragonKilledCondition = dragonCondition;
    }
    
    // Rest of implementation remains unchanged
    // Event handlers, domain event subscriptions, etc.
}
```

### Dependency Injection Characteristics

- **Google Guice manages lifecycle** - no getInstance() calls
- **Constructor injection** - all dependencies via @Inject
- **@Singleton scope** - services are singletons but safe (stateless)
- **Main.java creates injector** - once on plugin enable
- **Infrastructure gets instances** - via injector.getInstance()
- **Easy to test** - can create test injector with mocks
- **No tight coupling** - dependencies explicit in constructors

### Maven Dependency

**Add to pom.xml:**

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>5.1.0</version>
</dependency>
```

---

## Error Handling & Compensation

Services and sagas use fail-fast with explicit compensation to ensure the system never ends up in half-initialized state.

### Error Categories

**1. Command Validation Errors**
- Thrown immediately in command constructor
- Infrastructure catches and shows user message
- No compensation needed - nothing was started

```java
try {
    StartGameCommand command = StartGameCommand.builder()...build();
} catch (IllegalArgumentException e) {
    Bukkit.broadcastMessage(ChatHelper.errorMessage(e.getMessage()));
    return false;
}
```

**2. Service Operation Errors**
- Services throw exceptions for business failures
- Sagas catch and trigger compensation
- Domain state rolled back

```java
public class GameStartException extends Exception {
    private final GameSessionId sessionId;
    private final CompensationStatus compensationStatus;
    
    public GameStartException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GameSessionId getSessionId() { return sessionId; }
    public CompensationStatus getCompensationStatus() { return compensationStatus; }
}
```

**3. Infrastructure Errors**
- Bukkit event registration failures
- Plugin initialization errors
- Handled in sagas, compensation executed

### Compensation Logic

Executed in reverse order of operations:

```java
private void compensate(StartGameWorkflow workflow, Exception cause) {
    CompensationStatus status = new CompensationStatus();
    
    try {
        // Step 5 rollback: Unregister handlers
        if (workflow.getHandlerRegistration() != null) {
            handlerService.unregisterHandlers(workflow.getHandlerRegistration());
            status.markHandlersUnregistered();
        }
    } catch (Exception e) {
        logger.warn("Failed to unregister handlers during compensation", e);
    }
    
    try {
        // Step 4 rollback: Clear role assignments
        if (workflow.getSessionId() != null) {
            GameSession session = sessionManager.getSession(workflow.getSessionId());
            if (session != null) {
                session.clear();
                status.markRolesCleared();
            }
        }
    } catch (Exception e) {
        logger.warn("Failed to clear roles during compensation", e);
    }
    
    try {
        // Step 1 rollback: Delete session
        if (workflow.getSessionId() != null) {
            sessionManager.removeSession(workflow.getSessionId());
            status.markSessionDeleted();
        }
    } catch (Exception e) {
        logger.warn("Failed to delete session during compensation", e);
    }
    
    // Publish failure event
    eventPublisher.publish(new GameStartFailedEvent(
        workflow.getSessionId(), 
        cause, 
        status
    ));
    
    activeWorkflows.remove(workflow.getSessionId());
}
```

### CompensationStatus

Tracks what was successfully rolled back:

```java
public class CompensationStatus {
    private boolean handlersUnregistered;
    private boolean rolesCleared;
    private boolean sessionDeleted;
    
    public void markHandlersUnregistered() { this.handlersUnregistered = true; }
    public void markRolesCleared() { this.rolesCleared = true; }
    public void markSessionDeleted() { this.sessionDeleted = true; }
    
    public boolean isFullyCompensated() {
        return handlersUnregistered && rolesCleared && sessionDeleted;
    }
    
    // Getters
}
```

### Failure Events

**GameStartFailedEvent:**

```java
public class GameStartFailedEvent extends DomainEvent {
    private final GameSessionId sessionId;
    private final Throwable cause;
    private final CompensationStatus compensationStatus;
    
    public GameStartFailedEvent(GameSessionId sessionId, Throwable cause, 
                               CompensationStatus status) {
        super();
        this.sessionId = sessionId;
        this.cause = cause;
        this.compensationStatus = status;
    }
    
    // Getters
}
```

**Infrastructure Handling:**

```java
@Subscribe
public void onGameStartFailed(GameStartFailedEvent event) {
    String message = "Failed to start game";
    if (event.getCompensationStatus().isFullyCompensated()) {
        message += " (rolled back successfully)";
    } else {
        message += " (WARNING: partial rollback - manual cleanup may be needed)";
    }
    Bukkit.broadcastMessage(ChatHelper.errorMessage(message));
    logger.error("Game start failed", event.getCause());
}
```

### Error Handling Characteristics

- **Fail-fast** - errors caught immediately
- **Explicit compensation** - each operation has undo logic
- **Idempotent compensation** - safe to call multiple times
- **Non-throwing compensation** - failures logged, not thrown
- **Status tracking** - compensation publishes success/failure
- **Infrastructure informed** - events allow UI to show errors
- **No partial state** - either complete success or complete rollback

---

## Testing Strategy

Application services layer is designed for testability through dependency injection and event-driven architecture.

### Test Organization

```
src/test/java/
  me/flamboyant/manhunt/application/
    commands/
      StartGameCommandTest.java
      EndGameCommandTest.java
      DistributeRolesCommandTest.java
      AssignRolesCommandTest.java
    
    services/
      GameLifecycleServiceTest.java
      RoleDistributionServiceTest.java
      RoleAssignmentServiceTest.java
      EventHandlerRegistrationServiceTest.java
    
    sagas/
      StartGameSagaTest.java
      EndGameSagaTest.java
    
    integration/
      GameStartIntegrationTest.java
      GameEndIntegrationTest.java
```

### Command Validation Tests

**Purpose:** Test command validation logic in isolation.

**Example:**

```java
public class StartGameCommandTest {
    
    @Test
    public void shouldRejectEmptyPlayerList() {
        assertThrows(IllegalArgumentException.class, () -> {
            StartGameCommand.builder()
                .players(List.of())
                .speedrunnerCount(1)
                .build();
        });
    }
    
    @Test
    public void shouldRejectTooManySpeedrunners() {
        List<Player> players = List.of(mockPlayer1, mockPlayer2);
        assertThrows(IllegalArgumentException.class, () -> {
            StartGameCommand.builder()
                .players(players)
                .speedrunnerCount(5)
                .allyCount(2)
                .build();
        });
    }
    
    @Test
    public void shouldRejectInvalidRoleRevealTime() {
        assertThrows(IllegalArgumentException.class, () -> {
            StartGameCommand.builder()
                .players(List.of(mockPlayer1))
                .minutesBeforeRoleReveal(100)
                .build();
        });
    }
    
    @Test
    public void shouldAcceptValidConfiguration() {
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .specialRolesOnly(false)
            .resetPlayerStuff(true)
            .surpriseSpeedrunner(false)
            .minutesBeforeRoleReveal(10)
            .fixedRoleAssignments(Map.of())
            .build();
        
        assertNotNull(command);
        assertEquals(2, command.getPlayers().size());
        assertEquals(1, command.getSpeedrunnerCount());
    }
    
    @Test
    public void shouldCreateImmutablePlayerList() {
        List<Player> mutableList = new ArrayList<>(List.of(mockPlayer1));
        StartGameCommand command = StartGameCommand.builder()
            .players(mutableList)
            .speedrunnerCount(1)
            .build();
        
        mutableList.add(mockPlayer2);
        
        assertEquals(1, command.getPlayers().size());
    }
}
```

### Service Unit Tests

**Purpose:** Test services in isolation with mocked dependencies.

**Example:**

```java
public class GameLifecycleServiceTest {
    private GameSessionManager mockSessionManager;
    private DomainEventPublisher mockPublisher;
    private GameLifecycleService service;
    
    @Before
    public void setUp() {
        mockSessionManager = mock(GameSessionManager.class);
        mockPublisher = mock(DomainEventPublisher.class);
        service = new GameLifecycleService(mockSessionManager, mockPublisher);
    }
    
    @Test
    public void createSession_shouldCreateAndPublishEvent() {
        // Arrange
        GameSession mockSession = mock(GameSession.class);
        GameSessionId sessionId = GameSessionId.generate();
        when(mockSessionManager.createSession()).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn(sessionId);
        
        List<Player> players = List.of(mockPlayer1);
        
        // Act
        GameSessionId result = service.createSession(players);
        
        // Assert
        assertNotNull(result);
        assertEquals(sessionId, result);
        verify(mockSessionManager).createSession();
        verify(mockPublisher).publish(argThat(event -> 
            event instanceof GameSessionCreatedEvent &&
            ((GameSessionCreatedEvent) event).getSessionId().equals(sessionId)
        ));
    }
    
    @Test
    public void startSession_shouldThrowIfSessionNotFound() {
        // Arrange
        GameSessionId id = GameSessionId.generate();
        when(mockSessionManager.getSession(id)).thenReturn(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.startSession(id, 10, false);
        });
        
        verify(mockPublisher, never()).publish(any());
    }
    
    @Test
    public void endSession_shouldRemoveSessionAndPublishEvent() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        EndGameCommand command = new EndGameCommand(sessionId, "Test end");
        
        // Act
        service.endSession(command);
        
        // Assert
        verify(mockSession).notifyGameEnded(null, "Test end");
        verify(mockSessionManager).removeSession(sessionId);
    }
}
```

### Saga Integration Tests

**Purpose:** Test saga workflow coordination with real event publisher and mocked services.

**Example:**

```java
public class StartGameSagaTest {
    private GameLifecycleService mockLifecycle;
    private RoleDistributionService mockDistribution;
    private RoleAssignmentService mockAssignment;
    private EventHandlerRegistrationService mockHandlers;
    private GameSessionManager mockSessionManager;
    private DomainEventPublisher realPublisher;
    private StartGameSaga saga;
    
    @Before
    public void setUp() {
        mockLifecycle = mock(GameLifecycleService.class);
        mockDistribution = mock(RoleDistributionService.class);
        mockAssignment = mock(RoleAssignmentService.class);
        mockHandlers = mock(EventHandlerRegistrationService.class);
        mockSessionManager = mock(GameSessionManager.class);
        
        // Use real event publisher for integration test
        realPublisher = new InMemoryEventPublisher();
        
        saga = new StartGameSaga(mockLifecycle, mockDistribution, mockAssignment, 
                                 mockHandlers, mockSessionManager, realPublisher);
        
        // Register saga event handlers
        realPublisher.subscribe(GameSessionCreatedEvent.class, saga::onSessionCreated);
        realPublisher.subscribe(RolesDistributedEvent.class, saga::onRolesDistributed);
        realPublisher.subscribe(RolesAssignedEvent.class, saga::onRolesAssigned);
        realPublisher.subscribe(HandlersRegisteredEvent.class, saga::onHandlersRegistered);
    }
    
    @Test
    public void startGame_shouldExecuteFullWorkflow() {
        // Arrange
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(mockPlayer1, mockPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .minutesBeforeRoleReveal(10)
            .build();
        
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = Map.of(
            mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE,
            mockPlayer2, ManhuntRoleIdentifier.HUNTER_SIMPLE
        );
        
        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        
        // Act
        GameSessionId result = saga.start(command);
        
        // Assert
        assertEquals(sessionId, result);
        
        // Verify workflow executed in order
        InOrder inOrder = inOrder(mockLifecycle, mockDistribution, 
                                   mockAssignment, mockHandlers);
        inOrder.verify(mockLifecycle).createSession(command.getPlayers());
        inOrder.verify(mockDistribution).distributeRoles(any(DistributeRolesCommand.class));
        inOrder.verify(mockAssignment).assignRoles(any(AssignRolesCommand.class));
        inOrder.verify(mockHandlers).registerHandlers(eq(sessionId), any());
        inOrder.verify(mockLifecycle).startSession(eq(sessionId), eq(10), eq(false));
    }
    
    @Test
    public void startGame_shouldCompensateOnDistributionFailure() {
        // Arrange
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(mockPlayer1))
            .speedrunnerCount(1)
            .build();
        
        GameSessionId sessionId = GameSessionId.generate();
        GameSession mockSession = mock(GameSession.class);
        
        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockDistribution.distributeRoles(any()))
            .thenThrow(new RuntimeException("Distribution failed"));
        
        // Subscribe to failure event
        AtomicReference<GameStartFailedEvent> failureEvent = new AtomicReference<>();
        realPublisher.subscribe(GameStartFailedEvent.class, failureEvent::set);
        
        // Act & Assert
        assertThrows(GameStartException.class, () -> saga.start(command));
        
        // Verify compensation executed
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);
        
        // Verify failure event published
        assertNotNull(failureEvent.get());
        assertEquals(sessionId, failureEvent.get().getSessionId());
        assertTrue(failureEvent.get().getCompensationStatus().isFullyCompensated());
    }
    
    @Test
    public void startGame_shouldCompensateOnHandlerRegistrationFailure() {
        // Arrange
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(mockPlayer1))
            .speedrunnerCount(1)
            .build();
        
        GameSessionId sessionId = GameSessionId.generate();
        Map<Player, ManhuntRoleIdentifier> assignments = Map.of(
            mockPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
        );
        GameSession mockSession = mock(GameSession.class);
        
        when(mockLifecycle.createSession(any())).thenReturn(sessionId);
        when(mockDistribution.distributeRoles(any())).thenReturn(assignments);
        when(mockSessionManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockHandlers.registerHandlers(any(), any()))
            .thenThrow(new RuntimeException("Handler registration failed"));
        
        // Act & Assert
        assertThrows(GameStartException.class, () -> saga.start(command));
        
        // Verify compensation executed in reverse order
        verify(mockSession).clear();
        verify(mockSessionManager).removeSession(sessionId);
    }
}
```

### End-to-End Integration Tests

**Purpose:** Test full workflow with real Guice injector and minimal mocking.

**Example:**

```java
public class GameStartIntegrationTest {
    private Injector injector;
    private StartGameSaga saga;
    private GameSessionManager sessionManager;
    
    @Before
    public void setUp() {
        // Create test Guice module with real implementations
        injector = Guice.createInjector(new TestManhuntModule());
        saga = injector.getInstance(StartGameSaga.class);
        sessionManager = injector.getInstance(GameSessionManager.class);
    }
    
    @Test
    public void shouldStartGameSuccessfully() {
        // Arrange
        Player testPlayer1 = createTestPlayer("Player1");
        Player testPlayer2 = createTestPlayer("Player2");
        
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(testPlayer1, testPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .specialRolesOnly(false)
            .resetPlayerStuff(false)
            .surpriseSpeedrunner(false)
            .minutesBeforeRoleReveal(10)
            .fixedRoleAssignments(Map.of())
            .build();
        
        // Act
        GameSessionId sessionId = saga.start(command);
        
        // Assert
        assertNotNull(sessionId);
        
        GameSession session = sessionManager.getSession(sessionId);
        assertNotNull(session);
        assertEquals(2, session.getPlayers().size());
        assertEquals(2, session.getAllRoles().size());
        
        // Verify role distribution
        long speedrunnerCount = session.getAllRoles().values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .count();
        assertEquals(1, speedrunnerCount);
        
        long hunterCount = session.getAllRoles().values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.HUNTER)
            .count();
        assertEquals(1, hunterCount);
    }
    
    @Test
    public void shouldRespectFixedRoleAssignments() {
        // Arrange
        Player testPlayer1 = createTestPlayer("Player1");
        Player testPlayer2 = createTestPlayer("Player2");
        
        StartGameCommand command = StartGameCommand.builder()
            .players(List.of(testPlayer1, testPlayer2))
            .speedrunnerCount(1)
            .allyCount(0)
            .fixedRoleAssignments(Map.of(
                testPlayer1, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT
            ))
            .build();
        
        // Act
        GameSessionId sessionId = saga.start(command);
        
        // Assert
        GameSession session = sessionManager.getSession(sessionId);
        AManhuntRole player1Role = session.getRole(testPlayer1);
        
        assertEquals(ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT, 
                     player1Role.getRoleIdentifier());
    }
}
```

### Testing Characteristics

- **Commands:** Validation tested in isolation
- **Services:** Unit tested with mocked dependencies (Mockito)
- **Sagas:** Integration tested with real event publisher, mocked services
- **End-to-end:** Full workflow with real Guice injector
- **Compensation:** Explicitly tested by simulating failures
- **Coverage target:** 80%+ for services and sagas
- **Mock framework:** Mockito
- **Test doubles:** Mocks for services, real event publisher for saga tests

---

## Migration Path

Step-by-step migration from current singleton architecture to application services layer.

### Step 1: Add Google Guice Dependency

**File:** `pom.xml`

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>5.1.0</version>
</dependency>
```

### Step 2: Create Command Classes

**Priority Order:**
1. `StartGameCommand` (most complex)
2. `EndGameCommand` (simple)
3. `DistributeRolesCommand`
4. `AssignRolesCommand`

**Implementation:**
- Start with simple constructor validation
- Add builder pattern for StartGameCommand
- Write command validation tests first (TDD)

### Step 3: Create Application Services

**Priority Order:**
1. `GameLifecycleService` - foundational
2. `RoleDistributionService` - move existing algorithm from GameRolesManagement
3. `RoleAssignmentService`
4. `EventHandlerRegistrationService`

**Implementation:**
- Create service interfaces first (optional but recommended)
- Implement with stateless design
- Write unit tests with mocked dependencies
- Services publish events for saga coordination

**Note for RoleDistributionService:**
- Copy `GameRolesManagement.distributeRoles()` method as-is
- Preserve all existing logic (berzerk mode, probabilities, special roles)
- Mark `GameRolesManagement` as @Deprecated
- Update `REFACTORING_PROGRESS.md` Priority 10 notes with new location:
  ```
  Priority 10: Extract Role Distribution Strategy
  Note: Complex distribution algorithm now located in RoleDistributionService.performDistribution()
  Refactor this method to use Strategy pattern as part of Priority 10.
  ```

### Step 4: Create Domain Events

**New Events Needed:**
1. `GameSessionCreatedEvent` - after session creation
2. `RolesDistributedEvent` - after role distribution
3. `RolesAssignedEvent` - after role assignment
4. `HandlersRegisteredEvent` - after Bukkit handler registration
5. `GameStartFailedEvent` - on saga compensation

**Implementation:**
- Extend existing `DomainEvent` base class from Priority 5
- Follow existing event patterns
- Include relevant context (session ID, assignments, etc.)

### Step 5: Create Saga Coordinators

**Priority Order:**
1. `StartGameSaga` - most complex workflow
2. `EndGameSaga` - simpler cleanup workflow

**Implementation:**
- Create saga with event handler methods
- Implement workflow state tracking (inner class)
- Implement compensation logic
- Write integration tests with real event publisher
- Test compensation scenarios explicitly

### Step 6: Create Guice Module

**File:** `ManhuntModule.java`

**Implementation:**
- Bind all services
- Bind sagas
- Bind domain services (WinConditionEvaluator, ManhuntRoleFactory)
- Use @Provides for complex bindings (NewManhuntManager)
- Transition GameSessionManager from singleton

### Step 7: Update Main.java

**Changes:**
1. Create Guice injector in `onEnable()`
2. Get saga instances from injector
3. Register saga event handlers with DomainEventPublisher
4. Get NewManhuntLauncher from injector (instead of getInstance())

**Implementation:**
- Test that plugin starts successfully with injector
- Verify all services injected correctly
- Test that sagas receive events

### Step 8: Migrate NewManhuntLauncher

**Changes:**
1. Remove `getInstance()` singleton pattern
2. Add constructor with `@Inject` annotation
3. Inject `StartGameSaga` and `EndGameSaga`
4. Update `start()` to build `StartGameCommand` and call saga
5. Update `stop()` to build `EndGameCommand` and call saga

**Implementation:**
- Keep UI parameter logic unchanged
- Replace direct `NewManhuntManager` calls with saga calls
- Add try-catch for command validation and saga exceptions
- Test game start/stop via UI

### Step 9: Migrate NewManhuntManager

**Changes:**
1. Remove `getInstance()` singleton pattern
2. Add constructor with `@Inject` annotation
3. Inject `WinConditionEvaluator` and `DragonKilledCondition`
4. Keep event handler logic unchanged

**Implementation:**
- Manager becomes Bukkit event listener only
- Domain event handlers stay in place
- Injected via Guice in `Main.java`

### Step 10: Remove Deprecated Code

**Classes to Remove/Deprecate:**
1. Mark `GameRolesManagement` as @Deprecated (keep for now, remove in Priority 10)
2. Remove `getInstance()` methods from all managers
3. Remove singleton instance fields

**Implementation:**
- Search for all `.getInstance()` calls
- Verify all replaced with DI
- Run all tests to ensure nothing broken

### Step 11: Update Documentation

**Files to Update:**
1. `REFACTORING_PROGRESS.md` - Mark Priority 6 complete
2. `REFACTORING_PROGRESS.md` - Update Priority 10 with RoleDistributionService location
3. `bounded-contexts-map.md` - Add application services context
4. Create architecture diagram showing new layer

### Migration Validation Checklist

- [ ] All tests pass (domain + new application layer tests)
- [ ] No `getInstance()` calls remain (except deprecated GameRolesManagement)
- [ ] Plugin starts successfully with Guice injector
- [ ] Game can start via UI (calls StartGameSaga)
- [ ] Game can stop via UI (calls EndGameSaga)
- [ ] Compensation logic works (test failure scenarios)
- [ ] Domain events flow correctly through sagas
- [ ] No memory leaks (event handlers cleaned up)
- [ ] Documentation updated

---

## Success Criteria

Priority 6 is complete when:

### Functional Criteria

- ✅ **Use cases clearly defined** - Start Game, End Game, Distribute Roles, Assign Roles each have explicit services
- ✅ **Application services orchestrate domain** - Services coordinate domain operations, don't contain business logic
- ✅ **Commands/queries separated** - Commands for mutations (StartGameCommand), queries via repositories
- ✅ **No business logic in services** - Logic lives in domain (roles, session), services only orchestrate
- ✅ **Services testable in isolation** - All services have unit tests with mocked dependencies

### Technical Criteria

- ✅ **Singleton managers eliminated** - No getInstance() calls (except deprecated GameRolesManagement)
- ✅ **Dependency injection working** - Google Guice manages all service lifecycle
- ✅ **Sagas orchestrate workflows** - StartGameSaga and EndGameSaga coordinate multi-step flows
- ✅ **Explicit compensation logic** - Failures trigger rollback in reverse order
- ✅ **Domain events integrated** - Sagas listen to domain events from Priority 5
- ✅ **Transaction boundaries defined** - Each use case is atomic (all-or-nothing)

### Quality Criteria

- ✅ **Test coverage 80%+** - Commands, services, and sagas well-tested
- ✅ **No regression** - All existing tests still pass
- ✅ **Game start/stop works** - Can start and stop game via UI
- ✅ **Compensation works** - Tested failure scenarios roll back correctly
- ✅ **Documentation updated** - REFACTORING_PROGRESS.md, architecture docs updated

### Code Quality Criteria

- ✅ **Services are stateless** - No mutable instance fields (except injected dependencies)
- ✅ **Commands are immutable** - All fields final, defensive copies
- ✅ **Sagas handle errors gracefully** - No exceptions escape saga compensation
- ✅ **Clear separation of concerns** - Infrastructure → Application → Domain boundaries respected
- ✅ **YAGNI applied** - No speculative abstractions, only what's needed now

---

## Appendix: New Domain Events

### GameSessionCreatedEvent

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.List;

public class GameSessionCreatedEvent extends DomainEvent {
    private final GameSessionId sessionId;
    private final List<Player> players;
    
    public GameSessionCreatedEvent(GameSessionId sessionId, List<Player> players) {
        super();
        this.sessionId = sessionId;
        this.players = List.copyOf(players);
    }
    
    public GameSessionId getSessionId() { return sessionId; }
    public List<Player> getPlayers() { return players; }
}
```

### RolesDistributedEvent

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Map;

public class RolesDistributedEvent extends DomainEvent {
    private final Map<Player, ManhuntRoleIdentifier> assignments;
    
    public RolesDistributedEvent(Map<Player, ManhuntRoleIdentifier> assignments) {
        super();
        this.assignments = Map.copyOf(assignments);
    }
    
    public Map<Player, ManhuntRoleIdentifier> getAssignments() { return assignments; }
}
```

### RolesAssignedEvent

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

public class RolesAssignedEvent extends DomainEvent {
    private final GameSessionId sessionId;
    
    public RolesAssignedEvent(GameSessionId sessionId) {
        super();
        this.sessionId = sessionId;
    }
    
    public GameSessionId getSessionId() { return sessionId; }
}
```

### HandlersRegisteredEvent

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

public class HandlersRegisteredEvent extends DomainEvent {
    private final GameSessionId sessionId;
    
    public HandlersRegisteredEvent(GameSessionId sessionId) {
        super();
        this.sessionId = sessionId;
    }
    
    public GameSessionId getSessionId() { return sessionId; }
}
```

### GameStartFailedEvent

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.application.CompensationStatus;
import me.flamboyant.manhunt.domain.game.GameSessionId;

public class GameStartFailedEvent extends DomainEvent {
    private final GameSessionId sessionId;
    private final Throwable cause;
    private final CompensationStatus compensationStatus;
    
    public GameStartFailedEvent(GameSessionId sessionId, Throwable cause, 
                               CompensationStatus status) {
        super();
        this.sessionId = sessionId;
        this.cause = cause;
        this.compensationStatus = status;
    }
    
    public GameSessionId getSessionId() { return sessionId; }
    public Throwable getCause() { return cause; }
    public CompensationStatus getCompensationStatus() { return compensationStatus; }
}
```

---

**End of Design Specification**
