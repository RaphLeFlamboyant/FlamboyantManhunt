# Role Factory Registry Pattern - Design Specification

**Date:** 2026-06-23  
**Priority:** 7 - Factory Violates Open/Closed Principle  
**Status:** Design Complete, Ready for Implementation  
**Effort Estimate:** 3-4 hours

---

## Overview

Refactor the current switch-statement-based `ManhuntRoleFactory` into an extensible registry pattern that follows the Open/Closed Principle. This change eliminates the need to modify core factory code when adding new roles, reduces merge conflicts, and aligns with DDD factory patterns.

---

## Problem Statement

### Current Implementation Issues

The existing `ManhuntRoleFactory` uses a static switch statement with 18+ cases:

```java
public class ManhuntRoleFactory {
    public static AManhuntRole createRole(Player owner, ManhuntRoleIdentifier roleIdentifier) {
        switch (roleIdentifier) {
            case SPEEDRUNNER_SIMPLE: return new SpeedrunnerRole(owner);
            case SPEEDRUNNER_SWAPPER: return new SpeedrunnerSwapperRole(owner);
            // ... 16 more cases
            default: return new HunterRole(owner);
        }
    }
}
```

**Problems:**
- Violates Open/Closed Principle - must modify factory to add roles
- Risk of merge conflicts when multiple developers add roles
- Tight coupling to all role implementations
- Not extensible for plugin-based roles
- Static design doesn't fit existing Guice DI architecture

---

## Solution: Registry Pattern

### Architecture Overview

**Core Components:**

1. **RoleFactory Interface** - Functional interface representing role construction (`Player → AManhuntRole`)
2. **RoleRegistry Class** - Singleton registry mapping `ManhuntRoleIdentifier` to `RoleFactory`
3. **Registration in ManhuntModule** - All roles registered via Guice provider using method references

**Migration Path:**
- Static `ManhuntRoleFactory.createRole()` → Instance `RoleRegistry.createRole()`
- Switch statement → Map-based lookup
- Static class → Guice-managed singleton
- One-time update in `RoleAssignmentService` (already uses constructor injection)

**Package Location:**
- Both new components in `domain/role/definition/` package
- Keeps role creation logic in Role Definition bounded context

---

## Component Design

### 1. RoleFactory Interface

**Purpose:** Functional interface for role construction

**Design:**
```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface RoleFactory {
    /**
     * Creates a role instance for the given player.
     *
     * @param owner The player who will own this role
     * @return A new role instance
     */
    AManhuntRole create(Player owner);
}
```

**Rationale:**
- Functional interface enables method references (`SpeedrunnerRole::new`)
- Single responsibility: create role given player
- No checked exceptions (role constructors don't throw in current codebase)
- Simple, focused contract

---

### 2. RoleRegistry Class

**Purpose:** Central registry for role factories

**Responsibilities:**
- Store role factory mappings
- Create role instances via delegation
- Validate role identifiers are registered
- Thread-safe after registration phase

**Design:**
```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RoleRegistry {
    private final Map<ManhuntRoleIdentifier, RoleFactory> factories;

    public RoleRegistry() {
        this.factories = new HashMap<>();
    }

    /**
     * Registers a role factory for the given identifier.
     *
     * @param identifier The role identifier
     * @param factory The factory to create instances of this role
     * @throws IllegalStateException if role already registered
     */
    public void register(ManhuntRoleIdentifier identifier, RoleFactory factory) {
        if (factories.containsKey(identifier)) {
            throw new IllegalStateException(
                "Role already registered: " + identifier
            );
        }
        factories.put(identifier, factory);
    }

    /**
     * Creates a role instance for the given identifier and owner.
     *
     * @param identifier The role identifier
     * @param owner The player who will own the role
     * @return A new role instance
     * @throws IllegalArgumentException if role not registered
     */
    public AManhuntRole createRole(ManhuntRoleIdentifier identifier, Player owner) {
        RoleFactory factory = factories.get(identifier);
        if (factory == null) {
            throw new IllegalArgumentException(
                "No factory registered for role: " + identifier
            );
        }
        return factory.create(owner);
    }

    /**
     * Checks if a role factory is registered.
     *
     * @param identifier The role identifier
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(ManhuntRoleIdentifier identifier) {
        return factories.containsKey(identifier);
    }
}
```

**Key Design Decisions:**

**Validation:**
- Duplicate registration throws `IllegalStateException` (programming error at startup)
- Unregistered role creation throws `IllegalArgumentException` (configuration error)
- Clear error messages include role identifier name for debugging

**Thread Safety:**
- Immutable after registration phase (all registration in ManhuntModule)
- No synchronization needed (single-threaded registration, read-only after startup)
- HashMap sufficient (no concurrent modification)

**Extensibility:**
- Public `register()` method allows future plugin extensions
- No hardcoded role list in registry itself
- Registry is a pure mechanism, not policy

---

## Integration

### ManhuntModule Changes

**Remove existing binding:**
```java
// DELETE THIS
bind(ManhuntRoleFactory.class);
```

**Add provider method:**
```java
@Provides
@Singleton
public RoleRegistry provideRoleRegistry() {
    RoleRegistry registry = new RoleRegistry();
    
    // Speedrunner roles
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_SWAPPER, SpeedrunnerSwapperRole::new);
    registry.register(SPEEDRUNNER_LINK, LinkSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_CHECKPOINT, CheckpointSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_ELF, ElfSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_WEREWOLF, WerewolfSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_CUTCLEAN, CutCleanSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_TNT_TACTICAL, TntTacticalSpeedrunnerRole::new);
    
    // Hunter roles
    registry.register(HUNTER_SIMPLE, HunterRole::new);
    registry.register(HUNTER_CHECKPOINT, CheckpointHunterRole::new);
    registry.register(HUNTER_CUTCLEAN, CutCleanHunterRole::new);
    registry.register(HUNTER_LINK, LinkHunterRole::new);
    registry.register(HUNTER_PRO_MINER, ProMinerRole::new);
    registry.register(HUNTER_ELF, ElfHunterRole::new);
    registry.register(SUPER_HUNTER, SuperHunterRole::new);
    
    // Special roles
    registry.register(ALLY_IMPOSTER, ImposterRole::new);
    registry.register(NEUTRAL_GLUER, GluerRole::new);
    registry.register(NEUTRAL_UNDECIDED, UndecidedRole::new);
    
    return registry;
}
```

**Why this approach:**
- Explicit registration keeps all role mappings visible in one place
- Method references are concise and type-safe
- Grouped by role type for clarity
- Easy to add new roles (one line, no switch modification)
- Guice singleton scope ensures single registry instance

---

### RoleAssignmentService Changes

**Update constructor injection:**
```java
// BEFORE
private final ManhuntRoleFactory roleFactory;

@Inject
public RoleAssignmentService(GameSessionManager sessionManager,
                              ManhuntRoleFactory roleFactory,
                              DomainEventPublisher eventPublisher) {
    this.sessionManager = sessionManager;
    this.roleFactory = roleFactory;
    this.eventPublisher = eventPublisher;
}

// AFTER
private final RoleRegistry roleRegistry;

@Inject
public RoleAssignmentService(GameSessionManager sessionManager,
                              RoleRegistry roleRegistry,
                              DomainEventPublisher eventPublisher) {
    this.sessionManager = sessionManager;
    this.roleRegistry = roleRegistry;
    this.eventPublisher = eventPublisher;
}
```

**Update role creation call:**
```java
// BEFORE
AManhuntRole role = roleFactory.createRole(entry.getKey(), entry.getValue());

// AFTER  
AManhuntRole role = roleRegistry.createRole(entry.getValue(), entry.getKey());
```

**Note:** Parameter order changes (`identifier, owner` instead of `owner, identifier`) to match registry API.

---

### File Deletions

**Delete:** `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java`

**Rationale:**
- Entirely replaced by RoleRegistry
- No adapter layer needed
- Clean migration (static class has no state to preserve)

---

## Testing Strategy

### Unit Tests: RoleRegistryTest

**Test Coverage:**

1. **Successful registration and creation**
   - Register a role factory
   - Create instance with mock player
   - Verify correct role type returned

2. **Duplicate registration prevention**
   - Register same identifier twice
   - Verify `IllegalStateException` thrown
   - Verify error message includes identifier name

3. **Unregistered role handling**
   - Attempt to create role without registration
   - Verify `IllegalArgumentException` thrown
   - Verify error message includes identifier name

4. **isRegistered() correctness**
   - Before registration returns false
   - After registration returns true
   - For different identifier returns false

5. **All role identifiers registrable**
   - Register all 18 roles in one registry
   - Verify no conflicts
   - Verify all can be created

**Test Structure Example:**
```java
@Test
public void createRole_withRegisteredRole_createsCorrectInstance() {
    // Arrange
    RoleRegistry registry = new RoleRegistry();
    Player mockPlayer = mock(Player.class);
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
    
    // Act
    AManhuntRole role = registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);
    
    // Assert
    assertNotNull(role);
    assertThat(role, instanceOf(SpeedrunnerRole.class));
}

@Test
public void register_duplicateIdentifier_throwsIllegalStateException() {
    // Arrange
    RoleRegistry registry = new RoleRegistry();
    registry.register(HUNTER_SIMPLE, HunterRole::new);
    
    // Act & Assert
    assertThrows(IllegalStateException.class, () -> {
        registry.register(HUNTER_SIMPLE, HunterRole::new);
    });
}

@Test
public void createRole_unregisteredIdentifier_throwsIllegalArgumentException() {
    // Arrange
    RoleRegistry registry = new RoleRegistry();
    Player mockPlayer = mock(Player.class);
    
    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer)
    );
    assertTrue(exception.getMessage().contains("SPEEDRUNNER_SIMPLE"));
}
```

---

### Integration Tests: RoleAssignmentServiceTest

**Updates to existing tests:**

1. **Update test setup**
   - Replace mock `ManhuntRoleFactory` with `RoleRegistry`
   - Register roles needed for each test
   - No behavioral changes expected

2. **Verify all role types assignable**
   - Test that service can assign all 18+ role types
   - Ensures registry is properly integrated

**Example:**
```java
@Before
public void setUp() {
    sessionManager = mock(GameSessionManager.class);
    eventPublisher = mock(DomainEventPublisher.class);
    
    // NEW: Create and populate registry
    roleRegistry = new RoleRegistry();
    roleRegistry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
    roleRegistry.register(HUNTER_SIMPLE, HunterRole::new);
    // ... register roles needed for tests
    
    // Updated constructor
    service = new RoleAssignmentService(sessionManager, roleRegistry, eventPublisher);
}
```

---

### No Behavioral Changes

**Registry pattern is pure refactoring:**
- Same inputs → same role instances
- Same exceptions for invalid scenarios
- Same integration points
- Existing role behavior tests unchanged
- Only factory creation mechanism changes

---

## Migration Checklist

**Phase 1: Create new components**
- [ ] Create `RoleFactory` interface
- [ ] Create `RoleRegistry` class
- [ ] Write `RoleRegistryTest` unit tests

**Phase 2: Update dependency injection**
- [ ] Add `provideRoleRegistry()` to `ManhuntModule`
- [ ] Remove `bind(ManhuntRoleFactory.class)`

**Phase 3: Update consumers**
- [ ] Update `RoleAssignmentService` constructor
- [ ] Update `RoleAssignmentService` role creation call
- [ ] Update `RoleAssignmentServiceTest` setup

**Phase 4: Cleanup**
- [ ] Delete `ManhuntRoleFactory.java`
- [ ] Run all tests to verify no regressions
- [ ] Update `REFACTORING_PROGRESS.md`

---

## Benefits

### Open/Closed Principle Achieved
- **Open for extension:** Add new roles by registering in ManhuntModule
- **Closed for modification:** RoleRegistry code never changes
- New role = one new line in provider method

### Reduced Coupling
- Registry doesn't import any role implementations
- Role implementations don't know about factory
- Clean separation of concerns

### Better Testability
- Mock registry for testing consumers
- Test registry independently with mock roles
- No static dependencies

### Future Extensibility
- Plugin-based roles can register at runtime
- Custom role packs can extend registry
- A/B test different role implementations

### Consistency with Architecture
- Follows existing Guice DI patterns
- Matches WinConditionEvaluator approach (collection of implementations)
- Aligns with DDD factory patterns

---

## Risks & Mitigations

### Risk: Forgot to register a role
**Mitigation:** 
- Unit test verifies all 18 identifiers can be registered
- Clear exception message at runtime
- Fail fast at role assignment time

### Risk: Wrong method reference
**Mitigation:**
- Compile-time type checking (method references are type-safe)
- Unit tests create instances of each role type

### Risk: Registry mutation after startup
**Mitigation:**
- Document that registration is startup-only
- Consider making registry immutable after first createRole() call (future enhancement)

---

## Future Enhancements

**Not in scope for Priority 7, but enabled by this design:**

1. **Immutable registry** - Lock registry after first use
2. **Plugin-based roles** - External plugins register roles via API
3. **Role metadata** - Store display name, description with factory
4. **Lazy initialization** - Create roles on-demand instead of upfront
5. **Role validation** - Verify all enum identifiers registered at startup

---

## Acceptance Criteria

- [x] Design approved
- [ ] `RoleFactory` interface created
- [ ] `RoleRegistry` class created with registration and creation methods
- [ ] All 18+ roles registered in `ManhuntModule.provideRoleRegistry()`
- [ ] `RoleAssignmentService` uses `RoleRegistry` instead of `ManhuntRoleFactory`
- [ ] `ManhuntRoleFactory.java` deleted
- [ ] `RoleRegistryTest` written with 5+ test cases
- [ ] `RoleAssignmentServiceTest` updated and passing
- [ ] All existing tests pass (no behavioral changes)
- [ ] No static factory code remains
- [ ] Documentation updated in `REFACTORING_PROGRESS.md`

---

## References

- **Priority Summary:** `docs/PROBLEMS_PRIORITY_SUMMARY.md` lines 318-367
- **Bounded Contexts:** `docs/architecture/bounded-contexts-map.md` (Role Definition context)
- **Guice Documentation:** https://github.com/google/guice/wiki
- **DDD Factories:** Vernon, "Implementing Domain-Driven Design", Chapter 11
