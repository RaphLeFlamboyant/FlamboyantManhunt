# Manhunt Plugin - Problems Priority Summary

**Last Updated:** 2026-06-18  
**Status:** Pre-refactoring baseline

---

## Overview

This document lists all identified architectural and design problems in priority order for DDD-based refactoring. Each problem includes severity, impact, and estimated effort to fix.

---

## Priority 1: No Aggregate Root / Game Instance Concept
**Severity:** CRITICAL  
**Category:** Architecture - Core Domain  
**Effort:** 6-8 hours

### Problem
No encapsulation of "a game." Global static `GameData` class holds all game state. No concept of game instances, boundaries, or lifecycle.

```java
// Current - Global mutable state
public class GameData {
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    public static HashMap<Player, Location> overworldLocationBeforePortal = new HashMap<>();
    public static int remainingSpeedrunner;
}
```

### Why It's #1
- **Defines system boundaries** - Without it, no clear scope of "what is the game"
- **Enables lifecycle management** - Start, run, cleanup cycles undefined
- **Blocks all other patterns** - Can't implement state machines, proper event handling, or win conditions without a context
- **Prevents testability** - Global state makes unit testing impossible
- **Prevents concurrency** - Can't run multiple games simultaneously
- **Root cause of many other issues** - Event leaks, unclear ownership, scattered logic

### Impact
- Cannot run multiple concurrent games
- Testing requires global state reset
- Memory leaks from accumulated event listeners
- Unclear ownership of game state
- Hidden dependencies throughout codebase

### DDD Mapping
**Missing Aggregate Root:** `GameSession` should be the aggregate root containing:
- Player-Role mappings (entities)
- Portal location tracking (value objects)
- Game configuration (value object)
- Game phase/state (entity)
- Win condition tracking (domain service)

---

## Priority 2: No Domain Model - Anemic Entities
**Severity:** HIGH  
**Category:** Architecture - Domain Layer  
**Effort:** 8-10 hours

### Problem
Domain entities (`AManhuntRole`, `GameData`) have no behavior. All business logic scattered in managers and services. Procedural code with object wrappers.

```java
// Current - Anemic role
public abstract class AManhuntRole {
    protected Player owner;
    public abstract ManhuntRoleType getRoleType();
    // No domain behavior
}

// Business logic in external manager
public class NewManhuntManager {
    public void onEntityDamage(EntityDamageEvent event) {
        if (GameData.playerClassList.get(player).getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            // Role logic outside the role
        }
    }
}
```

### Why It's #2
- **Rich domain model is DDD foundation** - Entities should contain behavior, not just data
- **Logic scattered across managers** - Hard to find where rules are implemented
- **Violates Tell Don't Ask** - Code asks roles for data, then decides what to do
- **Poor encapsulation** - Roles can't enforce their own invariants

### Impact
- Business logic fragmented across multiple classes
- Roles are just data containers
- Hard to understand role-specific rules
- Duplication of similar logic across managers

### DDD Mapping
**Should be Rich Entities:**
```java
class SpeedrunnerRole extends Role {
    // Domain behavior
    public RoleDeathOutcome handleDeath(DamageEvent event) { ... }
    public boolean canUseCompass() { ... }
    public CompassTarget selectTarget(List<Player> hunters) { ... }
}
```

---

## Priority 3: Missing Bounded Context Boundaries
**Severity:** HIGH  
**Category:** Architecture - Strategic Design  
**Effort:** 4-6 hours (design) + ongoing

### Problem
No clear boundaries between subdomains. Game management, role logic, UI, event handling all mixed together. No ubiquitous language defined.

```java
// Mixed concerns
public class NewManhuntLauncher implements ILaunchablePlugin {
    // Configuration UI
    private BooleanParameter resetPlayersStuffParameter;
    
    // Game startup
    public boolean start() { ... }
    
    // Dragon death detection (win condition)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) { ... }
    
    // Player state management
    private void resetPlayerState(Player player) { ... }
}
```

### Why It's #3
- **Enables parallel work** - Clear boundaries let multiple developers work safely
- **Reduces coupling** - Each context has its own models
- **Clarifies responsibilities** - Each subsystem has clear purpose
- **Prevents big ball of mud** - Boundaries contain complexity

### Impact
- Changes ripple unpredictably across system
- Hard to understand what each class is responsible for
- Cannot work on subsystems independently
- No clear place to add new features

### DDD Mapping
**Should have separate Bounded Contexts:**

1. **Game Management Context** - Session lifecycle, configuration
2. **Role Domain Context** - Role abilities, win conditions, role logic
3. **Player Tracking Context** - Location tracking, player state
4. **Infrastructure Context** - Bukkit events, persistence, UI

Each context should have:
- Own entities/value objects
- Own repository interfaces  
- Own domain services
- Context mapping between them

---

## Priority 4: String-Based Type Detection (Primitive Obsession)
**Severity:** HIGH  
**Category:** Design - Type Safety  
**Effort:** 2-3 hours

### Problem
Role type detection uses string parsing instead of explicit type relationships. Fragile and error-prone.

```java
// In GameRolesManagement.java
if (roleId.toString().contains("SPEEDRUNNER"))
if (roleId.toString().contains("ALLY"))
if (roleId.toString().contains("HUNTER"))

// In ManhuntRoleIdentifier.java
public enum ManhuntRoleIdentifier {
    // Bad design but f*ck : Names must start with corresponding ManhuntRoleType
    HUNTER_SIMPLE,
    SPEEDRUNNER_SIMPLE,
    // ...
}
```

### Why It's #4
- **High risk of bugs** - Rename a role, break logic
- **No compile-time safety** - Refactoring is dangerous
- **Violates DDD Value Object pattern** - Types should be explicit
- **Easy to fix** - Low effort, high value

### Impact
- Renaming roles breaks role distribution logic
- IDE refactoring tools can't help
- No way to find all role type checks
- Forces awkward naming conventions

### DDD Mapping
**Should use Value Objects:**
```java
public enum ManhuntRoleIdentifier {
    HUNTER_SIMPLE(RoleType.HUNTER),
    SPEEDRUNNER_SIMPLE(RoleType.SPEEDRUNNER);
    
    private final RoleType roleType;
    
    public RoleType getRoleType() { return roleType; }
}
```

---

## Priority 5: No Domain Events
**Severity:** MEDIUM-HIGH  
**Category:** Architecture - Domain Events  
**Effort:** 6-8 hours

### Problem
Important domain events (player death, dragon kill, role reveal) handled via Bukkit events directly in roles/managers. No explicit domain event model. Tight coupling to infrastructure.

```java
// Current - Infrastructure event mixed with domain logic
@EventHandler
public void onEntityDamage(EntityDamageEvent event) {
    if (event.getEntityType() != EntityType.PLAYER) return;
    Player player = (Player)event.getEntity();
    
    if (GameData.playerClassList.get(player).getRoleType() == ManhuntRoleType.SPEEDRUNNER
            && player.getHealth() - event.getFinalDamage() <= 0) {
        // Domain logic: speedrunner died
        if (--GameData.remainingSpeedrunner == 0) {
            stopGame("L'équipe SPEEDRUNNER a perdu !!!");
        }
    }
}
```

### Why It's #5
- **Couples domain to infrastructure** - Domain logic depends on Bukkit
- **Hard to test** - Can't test "speedrunner died" logic without Bukkit events
- **Poor separation** - Domain events buried in event handlers
- **Violates DDD patterns** - Domain events should be explicit

### Impact
- Domain logic tied to Minecraft/Bukkit
- Can't test game rules without full Bukkit environment
- Event handling scattered across multiple classes
- No audit trail of domain events

### DDD Mapping
**Should have Domain Events:**
```java
// Domain events
class SpeedrunnerDiedEvent extends DomainEvent { ... }
class DragonKilledEvent extends DomainEvent { ... }
class RoleRevealedEvent extends DomainEvent { ... }

// Event handlers
class GameEndingPolicy implements DomainEventHandler<SpeedrunnerDiedEvent> {
    public void handle(SpeedrunnerDiedEvent event) {
        if (event.remainingCount() == 0) {
            gameSession.endGame(Outcome.HUNTERS_WIN);
        }
    }
}
```

---

## Priority 6: Missing Domain Services Layer
**Severity:** MEDIUM  
**Category:** Architecture - Application Layer  
**Effort:** 5-7 hours

### Problem
No clear application/domain services layer. Logic scattered between "Manager" singletons, roles, and launchers. No orchestration layer.

```java
// Current - Logic spread across managers
NewManhuntManager.getInstance()  // Singleton with game logic
NewManhuntLauncher.getInstance() // Singleton with startup logic
GameRolesManagement.getInstance() // Singleton with role assignment
```

### Why It's #6
- **No use case orchestration** - Complex operations spread across classes
- **Hard to understand workflows** - "Start game" touches 5+ classes
- **Poor transaction boundaries** - No clear consistency boundaries
- **Singletons everywhere** - Can't inject dependencies or test

### Impact
- Use cases (start game, end game, assign roles) fragmented
- Hard to trace execution flow
- Cannot test workflows in isolation
- Tight coupling between managers

### DDD Mapping
**Should have Application Services:**
```java
// Application services orchestrate use cases
class StartGameService {
    private final GameSessionRepository sessions;
    private final RoleDistributionService roleDistribution;
    private final EventPublisher eventPublisher;
    
    public GameSession startGame(StartGameCommand command) {
        // Orchestrate use case
        GameSession session = new GameSession(command.getConfiguration());
        roleDistribution.assignRoles(session, command.getPlayers());
        session.start();
        sessions.save(session);
        eventPublisher.publish(new GameStartedEvent(session.getId()));
        return session;
    }
}
```

---

## Priority 7: Factory Violates Open/Closed Principle
**Severity:** MEDIUM  
**Category:** Design - Extensibility  
**Effort:** 3-4 hours

### Problem
`ManhuntRoleFactory` is a giant switch statement. Every new role requires modifying the factory. Not extensible.

```java
public static AManhuntRole createRole(Player owner, ManhuntRoleIdentifier roleIdentifier) {
    switch (roleIdentifier) {
        case SPEEDRUNNER_SIMPLE: return new SpeedrunnerRole(owner);
        case SPEEDRUNNER_SWAPPER: return new SpeedrunnerSwapperRole(owner);
        // ... 16 more cases
        default: return new HunterRole(owner);
    }
}
```

### Why It's #7
- **Prevents extensions** - Can't add roles without modifying core code
- **Merge conflicts** - Everyone touches factory when adding roles
- **Violates OCP** - Not open for extension, requires modification
- **Not DDD-friendly** - Factories should discover implementations

### Impact
- Adding roles touches central factory
- Can't plugin external role implementations
- Risk of merge conflicts
- Tight coupling to all role implementations

### DDD Mapping
**Should use Registry Pattern:**
```java
class RoleFactory {
    private Map<RoleIdentifier, RoleConstructor> constructors = new HashMap<>();
    
    public void register(RoleIdentifier id, RoleConstructor constructor) {
        constructors.put(id, constructor);
    }
    
    public Role create(RoleIdentifier id, Player owner) {
        return constructors.get(id).construct(owner);
    }
}

// Self-registration
@RegisterRole(SPEEDRUNNER_CHECKPOINT)
class CheckpointSpeedrunnerRole extends SpeedrunnerRole { ... }
```

---

## Priority 8: No Explicit State Machine
**Severity:** MEDIUM  
**Category:** Design - State Management  
**Effort:** 4-5 hours

### Problem
Game phases (configuring, countdown, role hidden, revealed, ended) implicit. State transitions scattered. No validation of legal transitions.

```java
// Current - Implicit state
Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
    for (Player player : GameData.playerClassList.keySet()) {
        role.start(); // When does this happen? What state are we in?
    }
}, (roleRevealDelayInMinutes * 60 + 1) * 20);
```

### Why It's #8
- **State transitions implicit** - Hard to know current phase
- **Can't validate transitions** - No guard against invalid state changes
- **Testing difficult** - Can't easily test phase-specific behavior
- **Poor debugging** - Can't see what phase game is in

### Impact
- Game state unclear
- Can't prevent invalid operations (e.g., start already-started game)
- Hard to add phase-specific features
- No audit trail of phase transitions

### DDD Mapping
**Should use State Pattern / Explicit Phases:**
```java
enum GamePhase {
    CONFIGURING, COUNTDOWN, ROLE_HIDDEN, ROLE_REVEALED, DRAGON_FIGHT, ENDED
}

class GameSession extends AggregateRoot {
    private GamePhase currentPhase;
    
    public void transitionTo(GamePhase newPhase) {
        validateTransition(currentPhase, newPhase);
        GamePhase oldPhase = currentPhase;
        currentPhase = newPhase;
        publishEvent(new PhaseChangedEvent(oldPhase, newPhase));
    }
}
```

---

## Priority 9: Event Listener Lifecycle Leaks
**Severity:** MEDIUM  
**Category:** Infrastructure - Resource Management  
**Effort:** 3-4 hours

### Problem
Roles manually register/unregister Bukkit event listeners. Inconsistent patterns. Easy to leak listeners if unregister forgotten.

```java
// Different patterns across roles
Common.server.getPluginManager().registerEvents(this, Common.plugin);
PlayerInteractEvent.getHandlerList().unregister(this);

// Static listener list in HunterRole
public static List<IHunterWinConditionModifier> winconModifiers = new ArrayList<>();
```

### Why It's #9
- **Memory leaks** - Forgotten unregister accumulates listeners
- **Inconsistent patterns** - Each role does it differently
- **Hard to debug** - Leaks only show up over time
- **Cleanup complexity** - Must remember to unregister everywhere

### Impact
- Memory leaks after multiple games
- Handlers firing for ended games
- Unpredictable behavior
- Server performance degradation

### DDD Mapping
**Should use Lifecycle Management:**
```java
class RoleEventManager {
    private Map<Role, List<HandlerRegistration>> registrations = new HashMap<>();
    
    public void registerForRole(Role role, Object listener) {
        HandlerRegistration reg = eventBus.register(listener);
        registrations.computeIfAbsent(role, k -> new ArrayList<>()).add(reg);
    }
    
    public void unregisterAllForRole(Role role) {
        List<HandlerRegistration> regs = registrations.remove(role);
        if (regs != null) regs.forEach(HandlerRegistration::unregister);
    }
}

// Tied to aggregate lifecycle
class GameSession {
    public void end() {
        roles.forEach(role -> eventManager.unregisterAllForRole(role));
    }
}
```

---

## Priority 10: Complex Role Distribution Algorithm
**Severity:** MEDIUM  
**Category:** Design - Domain Logic  
**Effort:** 4-5 hours

### Problem
`GameRolesManagement.distributeRoles()` is 174 lines of complex logic. Hard to understand, test, or modify. Business rules embedded in procedural code.

```java
private void distributeRoles(...) {
    // 100+ lines of complex role assignment logic
    // Hard-coded probabilities
    // "Berzerk mode" when counts don't match
    // Mutation of input parameters
}
```

### Why It's #10
- **Hard to test** - Complex nested logic
- **Hard to understand** - Probabilities and special cases unclear
- **Hard to modify** - Changing rules risks breaking existing logic
- **No business rule visibility** - Rules buried in code

### Impact
- Role balancing changes are risky
- Can't easily test different distribution strategies
- Hard to explain role selection to players
- Business rules not explicit

### DDD Mapping
**Should use Strategy Pattern with Specifications:**
```java
interface RoleDistributionStrategy {
    RoleAssignments distribute(List<Player> players, DistributionRules rules);
}

class DistributionRules {
    private final Specification<Player> speedrunnerEligibility;
    private final int minSpeedrunners;
    private final int maxSpeedrunners;
    private final boolean specialRolesOnly;
}

// Testable, swappable strategies
class BalancedDistribution implements RoleDistributionStrategy { ... }
class RandomDistribution implements RoleDistributionStrategy { ... }
```

---

## Priority 11: No Win Condition Abstraction
**Severity:** LOW-MEDIUM  
**Category:** Design - Domain Logic  
**Effort:** 3-4 hours

### Problem
Win conditions hardcoded in event handlers. No explicit win condition model. Logic scattered across `NewManhuntManager`, `NewManhuntLauncher`, and role classes.

```java
// Win condition in NewManhuntManager
if (--GameData.remainingSpeedrunner == 0) {
    stopGame("L'équipe SPEEDRUNNER a perdu !!!");
}

// Win condition in NewManhuntLauncher  
if (dragon.getHealth() - event.getFinalDamage() <= 0) {
    Bukkit.broadcastMessage("Le speedrunner a gagné !!!");
}

// Win condition modifier in HunterRole
for (IHunterWinConditionModifier modifier : winconModifiers) {
    wincon &= modifier.isHunterWinPossible();
}
```

### Why It's #11
- **Win conditions implicit** - Hard to see what causes game end
- **Scattered logic** - No single place defining win rules
- **Hard to add custom conditions** - No extension point
- **Poor testability** - Can't test win conditions in isolation

### Impact
- Win condition logic fragmented
- Hard to add new win conditions
- Can't test win scenarios easily
- Unclear what constitutes victory

### DDD Mapping
**Should use Specification Pattern:**
```java
interface WinCondition {
    boolean isMet(GameSession session);
    Set<RoleType> getWinners();
    String getDescription();
}

class AllSpeedrunnersDeadCondition implements WinCondition {
    public boolean isMet(GameSession session) {
        return session.getRemainingSpeedrunners() == 0;
    }
    
    public Set<RoleType> getWinners() {
        return Set.of(RoleType.HUNTER);
    }
}

// Composable
class CompositeWinCondition implements WinCondition {
    private List<WinCondition> conditions;
    // AND/OR logic
}
```

---

## Priority 12: Tight Coupling to Framework
**Severity:** LOW-MEDIUM  
**Category:** Architecture - Infrastructure  
**Effort:** 6-8 hours

### Problem
Heavy dependency on `FlamboyantPluginTools` submodule for UI and configuration. No abstraction layer. Hard to test without framework.

```java
import me.flamboyant.FlamboyantPlugin;
import me.flamboyant.gui.ConfigurablePluginListener;
import me.flamboyant.utils.*;
import me.flamboyant.configurable.parameters.*;
```

### Why It's #12
- **Hard to test** - Requires full framework
- **Hard to migrate** - Locked to specific framework
- **Framework dependency** - External submodule required
- **Not critical to core** - UI/config is peripheral concern

### Impact
- Testing requires framework
- Can't swap UI implementations
- Locked into framework patterns
- Framework must be maintained

### DDD Mapping
**Should use Anti-Corruption Layer:**
```java
// Domain interface
interface GameConfigurationProvider {
    GameConfiguration getConfiguration(Player initiator);
}

// Framework adapter (infrastructure layer)
class FlamboyantConfigAdapter implements GameConfigurationProvider {
    public GameConfiguration getConfiguration(Player initiator) {
        // Translate framework concepts to domain concepts
    }
}

// Test adapter
class MockConfigProvider implements GameConfigurationProvider {
    // In-memory configuration for testing
}
```

---

## Priority 13: Weak Role Abstraction
**Severity:** LOW  
**Category:** Design - Inheritance  
**Effort:** 2-3 hours

### Problem
`AManhuntRole` base class provides minimal shared behavior. Each role handles its own event listeners, cooldowns, and lifecycle. Lots of duplication.

```java
public abstract class AManhuntRole {
    protected boolean doStart();
    protected boolean doStop();
    // Minimal shared logic
}

// Each role implements similar patterns
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (owner != event.getPlayer()) return;
    if (!event.hasItem()) return;
    if (owner.hasCooldown(Material.COMPASS)) return;
    owner.setCooldown(Material.COMPASS, 30 * 20);
    // ... actual logic
}
```

### Why It's #13
- **Duplication** - Similar patterns across roles
- **Boilerplate** - Each role handles event filtering
- **Not critical** - Doesn't block other refactorings
- **Can wait** - Fix after aggregate root established

### Impact
- Code duplication across role implementations
- Each role handles boilerplate
- No enforcement of common patterns
- Harder to add cross-cutting concerns

### DDD Mapping
**Should use Template Method / Composition:**
```java
abstract class Role extends Entity {
    private final AbilityManager abilities;
    private final CooldownManager cooldowns;
    
    protected void registerAbility(Ability ability) {
        abilities.register(ability);
    }
    
    // Template method
    protected abstract void defineAbilities();
}

// Usage
class SpeedrunnerRole extends Role {
    protected void defineAbilities() {
        registerAbility(new CompassTrackingAbility(15 * 60));
    }
}
```

---

## Summary Table

| Priority | Problem | Severity | Category | Effort | DDD Impact |
|----------|---------|----------|----------|--------|------------|
| 1 | No Aggregate Root | CRITICAL | Architecture | 6-8h | Blocks all patterns |
| 2 | Anemic Domain Model | HIGH | Architecture | 8-10h | Foundation of DDD |
| 3 | No Bounded Contexts | HIGH | Architecture | 4-6h | Strategic design |
| 4 | String-Based Types | HIGH | Design | 2-3h | Value objects |
| 5 | No Domain Events | MEDIUM-HIGH | Architecture | 6-8h | Event-driven domain |
| 6 | Missing Domain Services | MEDIUM | Architecture | 5-7h | Application layer |
| 7 | Factory Violates OCP | MEDIUM | Design | 3-4h | Factory pattern |
| 8 | No State Machine | MEDIUM | Design | 4-5h | State pattern |
| 9 | Event Listener Leaks | MEDIUM | Infrastructure | 3-4h | Resource management |
| 10 | Complex Distribution | MEDIUM | Design | 4-5h | Strategy pattern |
| 11 | No Win Condition Model | LOW-MEDIUM | Design | 3-4h | Specification pattern |
| 12 | Framework Coupling | LOW-MEDIUM | Architecture | 6-8h | Anti-corruption layer |
| 13 | Weak Role Abstraction | LOW | Design | 2-3h | Template method |

**Total Estimated Effort:** 57-76 hours

---

## Recommended Implementation Order (DDD Approach)

### Phase 1: Core Domain (Weeks 1-2)
1. **Priority 1:** Create GameSession aggregate root
2. **Priority 2:** Enrich domain model (roles with behavior)
3. **Priority 3:** Define bounded contexts
4. **Priority 4:** Fix primitive obsession (string types)

### Phase 2: Domain Events & Services (Week 3)
5. **Priority 5:** Implement domain events
6. **Priority 6:** Create application services layer
7. **Priority 8:** Add explicit state machine

### Phase 3: Tactical Patterns (Week 4)
8. **Priority 7:** Refactor factory
9. **Priority 10:** Extract role distribution strategy
10. **Priority 11:** Model win conditions

### Phase 4: Infrastructure & Polish (Week 5)
11. **Priority 9:** Fix event listener lifecycle
12. **Priority 12:** Add anti-corruption layer
13. **Priority 13:** Improve role abstraction

---

## Next Steps

1. Review this priority list
2. Start with **Priority 1** (GameSession aggregate)
3. Use the companion `REFACTORING_PROGRESS.md` to track implementation
4. Validate each change with tests before moving to next priority

---

**See Also:**
- `ARCHITECTURE_ANALYSIS.md` - Detailed analysis with code examples
- `REFACTORING_PROGRESS.md` - Implementation tracking document
