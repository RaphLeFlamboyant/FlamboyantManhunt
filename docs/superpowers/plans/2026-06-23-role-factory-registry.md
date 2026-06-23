# Role Factory Registry Pattern - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the switch-statement-based `ManhuntRoleFactory` with an extensible registry pattern that follows the Open/Closed Principle.

**Architecture:** Create a `RoleRegistry` class that maps `ManhuntRoleIdentifier` to `RoleFactory` functional interfaces. Register all role constructors via method references in `ManhuntModule`. Update `RoleAssignmentService` to use the registry instead of the static factory.

**Tech Stack:** Java 8, Google Guice 5.1.0, JUnit 4, Mockito

## Global Constraints

- Java 8 compatibility required
- Follow existing DDD package structure (`domain/role/definition/`)
- Use existing test patterns (JUnit 4, Mockito for mocking)
- TDD discipline: write tests first, implement second
- Commit after each task completion
- No behavioral changes (pure refactoring)

---

## File Structure

### New Files
- `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleFactory.java` - Functional interface for role construction
- `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistry.java` - Registry mapping identifiers to factories
- `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistryTest.java` - Unit tests for registry

### Modified Files
- `src/main/java/me/flamboyant/manhunt/application/injection/ManhuntModule.java` - Add registry provider, remove factory binding
- `src/main/java/me/flamboyant/manhunt/application/services/RoleAssignmentService.java` - Update to use RoleRegistry
- `src/test/java/me/flamboyant/manhunt/application/services/RoleAssignmentServiceTest.java` - Update test setup

### Deleted Files
- `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java` - Replaced by RoleRegistry

---

## Task 1: Create RoleFactory Interface

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleFactory.java`

**Interfaces:**
- Consumes: Nothing (foundation interface)
- Produces: `RoleFactory` functional interface with `AManhuntRole create(Player owner)` method

**Steps:**

- [ ] **Step 1: Create RoleFactory interface file**

Create file `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleFactory.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

/**
 * Functional interface for creating role instances.
 * Enables method reference syntax for role constructors (e.g., SpeedrunnerRole::new).
 */
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

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -DskipTests`
Expected: BUILD SUCCESS with no compilation errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleFactory.java
git commit -m "feat(domain): add RoleFactory functional interface"
```

---

## Task 2: Create RoleRegistry with Tests (TDD)

**Files:**
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistryTest.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistry.java`

**Interfaces:**
- Consumes: `RoleFactory` interface from Task 1
- Produces: `RoleRegistry` class with:
  - `void register(ManhuntRoleIdentifier identifier, RoleFactory factory)` - throws `IllegalStateException` on duplicate
  - `AManhuntRole createRole(ManhuntRoleIdentifier identifier, Player owner)` - throws `IllegalArgumentException` if unregistered
  - `boolean isRegistered(ManhuntRoleIdentifier identifier)`

**Steps:**

- [ ] **Step 1: Write failing test for successful registration and creation**

Create file `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistryTest.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RoleRegistryTest {
    private RoleRegistry registry;
    private Player mockPlayer;

    @Before
    public void setUp() {
        registry = new RoleRegistry();
        mockPlayer = mock(Player.class);
    }

    @Test
    public void createRole_withRegisteredRole_createsCorrectInstance() {
        // Arrange
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

        // Act
        AManhuntRole role = registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);

        // Assert
        assertNotNull(role);
        assertTrue(role instanceof SpeedrunnerRole);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoleRegistryTest#createRole_withRegisteredRole_createsCorrectInstance`
Expected: FAIL with "cannot find symbol: class RoleRegistry"

- [ ] **Step 3: Create RoleRegistry class with minimal implementation**

Create file `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistry.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for role factories following the Registry pattern.
 * Maps role identifiers to factory functions for creating role instances.
 */
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
            throw new IllegalStateException("Role already registered: " + identifier);
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
            throw new IllegalArgumentException("No factory registered for role: " + identifier);
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

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoleRegistryTest#createRole_withRegisteredRole_createsCorrectInstance`
Expected: PASS

- [ ] **Step 5: Write failing test for duplicate registration**

Add to `RoleRegistryTest.java`:

```java
@Test(expected = IllegalStateException.class)
public void register_duplicateIdentifier_throwsIllegalStateException() {
    // Arrange
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

    // Act & Assert
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
}
```

- [ ] **Step 6: Run test to verify it passes (implementation already correct)**

Run: `mvn test -Dtest=RoleRegistryTest#register_duplicateIdentifier_throwsIllegalStateException`
Expected: PASS (implementation already throws)

- [ ] **Step 7: Write failing test for unregistered role**

Add to `RoleRegistryTest.java`:

```java
@Test
public void createRole_unregisteredIdentifier_throwsIllegalArgumentException() {
    // Act & Assert
    try {
        registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);
        fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
        assertTrue(e.getMessage().contains("SPEEDRUNNER_SIMPLE"));
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn test -Dtest=RoleRegistryTest#createRole_unregisteredIdentifier_throwsIllegalArgumentException`
Expected: PASS

- [ ] **Step 9: Write test for isRegistered()**

Add to `RoleRegistryTest.java`:

```java
@Test
public void isRegistered_beforeRegistration_returnsFalse() {
    // Act & Assert
    assertFalse(registry.isRegistered(SPEEDRUNNER_SIMPLE));
}

@Test
public void isRegistered_afterRegistration_returnsTrue() {
    // Arrange
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

    // Act & Assert
    assertTrue(registry.isRegistered(SPEEDRUNNER_SIMPLE));
}

@Test
public void isRegistered_differentIdentifier_returnsFalse() {
    // Arrange
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);

    // Act & Assert
    assertFalse(registry.isRegistered(ManhuntRoleIdentifier.HUNTER_SIMPLE));
}
```

- [ ] **Step 10: Run all tests to verify they pass**

Run: `mvn test -Dtest=RoleRegistryTest`
Expected: All tests PASS

- [ ] **Step 11: Write test for all role identifiers registrable**

Add to `RoleRegistryTest.java`:

```java
import me.flamboyant.manhunt.domain.role.behavior.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;

@Test
public void register_allRoleIdentifiers_noConflicts() {
    // Act - Register all 18 roles
    registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_SWAPPER, SpeedrunnerSwapperRole::new);
    registry.register(SPEEDRUNNER_LINK, LinkSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_CHECKPOINT, CheckpointSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_ELF, ElfSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_WEREWOLF, WerewolfSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_CUTCLEAN, CutCleanSpeedrunnerRole::new);
    registry.register(SPEEDRUNNER_TNT_TACTICAL, TntTacticalSpeedrunnerRole::new);
    registry.register(HUNTER_SIMPLE, HunterRole::new);
    registry.register(HUNTER_CHECKPOINT, CheckpointHunterRole::new);
    registry.register(HUNTER_CUTCLEAN, CutCleanHunterRole::new);
    registry.register(HUNTER_LINK, LinkHunterRole::new);
    registry.register(HUNTER_PRO_MINER, ProMinerRole::new);
    registry.register(HUNTER_ELF, ElfHunterRole::new);
    registry.register(SUPER_HUNTER, SuperHunterRole::new);
    registry.register(ALLY_IMPOSTER, ImposterRole::new);
    registry.register(NEUTRAL_GLUER, GluerRole::new);
    registry.register(NEUTRAL_UNDECIDED, UndecidedRole::new);

    // Assert - Verify all registered
    assertTrue(registry.isRegistered(SPEEDRUNNER_SIMPLE));
    assertTrue(registry.isRegistered(HUNTER_SIMPLE));
    assertTrue(registry.isRegistered(ALLY_IMPOSTER));
    assertTrue(registry.isRegistered(NEUTRAL_GLUER));

    // Verify can create instances
    AManhuntRole speedrunner = registry.createRole(SPEEDRUNNER_SIMPLE, mockPlayer);
    AManhuntRole hunter = registry.createRole(HUNTER_SIMPLE, mockPlayer);
    assertNotNull(speedrunner);
    assertNotNull(hunter);
}
```

- [ ] **Step 12: Run all tests to verify they pass**

Run: `mvn test -Dtest=RoleRegistryTest`
Expected: All 8 tests PASS

- [ ] **Step 13: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistry.java
git add src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleRegistryTest.java
git commit -m "feat(domain): add RoleRegistry with comprehensive tests"
```

---

## Task 3: Add RoleRegistry Provider to ManhuntModule

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/application/injection/ManhuntModule.java`

**Interfaces:**
- Consumes: `RoleRegistry` class from Task 2, all role implementation classes
- Produces: Guice provider method `RoleRegistry provideRoleRegistry()` with all 18 roles registered

**Steps:**

- [ ] **Step 1: Add import statements**

Add to imports section in `ManhuntModule.java`:

```java
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
import me.flamboyant.manhunt.domain.role.behavior.*;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
```

- [ ] **Step 2: Remove old factory binding**

In `ManhuntModule.configure()` method, remove this line:

```java
bind(ManhuntRoleFactory.class);
```

- [ ] **Step 3: Add RoleRegistry provider method**

Add this provider method to `ManhuntModule` class (after the existing provider methods):

```java
/**
 * Provides the RoleRegistry singleton with all role factories registered.
 * Each role is registered using method references for type-safe construction.
 */
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

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/application/injection/ManhuntModule.java
git commit -m "feat(application): add RoleRegistry provider to ManhuntModule"
```

---

## Task 4: Update RoleAssignmentService to Use RoleRegistry

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/application/services/RoleAssignmentService.java`
- Modify: `src/test/java/me/flamboyant/manhunt/application/services/RoleAssignmentServiceTest.java`

**Interfaces:**
- Consumes: `RoleRegistry` from Task 3
- Produces: Updated `RoleAssignmentService` using `RoleRegistry.createRole(identifier, owner)`

**Steps:**

- [ ] **Step 1: Update RoleAssignmentService imports**

In `RoleAssignmentService.java`, replace:

```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleFactory;
```

With:

```java
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
```

- [ ] **Step 2: Update RoleAssignmentService field and constructor**

In `RoleAssignmentService.java`, replace:

```java
private final ManhuntRoleFactory roleFactory;

@Inject
public RoleAssignmentService(GameSessionManager sessionManager,
                              ManhuntRoleFactory roleFactory,
                              DomainEventPublisher eventPublisher) {
    this.sessionManager = sessionManager;
    this.roleFactory = roleFactory;
    this.eventPublisher = eventPublisher;
}
```

With:

```java
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

- [ ] **Step 3: Update role creation call**

In `RoleAssignmentService.java` `assignRoles()` method, replace:

```java
AManhuntRole role = roleFactory.createRole(entry.getKey(), entry.getValue());
```

With:

```java
AManhuntRole role = roleRegistry.createRole(entry.getValue(), entry.getKey());
```

Note: Parameter order changes to `(identifier, owner)` to match registry API.

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Update RoleAssignmentServiceTest imports**

In `RoleAssignmentServiceTest.java`, replace:

```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleFactory;
```

With:

```java
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.behavior.HunterRole;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
```

- [ ] **Step 6: Update RoleAssignmentServiceTest setup**

In `RoleAssignmentServiceTest.java`, replace the field declaration and setUp() method:

Replace:
```java
private ManhuntRoleFactory roleFactory;
```

With:
```java
private RoleRegistry roleRegistry;
```

In the `setUp()` method, replace:

```java
roleFactory = mock(ManhuntRoleFactory.class);
```

With:

```java
roleRegistry = new RoleRegistry();
roleRegistry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
roleRegistry.register(HUNTER_SIMPLE, HunterRole::new);
```

And replace:

```java
service = new RoleAssignmentService(sessionManager, roleFactory, eventPublisher);
```

With:

```java
service = new RoleAssignmentService(sessionManager, roleRegistry, eventPublisher);
```

- [ ] **Step 7: Remove mock factory behavior from tests**

In `RoleAssignmentServiceTest.java`, find and remove any lines setting up mock behavior like:

```java
when(roleFactory.createRole(...)).thenReturn(...);
```

The real registry is used now, so no mocking needed.

- [ ] **Step 8: Run RoleAssignmentServiceTest**

Run: `mvn test -Dtest=RoleAssignmentServiceTest`
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/application/services/RoleAssignmentService.java
git add src/test/java/me/flamboyant/manhunt/application/services/RoleAssignmentServiceTest.java
git commit -m "refactor(application): update RoleAssignmentService to use RoleRegistry"
```

---

## Task 5: Delete ManhuntRoleFactory and Run All Tests

**Files:**
- Delete: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java`

**Interfaces:**
- Consumes: Nothing (cleanup task)
- Produces: Codebase with no references to `ManhuntRoleFactory`

**Steps:**

- [ ] **Step 1: Verify no references to ManhuntRoleFactory remain**

Run: `grep -r "ManhuntRoleFactory" src/main/java/ src/test/java/ --include="*.java"`
Expected: No matches (or only import statements we're about to clean up)

- [ ] **Step 2: Delete ManhuntRoleFactory.java**

```bash
git rm src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java
```

- [ ] **Step 3: Run all tests to verify no regressions**

Run: `mvn test`
Expected: All tests PASS (BUILD SUCCESS)

- [ ] **Step 4: Commit deletion**

```bash
git commit -m "refactor(domain): delete ManhuntRoleFactory (replaced by RoleRegistry)"
```

---

## Task 6: Update Documentation

**Files:**
- Modify: `docs/REFACTORING_PROGRESS.md`

**Interfaces:**
- Consumes: Nothing (documentation task)
- Produces: Updated progress tracker marking Priority 7 complete

**Steps:**

- [ ] **Step 1: Update Priority 7 status in REFACTORING_PROGRESS.md**

Find the "Priority 7: Refactor Factory (Open/Closed)" section (around line 572) and update:

Change:
```markdown
### Priority 7: Refactor Factory (Open/Closed) ⬜
**Status:** Not Started  
**Assigned To:** -  
**Started:** -  
**Completed:** -  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** -
```

To:
```markdown
### Priority 7: Refactor Factory (Open/Closed) ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-23  
**Completed:** 2026-06-23  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** ~3 hours
```

- [ ] **Step 2: Check off all task items**

In the same section, change all `- [ ]` to `- [x]` for tasks 7.1 through 7.7:

```markdown
#### Tasks
- [x] 7.1 Create RoleFactory interface
- [x] 7.2 Create RoleRegistry
- [x] 7.3 Register all roles in ManhuntModule
- [x] 7.4 Replace ManhuntRoleFactory with RoleRegistry
- [x] 7.5 (Optional) Add annotation-based registration - SKIPPED (manual registration chosen)
- [x] 7.6 Remove old ManhuntRoleFactory
- [x] 7.7 Write tests
```

- [ ] **Step 3: Add notes section**

In the Notes section, replace:
```markdown
#### Notes
- **Blockers:** None (independent change)
- **Decisions Made:** -
- **Questions:** -
- **Commits:** -
```

With:
```markdown
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
```

- [ ] **Step 4: Update success criteria**

Change all criteria to checked:
```markdown
#### Success Criteria
- ✅ No switch statement in factory - ACHIEVED (replaced with Map lookup)
- ✅ Can register roles dynamically - ACHIEVED (public register() method)
- ✅ Adding role doesn't require modifying factory - ACHIEVED (one line in provider)
- ✅ Registry is extensible - ACHIEVED (public API allows plugin registration)
- ✅ Tests cover registration and creation - ACHIEVED (8 unit tests)
```

- [ ] **Step 5: Update Phase 3 progress summary**

Find the "Phase Completion" section (around line 976) and update:

Change:
```markdown
- [ ] Phase 3: Tactical Patterns (0/3 complete)
```

To:
```markdown
- [ ] Phase 3: Tactical Patterns (1/3 complete) - **Priority 7 DONE** ✅
```

And update the total:
```markdown
### Total Progress: 7/13 priorities completed (54%)
```

- [ ] **Step 6: Commit documentation updates**

```bash
git add docs/REFACTORING_PROGRESS.md
git commit -m "docs: mark Priority 7 (Factory Refactoring) as complete"
```

---

## Verification Checklist

After completing all tasks, verify:

- [ ] All tests pass: `mvn test`
- [ ] No compilation errors: `mvn compile`
- [ ] No references to `ManhuntRoleFactory` remain: `grep -r "ManhuntRoleFactory" src/ --include="*.java"`
- [ ] RoleRegistry has 8 passing unit tests
- [ ] RoleAssignmentService tests still pass
- [ ] All 18 role identifiers registered in ManhuntModule
- [ ] Git history shows 6 commits for the 6 tasks
- [ ] Documentation updated in REFACTORING_PROGRESS.md

---

## Success Criteria

- [x] Design approved
- [ ] `RoleFactory` interface created
- [ ] `RoleRegistry` class created with registration and creation methods
- [ ] All 18+ roles registered in `ManhuntModule.provideRoleRegistry()`
- [ ] `RoleAssignmentService` uses `RoleRegistry` instead of `ManhuntRoleFactory`
- [ ] `ManhuntRoleFactory.java` deleted
- [ ] `RoleRegistryTest` written with 8 test cases
- [ ] `RoleAssignmentServiceTest` updated and passing
- [ ] All existing tests pass (no behavioral changes)
- [ ] No static factory code remains
- [ ] Documentation updated in `REFACTORING_PROGRESS.md`
