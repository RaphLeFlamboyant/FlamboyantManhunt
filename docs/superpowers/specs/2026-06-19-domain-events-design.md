# Domain Events Implementation - Design Specification

**Date:** 2026-06-19  
**Priority:** 5 (Domain Events)  
**Status:** Design Approved  
**Estimated Effort:** 6-8 hours

---

## Overview

This specification defines the implementation of a domain events system for the Manhunt plugin following Domain-Driven Design principles. The goal is to decouple domain logic from infrastructure (Bukkit), enable testing without Bukkit dependencies, and prevent event listener memory leaks through proper lifecycle management.

## Objectives

1. **Decouple domain from infrastructure**: Domain events represent business moments, not Bukkit events
2. **Enable testing**: Test win conditions, game flows without Bukkit
3. **Prevent memory leaks**: Session-scoped handlers that clean up automatically
4. **Maintain pragmatism**: Hybrid approach - keep Bukkit events for simple interactions, domain events for business logic

## Design Decisions

### Architectural Approach: Lightweight Event Bus

**Selected:** Approach 1 - Lightweight in-memory event bus  
**Rejected alternatives:**
- Aggregate event queue pattern (over-engineered, no event sourcing requirement)
- Full Bukkit bridge isolation (too much indirection for pragmatic needs)

**Rationale:** Simple synchronous event bus fits single-threaded Bukkit environment, integrates naturally with GameSession aggregate, requires minimal infrastructure code (~200 lines), and solves the actual problems without over-engineering.

### Key Characteristics

- **Synchronous event publishing**: Events handled immediately in same thread (simpler, maintains transaction boundaries)
- **Manual handler registration**: Explicit subscriptions at session start (clear, no magic)
- **Session-scoped handlers**: New handlers per game session, cleaned up on session end (prevents leaks)
- **Type-safe subscriptions**: Generics ensure compile-time safety
- **Zero external dependencies**: Pure Java + Bukkit Player references

### Hybrid Bukkit Strategy

**Keep Bukkit @EventHandler for:**
- UI interactions (compass clicks, item use)
- State updates (portal entry tracking)
- Role abilities (non-business events)

**Translate to domain events:**
- Speedrunner death (business-critical, needs testing)
- Dragon killed (win condition)
- Game lifecycle (start/end/phase changes)
- Role assignment/reveal (game setup moments)

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    GameSession                           │
│                  (Aggregate Root)                        │
│  ┌────────────────────────────────────────────────┐    │
│  │         DomainEventPublisher                    │    │
│  │  - subscribe(eventType, handler)                │    │
│  │  - publish(event)                               │    │
│  │  - unsubscribeAll()                             │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  Methods:                                                │
│  - notifyGameStarted()                                  │
│  - notifySpeedrunnerDied(player)                        │
│  - notifyDragonKilled(killer)                           │
│  - notifyGameEnded(outcome, reason)                     │
│  - end() → cleans up all handlers                       │
└─────────────────────────────────────────────────────────┘
                            │
                            │ publishes
                            ▼
                    ┌───────────────┐
                    │  DomainEvent  │
                    │  (abstract)   │
                    └───────────────┘
                            △
                ┌───────────┼───────────────┐
                │           │               │
    ┌───────────────┐ ┌─────────────┐ ┌──────────────┐
    │ GameStarted   │ │ Speedrunner │ │ DragonKilled │
    │     Event     │ │  DiedEvent  │ │    Event     │
    └───────────────┘ └─────────────┘ └──────────────┘
                            │
                            │ handled by
                            ▼
                ┌──────────────────────────┐
                │  DomainEventHandler<T>   │
                │  - handle(T event)       │
                └──────────────────────────┘
                            △
                            │
                ┌───────────┴────────────┐
                │                        │
    ┌──────────────────────┐  ┌───────────────────┐
    │ WinConditionCheck    │  │ GameCleanup       │
    │      Policy          │  │    Policy         │
    └──────────────────────┘  └───────────────────┘
```

---

## Core Infrastructure Components

### 1. Base Domain Event Class

**Package:** `me.flamboyant.manhunt.domain.event`

```java
public abstract class DomainEvent {
    private final String eventId;
    private final long occurredAtMillis;
    private final GameSessionId sessionId;
    
    protected DomainEvent(GameSessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        this.eventId = UUID.randomUUID().toString();
        this.occurredAtMillis = System.currentTimeMillis();
        this.sessionId = sessionId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public long getOccurredAtMillis() {
        return occurredAtMillis;
    }
    
    public GameSessionId getSessionId() {
        return sessionId;
    }
}
```

**Fields:**
- `eventId`: Unique identifier (UUID) for each event instance
- `occurredAtMillis`: Timestamp when event occurred (for ordering, debugging)
- `sessionId`: Which game session produced this event

### 2. Event Publisher Interface

**Package:** `me.flamboyant.manhunt.domain.event`

```java
public interface DomainEventPublisher {
    /**
     * Subscribe a handler to a specific event type.
     * Handler will be called synchronously when events of this type are published.
     */
    <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler);
    
    /**
     * Publish an event to all registered handlers.
     * Handlers are called synchronously in registration order.
     */
    void publish(DomainEvent event);
    
    /**
     * Unsubscribe all handlers. Called when session ends to prevent leaks.
     */
    void unsubscribeAll();
}
```

### 3. Event Handler Interface

**Package:** `me.flamboyant.manhunt.domain.event`

```java
@FunctionalInterface
public interface DomainEventHandler<T extends DomainEvent> {
    /**
     * Handle a domain event.
     * @param event The event to handle
     * @throws Exception if handling fails (will be logged, won't stop other handlers)
     */
    void handle(T event);
}
```

Functional interface enables lambda subscriptions: `publisher.subscribe(EventType.class, this::onEvent)`

### 4. In-Memory Event Publisher Implementation

**Package:** `me.flamboyant.manhunt.domain.event`

```java
public class InMemoryEventPublisher implements DomainEventPublisher {
    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();
    
    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler) {
        if (eventType == null || handler == null) {
            throw new IllegalArgumentException("Event type and handler cannot be null");
        }
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        if (handlers.isEmpty()) {
            // Session already ended, handlers cleared
            Common.plugin.getLogger().warning(
                "Event published after session ended: " + event.getClass().getSimpleName()
            );
            return;
        }
        
        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (DomainEventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // Log but continue to next handler
                    Common.plugin.getLogger().severe(
                        "Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage()
                    );
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }
}
```

**Error handling:** Exceptions in one handler don't prevent other handlers from running. Errors are logged for debugging.

---

## Domain Events (Concrete Classes)

All events in package: `me.flamboyant.manhunt.domain.event`

### Game Lifecycle Events

#### GameStartedEvent

```java
public class GameStartedEvent extends DomainEvent {
    private final Set<Player> players;
    private final int totalSpeedrunners;
    
    public GameStartedEvent(GameSessionId sessionId, Set<Player> players, int totalSpeedrunners) {
        super(sessionId);
        if (players == null) {
            throw new IllegalArgumentException("Players cannot be null");
        }
        this.players = Set.copyOf(players); // Defensive copy
        this.totalSpeedrunners = totalSpeedrunners;
    }
    
    public Set<Player> getPlayers() {
        return players;
    }
    
    public int getTotalSpeedrunners() {
        return totalSpeedrunners;
    }
}
```

**When published:** After all roles assigned and game initialization complete  
**Published by:** `GameSession.notifyGameStarted()`  
**Used for:** Logging, metrics, initializing game-wide state

#### GameEndedEvent

```java
public class GameEndedEvent extends DomainEvent {
    private final WinOutcome outcome;
    private final String reason;
    
    public GameEndedEvent(GameSessionId sessionId, WinOutcome outcome, String reason) {
        super(sessionId);
        if (outcome == null || reason == null) {
            throw new IllegalArgumentException("Outcome and reason cannot be null");
        }
        this.outcome = outcome;
        this.reason = reason;
    }
    
    public WinOutcome getOutcome() {
        return outcome;
    }
    
    public String getReason() {
        return reason;
    }
}
```

**When published:** When win condition met or game manually stopped  
**Published by:** `GameSession.notifyGameEnded()`  
**Used for:** Broadcasting results, cleaning up resources, recording stats

#### PhaseChangedEvent

```java
public class PhaseChangedEvent extends DomainEvent {
    private final GamePhase fromPhase;
    private final GamePhase toPhase;
    
    public PhaseChangedEvent(GameSessionId sessionId, GamePhase from, GamePhase to) {
        super(sessionId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Phases cannot be null");
        }
        this.fromPhase = from;
        this.toPhase = to;
    }
    
    public GamePhase getFromPhase() {
        return fromPhase;
    }
    
    public GamePhase getToPhase() {
        return toPhase;
    }
}
```

**When published:** When game transitions between phases (CONFIGURING → COUNTDOWN → ROLE_HIDDEN → ROLE_REVEALED → ENDED)  
**Published by:** `GameSession.transitionTo()` (Priority 8 - State Machine)  
**Used for:** Phase-specific behavior, UI updates, logging  
**Note:** Will be implemented alongside Priority 8 (Explicit State Machine)

### Role/Player Events

#### RoleAssignedEvent

```java
public class RoleAssignedEvent extends DomainEvent {
    private final Player player;
    private final ManhuntRoleIdentifier roleIdentifier;
    
    public RoleAssignedEvent(GameSessionId sessionId, Player player, ManhuntRoleIdentifier roleId) {
        super(sessionId);
        if (player == null || roleId == null) {
            throw new IllegalArgumentException("Player and role identifier cannot be null");
        }
        this.player = player;
        this.roleIdentifier = roleId;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return roleIdentifier;
    }
}
```

**When published:** When role assigned to player during game setup  
**Published by:** `GameSession.assignRole()`  
**Used for:** Logging role distribution, notifying players, auditing

#### RolesRevealedEvent

```java
public class RolesRevealedEvent extends DomainEvent {
    public RolesRevealedEvent(GameSessionId sessionId) {
        super(sessionId);
    }
}
```

**When published:** When surprise mode timer expires and roles are revealed  
**Published by:** `GameSession.notifyRolesRevealed()`  
**Used for:** Triggering role.start() for speedrunners, UI updates

#### SpeedrunnerDiedEvent

```java
public class SpeedrunnerDiedEvent extends DomainEvent {
    private final Player player;
    private final int remainingSpeedrunners;
    
    public SpeedrunnerDiedEvent(GameSessionId sessionId, Player player, int remaining) {
        super(sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
        this.remainingSpeedrunners = remaining;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getRemainingSpeedrunners() {
        return remainingSpeedrunners;
    }
}
```

**When published:** When speedrunner takes fatal damage  
**Published by:** `GameSession.notifySpeedrunnerDied()`  
**Used for:** Win condition evaluation, broadcasting death message

**Critical business event** - This is the most important event for testing game logic without Bukkit.

### Win Condition Events

#### DragonKilledEvent

```java
public class DragonKilledEvent extends DomainEvent {
    private final Player killer; // May be null if environmental kill
    
    public DragonKilledEvent(GameSessionId sessionId, Player killer) {
        super(sessionId);
        this.killer = killer; // Nullable
    }
    
    public Player getKiller() {
        return killer;
    }
    
    public boolean hasKiller() {
        return killer != null;
    }
}
```

**When published:** When Ender Dragon health reaches zero  
**Published by:** Infrastructure layer detecting dragon death  
**Used for:** Win condition evaluation, victory celebration

#### WinConditionMetEvent

```java
public class WinConditionMetEvent extends DomainEvent {
    private final WinOutcome outcome;
    
    public WinConditionMetEvent(GameSessionId sessionId, WinOutcome outcome) {
        super(sessionId);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        this.outcome = outcome;
    }
    
    public WinOutcome getOutcome() {
        return outcome;
    }
}
```

**When published:** When any win condition evaluates to true  
**Published by:** `WinConditionEvaluator.evaluate()`  
**Used for:** Triggering game end, broadcasting results

---

## Integration with GameSession

### GameSession Modifications

**New field:**
```java
private final DomainEventPublisher eventPublisher;
```

**Updated constructors:**
```java
// Default constructor (for backward compatibility)
public GameSession(GameSessionId id) {
    this(id, new InMemoryPortalTracker(), new InMemoryEventPublisher());
}

// Constructor with dependency injection (for testing)
public GameSession(GameSessionId id, PortalTracker portalTracker, DomainEventPublisher eventPublisher) {
    if (id == null) {
        throw new IllegalArgumentException("Session ID cannot be null");
    }
    if (portalTracker == null) {
        throw new IllegalArgumentException("PortalTracker cannot be null");
    }
    if (eventPublisher == null) {
        throw new IllegalArgumentException("EventPublisher cannot be null");
    }
    this.id = id;
    this.playerRoles = new HashMap<>();
    this.portalTracker = portalTracker;
    this.eventPublisher = eventPublisher;
    this.remainingSpeedrunners = 0;
}
```

**Expose publisher for handler registration:**
```java
public DomainEventPublisher getEventPublisher() {
    return eventPublisher;
}
```

**Domain event publishing methods:**
```java
public void notifyGameStarted() {
    eventPublisher.publish(new GameStartedEvent(id, getPlayers(), remainingSpeedrunners));
}

public void notifySpeedrunnerDied(Player player) {
    int remaining = decrementSpeedrunners();
    eventPublisher.publish(new SpeedrunnerDiedEvent(id, player, remaining));
}

public void notifyDragonKilled(Player killer) {
    eventPublisher.publish(new DragonKilledEvent(id, killer));
}

public void notifyRolesRevealed() {
    eventPublisher.publish(new RolesRevealedEvent(id));
}

public void notifyGameEnded(WinOutcome outcome, String reason) {
    eventPublisher.publish(new GameEndedEvent(id, outcome, reason));
}
```

**Updated assignRole to publish event:**
```java
public void assignRole(Player player, AManhuntRole role) {
    if (player == null) {
        throw new IllegalArgumentException("Player cannot be null");
    }
    if (role == null) {
        throw new IllegalArgumentException("Role cannot be null");
    }
    playerRoles.put(player, role);
    eventPublisher.publish(new RoleAssignedEvent(id, player, role.getRoleIdentifier()));
}
```

**Updated end() to cleanup handlers:**
```java
public void end() {
    eventPublisher.unsubscribeAll(); // Prevent memory leaks
    clear();
}
```

---

## Event Handler Registration

### Registration Pattern

Handlers are registered when a game session starts. Registration happens in `NewManhuntManager.startGame()`:

```java
public boolean startGame(GameSession session, int roleRevealDelayInMinutes, boolean speedrunnerSurprise) {
    this.session = session;
    
    // Register domain event handlers for this session
    registerEventHandlers(session);
    
    // ... rest of game startup logic
    session.notifyGameStarted(); // Publish game started event
    
    return true;
}

private void registerEventHandlers(GameSession session) {
    DomainEventPublisher publisher = session.getEventPublisher();
    
    // Win condition checking
    publisher.subscribe(SpeedrunnerDiedEvent.class, this::onSpeedrunnerDied);
    publisher.subscribe(DragonKilledEvent.class, this::onDragonKilled);
    
    // Win condition met
    publisher.subscribe(WinConditionMetEvent.class, this::onWinConditionMet);
    
    // Game lifecycle
    publisher.subscribe(GameEndedEvent.class, this::onGameEnded);
    
    // Role lifecycle (if needed)
    publisher.subscribe(RolesRevealedEvent.class, this::onRolesRevealed);
}
```

### Handler Implementations

**Win condition checking:**
```java
private void onSpeedrunnerDied(SpeedrunnerDiedEvent event) {
    // Evaluate all win conditions
    Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(session);
    if (outcome.isPresent()) {
        session.notifyGameEnded(outcome.get(), "All speedrunners eliminated!");
    }
}

private void onDragonKilled(DragonKilledEvent event) {
    // Mark dragon killed in condition
    dragonKilledCondition.markDragonKilled();
    
    // Evaluate win conditions
    Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(session);
    if (outcome.isPresent()) {
        String message = event.hasKiller() 
            ? event.getKiller().getDisplayName() + " defeated the dragon!"
            : "The dragon was defeated!";
        session.notifyGameEnded(outcome.get(), message);
    }
}
```

**Win condition met:**
```java
private void onWinConditionMet(WinConditionMetEvent event) {
    // Broadcast result
    Bukkit.broadcastMessage(ChatHelper.importantMessage(event.getOutcome().getDescription()));
}
```

**Game ended cleanup:**
```java
private void onGameEnded(GameEndedEvent event) {
    // Stop all roles
    for (AManhuntRole role : session.getAllRoles().values()) {
        role.stop();
    }
    
    // Unregister Bukkit listeners
    EntityDamageEvent.getHandlerList().unregister(this);
    
    // Broadcast reason
    Bukkit.broadcastMessage(ChatHelper.importantMessage(event.getReason()));
    
    // Session cleanup (will call eventPublisher.unsubscribeAll())
    session.end();
}
```

**Roles revealed (surprise mode):**
```java
private void onRolesRevealed(RolesRevealedEvent event) {
    // Start speedrunner abilities
    for (AManhuntRole role : session.getAllRoles().values()) {
        if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            role.start();
        }
    }
}
```

### Handler Lifecycle

1. **Registration:** Handlers registered at session start via `registerEventHandlers()`
2. **Execution:** Handlers called synchronously when events published
3. **Cleanup:** All handlers unregistered when `session.end()` called via `unsubscribeAll()`

**Prevents memory leaks** because handlers are scoped to session lifetime, not global.

---

## Bukkit Event Bridge

### Bridge Pattern

Infrastructure layer (`NewManhuntManager`, `NewManhuntLauncher`) listens to Bukkit events and translates them to domain events when they represent business moments.

### Speedrunner Death Bridge

**In NewManhuntManager:**
```java
@EventHandler
public void onEntityDamage(EntityDamageEvent event) {
    if (event.getEntityType() != EntityType.PLAYER) return;
    
    Player player = (Player) event.getEntity();
    
    // Check if player is in this session
    if (!session.hasRole(player)) return;
    
    AManhuntRole role = session.getRole(player);
    
    // Only process speedrunner damage
    if (!(role instanceof SpeedrunnerRole)) return;
    
    // Check if damage is fatal
    SpeedrunnerRole speedrunner = (SpeedrunnerRole) role;
    DamageOutcome outcome = speedrunner.handleDamage(event.getFinalDamage());
    
    if (outcome.died()) {
        // Publish domain event
        session.notifySpeedrunnerDied(player);
    }
}
```

**Translation:**
1. Bukkit `EntityDamageEvent` (infrastructure)
2. Check if relevant to domain (is it a player? is it a speedrunner? is it fatal?)
3. Call domain method `handleDamage()` (business logic in role)
4. Publish domain event `SpeedrunnerDiedEvent` (domain layer)
5. Domain handlers react (win condition check)

### Dragon Death Bridge

**In NewManhuntLauncher:**
```java
@EventHandler
public void onEntityDamage(EntityDamageEvent event) {
    if (event.getEntityType() != EntityType.ENDER_DRAGON) return;
    
    EnderDragon dragon = (EnderDragon) event.getEntity();
    
    // Check if this damage kills the dragon
    if (dragon.getHealth() - event.getFinalDamage() <= 0) {
        // Determine killer
        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Player) {
                killer = (Player) damager;
            }
        }
        
        // Get active session and publish domain event
        GameSession session = GameSessionManager.getInstance().getActiveSession();
        if (session != null) {
            session.notifyDragonKilled(killer);
        }
    }
}
```

### What Stays as Bukkit Events

**No domain event translation for:**
- Compass click handling (UI interaction, stays in SpeedrunnerRole/HunterRole)
- Portal entry tracking (state update, stays in roles)
- Item interactions (role abilities, stays in roles)
- Player chat, movement, inventory (not business events)

**Only critical business moments become domain events.**

---

## Testing Strategy

### Unit Testing Domain Events

#### Testing Event Publishing

```java
@Test
public void testSpeedrunnerDeathPublishesEvent() {
    // Arrange
    MockEventPublisher mockPublisher = new MockEventPublisher();
    GameSession session = new GameSession(
        GameSessionId.generate(), 
        new InMemoryPortalTracker(),
        mockPublisher
    );
    
    Player player = mock(Player.class);
    when(player.getName()).thenReturn("TestPlayer");
    session.setRemainingSpeedrunners(1);
    
    // Act
    session.notifySpeedrunnerDied(player);
    
    // Assert
    List<DomainEvent> published = mockPublisher.getPublishedEvents();
    assertEquals(1, published.size());
    assertTrue(published.get(0) instanceof SpeedrunnerDiedEvent);
    
    SpeedrunnerDiedEvent event = (SpeedrunnerDiedEvent) published.get(0);
    assertEquals(player, event.getPlayer());
    assertEquals(0, event.getRemainingSpeedrunners());
}
```

#### Testing Event Handlers

```java
@Test
public void testWinConditionCheckTriggersGameEnd() {
    // Arrange
    MockEventPublisher mockPublisher = new MockEventPublisher();
    GameSession session = new GameSession(
        GameSessionId.generate(),
        new InMemoryPortalTracker(),
        mockPublisher
    );
    session.setRemainingSpeedrunners(0); // All dead
    
    WinConditionEvaluator evaluator = new WinConditionEvaluator(
        List.of(new AllSpeedrunnersDeadCondition())
    );
    
    // Register handler
    session.getEventPublisher().subscribe(SpeedrunnerDiedEvent.class, event -> {
        evaluator.evaluate(session).ifPresent(outcome ->
            session.notifyGameEnded(outcome, "Test")
        );
    });
    
    Player player = mock(Player.class);
    
    // Act
    session.notifySpeedrunnerDied(player);
    
    // Assert - verify both events published
    List<DomainEvent> events = mockPublisher.getPublishedEvents();
    assertEquals(2, events.size());
    assertTrue(events.get(0) instanceof SpeedrunnerDiedEvent);
    assertTrue(events.get(1) instanceof GameEndedEvent);
}
```

### Integration Testing

```java
@Test
public void testFullEventChainFromDeathToGameEnd() {
    // Arrange: Real event publisher, real win condition evaluator
    GameSession session = new GameSession(GameSessionId.generate());
    
    WinConditionEvaluator evaluator = new WinConditionEvaluator(
        List.of(new AllSpeedrunnersDeadCondition())
    );
    
    // Capture all events
    List<DomainEvent> capturedEvents = new ArrayList<>();
    session.getEventPublisher().subscribe(SpeedrunnerDiedEvent.class, event -> {
        capturedEvents.add(event);
        // Simulate manager handler
        evaluator.evaluate(session).ifPresent(outcome ->
            session.notifyGameEnded(outcome, "All speedrunners eliminated")
        );
    });
    session.getEventPublisher().subscribe(GameEndedEvent.class, capturedEvents::add);
    
    Player player = mock(Player.class);
    session.setRemainingSpeedrunners(1);
    
    // Act
    session.notifySpeedrunnerDied(player);
    
    // Assert: Event chain completed
    assertEquals(2, capturedEvents.size());
    assertTrue(capturedEvents.get(0) instanceof SpeedrunnerDiedEvent);
    assertTrue(capturedEvents.get(1) instanceof GameEndedEvent);
    
    GameEndedEvent endEvent = (GameEndedEvent) capturedEvents.get(1);
    assertEquals("All speedrunners eliminated", endEvent.getReason());
}
```

### Test Utilities

#### MockEventPublisher

**Package:** `me.flamboyant.manhunt.domain.event` (test sources)

```java
public class MockEventPublisher implements DomainEventPublisher {
    private final List<DomainEvent> publishedEvents = new ArrayList<>();
    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();
    
    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, DomainEventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        publishedEvents.add(event);
        
        // Also call handlers for integration tests
        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (DomainEventHandler handler : eventHandlers) {
                handler.handle(event);
            }
        }
    }
    
    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }
    
    // Test helper methods
    public List<DomainEvent> getPublishedEvents() {
        return List.copyOf(publishedEvents);
    }
    
    public <T extends DomainEvent> List<T> getEventsOfType(Class<T> eventType) {
        return publishedEvents.stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .collect(Collectors.toList());
    }
    
    public boolean hasEventOfType(Class<? extends DomainEvent> eventType) {
        return publishedEvents.stream().anyMatch(eventType::isInstance);
    }
    
    public void clearPublishedEvents() {
        publishedEvents.clear();
    }
}
```

---

## Error Handling & Edge Cases

### Handler Exception Handling

**Principle:** One failing handler should not break the entire event chain.

**Implementation:** Exceptions caught and logged in `InMemoryEventPublisher.publish()`:

```java
for (DomainEventHandler handler : eventHandlers) {
    try {
        handler.handle(event);
    } catch (Exception e) {
        // Log error but continue to next handler
        Common.plugin.getLogger().severe(
            "Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage()
        );
        e.printStackTrace();
        // Continue loop
    }
}
```

**Testing:** Verify that if handler A throws exception, handler B still executes.

### Session Lifecycle Edge Cases

**Event published after session ends:**
```java
public void publish(DomainEvent event) {
    if (handlers.isEmpty()) {
        // Session already ended, handlers cleared
        Common.plugin.getLogger().warning(
            "Event published after session ended: " + event.getClass().getSimpleName()
        );
        return;
    }
    // ... normal publishing
}
```

**Multiple cleanup calls:**
```java
public void end() {
    if (eventPublisher != null) {
        eventPublisher.unsubscribeAll();
    }
    clear();
    // Idempotent - safe to call multiple times
}
```

### Null Safety

All event constructors validate required fields:
```java
public SpeedrunnerDiedEvent(GameSessionId sessionId, Player player, int remaining) {
    super(sessionId); // Validates sessionId != null
    if (player == null) {
        throw new IllegalArgumentException("Player cannot be null");
    }
    this.player = player;
    this.remainingSpeedrunners = remaining;
}
```

Base class validates:
```java
protected DomainEvent(GameSessionId sessionId) {
    if (sessionId == null) {
        throw new IllegalArgumentException("Session ID cannot be null");
    }
    // ...
}
```

### Event Ordering

**Within same event type:** Handlers execute in registration order (deterministic).

**Across different event types:** No ordering guarantees. If ordering matters:
- Use causality: SpeedrunnerDiedEvent handler publishes WinConditionMetEvent
- Check session state: If GameEndedEvent already published, ignore subsequent events

**Example:** Both SpeedrunnerDiedEvent and DragonKilledEvent handlers check win conditions independently. First to detect win publishes GameEndedEvent. Second handler sees game already ended via session state check.

### Memory Leak Prevention

**Handler registration lifecycle:**
- ✅ Handlers registered when session starts
- ✅ Handlers automatically cleared when `session.end()` called
- ✅ No static handler registrations
- ✅ Handler closures reference session (session owns handlers, not vice versa)
- ✅ GameSession itself can be garbage collected after end()

**Verification:** Manual testing with multiple game starts/stops, checking heap dump for accumulated handlers.

---

## Package Structure

```
src/main/java/me/flamboyant/manhunt/
├── domain/
│   ├── event/                          (NEW PACKAGE)
│   │   ├── DomainEvent.java            (abstract base)
│   │   ├── DomainEventPublisher.java   (interface)
│   │   ├── DomainEventHandler.java     (interface)
│   │   ├── InMemoryEventPublisher.java (implementation)
│   │   ├── GameStartedEvent.java
│   │   ├── GameEndedEvent.java
│   │   ├── PhaseChangedEvent.java      (for Priority 8)
│   │   ├── RoleAssignedEvent.java
│   │   ├── RolesRevealedEvent.java
│   │   ├── SpeedrunnerDiedEvent.java
│   │   ├── DragonKilledEvent.java
│   │   └── WinConditionMetEvent.java
│   ├── game/
│   │   ├── GameSession.java            (MODIFIED - add event publisher)
│   │   └── GameSessionId.java
│   └── ...
├── NewManhuntManager.java              (MODIFIED - register handlers)
├── NewManhuntLauncher.java             (MODIFIED - dragon death bridge)
└── ...

src/test/java/me/flamboyant/manhunt/
└── domain/
    └── event/
        ├── MockEventPublisher.java     (test utility)
        ├── GameSessionEventTest.java   (unit tests)
        └── EventIntegrationTest.java   (integration tests)
```

---

## Implementation Checklist

### Phase 1: Core Infrastructure (2-3 hours)
- [ ] Create `domain.event` package
- [ ] Implement `DomainEvent` abstract class
- [ ] Implement `DomainEventPublisher` interface
- [ ] Implement `DomainEventHandler` interface
- [ ] Implement `InMemoryEventPublisher` class
- [ ] Write unit tests for `InMemoryEventPublisher`

### Phase 2: Concrete Events (1-2 hours)
- [ ] Implement `GameStartedEvent`
- [ ] Implement `GameEndedEvent`
- [ ] Implement `RoleAssignedEvent`
- [ ] Implement `RolesRevealedEvent`
- [ ] Implement `SpeedrunnerDiedEvent`
- [ ] Implement `DragonKilledEvent`
- [ ] Implement `WinConditionMetEvent`
- [ ] Write unit tests for each event class

### Phase 3: GameSession Integration (1-2 hours)
- [ ] Add `eventPublisher` field to `GameSession`
- [ ] Update constructors with dependency injection
- [ ] Add `getEventPublisher()` method
- [ ] Implement `notifyGameStarted()`
- [ ] Implement `notifySpeedrunnerDied()`
- [ ] Implement `notifyDragonKilled()`
- [ ] Implement `notifyRolesRevealed()`
- [ ] Implement `notifyGameEnded()`
- [ ] Update `assignRole()` to publish `RoleAssignedEvent`
- [ ] Update `end()` to call `unsubscribeAll()`
- [ ] Write integration tests for GameSession events

### Phase 4: Handler Registration (1-2 hours)
- [ ] Implement `registerEventHandlers()` in `NewManhuntManager`
- [ ] Implement `onSpeedrunnerDied()` handler
- [ ] Implement `onDragonKilled()` handler
- [ ] Implement `onWinConditionMet()` handler
- [ ] Implement `onGameEnded()` handler
- [ ] Implement `onRolesRevealed()` handler (if needed)
- [ ] Call `registerEventHandlers()` in `startGame()`
- [ ] Call `session.notifyGameStarted()` after setup

### Phase 5: Bukkit Event Bridges (1 hour)
- [ ] Update `NewManhuntManager.onEntityDamage()` to publish `SpeedrunnerDiedEvent`
- [ ] Update `NewManhuntLauncher.onEntityDamage()` to publish `DragonKilledEvent`
- [ ] Remove direct win condition checks from Bukkit handlers
- [ ] Verify domain events flow correctly from Bukkit events

### Phase 6: Testing & Validation (1-2 hours)
- [ ] Implement `MockEventPublisher` test utility
- [ ] Write unit tests for all event handlers
- [ ] Write integration test for full event chain (death → win → end)
- [ ] Test error handling (handler exceptions)
- [ ] Test lifecycle cleanup (no memory leaks)
- [ ] Manual testing: start/stop multiple games, verify no leaks
- [ ] Update `REFACTORING_PROGRESS.md` with completion status

---

## Success Criteria

✅ **All significant domain changes publish events**
- Game start/end publishes events
- Speedrunner death publishes event
- Dragon killed publishes event
- Role assignment publishes event

✅ **Domain events decoupled from infrastructure events**
- Domain layer doesn't depend on Bukkit event classes
- Can test win condition logic without Bukkit
- Infrastructure translates Bukkit → Domain events

✅ **Event handlers contain domain logic**
- Win condition checking in domain event handlers
- Game cleanup in domain event handlers
- Business rules in handlers, not in Bukkit listeners

✅ **Domain can be tested without Bukkit**
- MockEventPublisher enables pure unit tests
- Win condition logic testable independently
- Event chains verifiable without Minecraft server

✅ **Event flow documented**
- This spec documents all events and handlers
- Code comments explain event causality
- Tests demonstrate expected event sequences

✅ **No memory leaks**
- Handlers cleaned up when session ends
- No static handler registrations
- Multiple game cycles don't accumulate handlers

---

## Future Enhancements

### Phase Changes (Priority 8)
When implementing explicit state machine (Priority 8):
- `PhaseChangedEvent` already defined
- Publish from `GameSession.transitionTo(GamePhase)`
- Handlers can react to phase transitions

### Event Persistence
If event sourcing needed later:
- Add `EventStore` interface
- Implement file/database persistence
- Replay events to rebuild game state

### Async Handlers
If performance issues arise:
- Add `@Async` annotation to handlers
- Execute handlers in thread pool
- Maintain ordering where needed via dependencies

### Domain Event Audit Log
For debugging/analytics:
- Subscribe logging handler to all event types
- Write events to file with timestamps
- Analyze game flows post-mortem

---

## References

- **Priority 5 Description:** `docs/PROBLEMS_PRIORITY_SUMMARY.md` (lines 210-264)
- **Bounded Contexts:** `docs/architecture/bounded-contexts-map.md`
- **GameSession Aggregate:** `docs/architecture/gamesession-aggregate.md`
- **Win Condition Context:** Already uses domain model, will integrate naturally
- **DDD Reference:** Eric Evans "Domain-Driven Design" - Chapter on Domain Events
- **DDD Reference:** Vaughn Vernon "Implementing Domain-Driven Design" - Chapter 8: Domain Events

---

**Last Updated:** 2026-06-19  
**Next Step:** Implementation via `writing-plans` skill
