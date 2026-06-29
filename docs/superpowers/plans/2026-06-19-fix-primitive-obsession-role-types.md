# Fix Primitive Obsession (Role Types) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace string-based role type detection with type-safe enum relationships and centralized registry.

**Architecture:** Enhance `ManhuntRoleIdentifier` enum with explicit type associations via constructor parameters. Create `RoleTypeRegistry` utility class for pre-computed, immutable type-based lookups. Migrate all string-based filtering to use type-safe methods.

**Tech Stack:** Java 17+, JUnit 5, Bukkit API

## Global Constraints

- Java 17+ features allowed (`.toList()`, `Set.of()`, etc.)
- Follow existing package structure: `domain.role.definition` for role types
- TDD discipline: Red-Green-Refactor for all changes
- Maintain backward compatibility until migration complete
- All existing 87+ tests must continue passing
- Commit after each passing test

---

## Task 1: Enhance ManhuntRoleIdentifier Enum

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java` (full enum)
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifierTest.java` (new)

**Interfaces:**
- Consumes: `ManhuntRoleType` enum (existing)
- Produces: `ManhuntRoleType getRoleType()` method on all 22 enum values

---

- [ ] **Step 1.1: Write failing test for getRoleType() method**

Create `src/test/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifierTest.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManhuntRoleIdentifierTest {

    @Test
    void testGetRoleType_HunterSimple_ReturnsHunter() {
        assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_SIMPLE.getRoleType());
    }

    @Test
    void testGetRoleType_SpeedrunnerSimple_ReturnsSpeedrunner() {
        assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE.getRoleType());
    }

    @Test
    void testGetRoleType_AllyImposter_ReturnsAlly() {
        assertEquals(ManhuntRoleType.ALLY, ManhuntRoleIdentifier.ALLY_IMPOSTER.getRoleType());
    }

    @Test
    void testGetRoleType_NeutralGluer_ReturnsNeutral() {
        assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_GLUER.getRoleType());
    }
}
```

- [ ] **Step 1.2: Run tests to verify they fail**

Run: `./gradlew test --tests ManhuntRoleIdentifierTest`  
Expected: FAIL with "cannot find symbol: method getRoleType()"

- [ ] **Step 1.3: Add constructor and getRoleType() to ManhuntRoleIdentifier**

Modify `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

public enum ManhuntRoleIdentifier {
    // Bad design but f*ck : Names must start with corresponding ManhuntRoleType
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),
    HUNTER_CHECKPOINT(ManhuntRoleType.HUNTER),
    HUNTER_CUTCLEAN(ManhuntRoleType.HUNTER),
    HUNTER_LINK(ManhuntRoleType.HUNTER),
    HUNTER_PRO_MINER(ManhuntRoleType.HUNTER),
    HUNTER_ELF(ManhuntRoleType.HUNTER),
    SPEEDRUNNER_SIMPLE(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_CUTCLEAN(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_SWAPPER(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_LINK(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_CHECKPOINT(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_ELF(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_WEREWOLF(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_TNT_TACTICAL(ManhuntRoleType.SPEEDRUNNER),
    ALLY_IMPOSTER(ManhuntRoleType.ALLY),
    SUPER_HUNTER(ManhuntRoleType.HUNTER),
    NEUTRAL_GLUER(ManhuntRoleType.NEUTRAL),
    NEUTRAL_UNDECIDED(ManhuntRoleType.NEUTRAL);

    private final ManhuntRoleType roleType;

    ManhuntRoleIdentifier(ManhuntRoleType roleType) {
        this.roleType = roleType;
    }

    public ManhuntRoleType getRoleType() {
        return roleType;
    }
}
```

- [ ] **Step 1.4: Run tests to verify they pass**

Run: `./gradlew test --tests ManhuntRoleIdentifierTest`  
Expected: 4 tests PASS

- [ ] **Step 1.5: Add comprehensive test for all identifiers**

Add to `ManhuntRoleIdentifierTest.java`:

```java
@Test
void testGetRoleType_AllIdentifiersReturnCorrectType() {
    // Hunters (7 total)
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_SIMPLE.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_CHECKPOINT.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_CUTCLEAN.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_LINK.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_PRO_MINER.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.HUNTER_ELF.getRoleType());
    assertEquals(ManhuntRoleType.HUNTER, ManhuntRoleIdentifier.SUPER_HUNTER.getRoleType());

    // Speedrunners (8 total)
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_LINK.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_ELF.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_WEREWOLF.getRoleType());
    assertEquals(ManhuntRoleType.SPEEDRUNNER, ManhuntRoleIdentifier.SPEEDRUNNER_TNT_TACTICAL.getRoleType());

    // Allies (1 total)
    assertEquals(ManhuntRoleType.ALLY, ManhuntRoleIdentifier.ALLY_IMPOSTER.getRoleType());

    // Neutrals (2 total)
    assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_GLUER.getRoleType());
    assertEquals(ManhuntRoleType.NEUTRAL, ManhuntRoleIdentifier.NEUTRAL_UNDECIDED.getRoleType());
}

@Test
void testGetRoleType_NoNullTypes() {
    for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
        assertNotNull(identifier.getRoleType(), 
            "Role identifier " + identifier + " has null type");
    }
}

@Test
void testRoleTypeConsistency_NamingMatchesType() {
    for (ManhuntRoleIdentifier identifier : ManhuntRoleIdentifier.values()) {
        String name = identifier.name();
        ManhuntRoleType type = identifier.getRoleType();
        
        // Verify naming convention matches type
        if (name.startsWith("HUNTER") || name.equals("SUPER_HUNTER")) {
            assertEquals(ManhuntRoleType.HUNTER, type, 
                name + " should be HUNTER type");
        } else if (name.startsWith("SPEEDRUNNER")) {
            assertEquals(ManhuntRoleType.SPEEDRUNNER, type, 
                name + " should be SPEEDRUNNER type");
        } else if (name.startsWith("ALLY")) {
            assertEquals(ManhuntRoleType.ALLY, type, 
                name + " should be ALLY type");
        } else if (name.startsWith("NEUTRAL")) {
            assertEquals(ManhuntRoleType.NEUTRAL, type, 
                name + " should be NEUTRAL type");
        }
    }
}
```

- [ ] **Step 1.6: Run tests to verify they pass**

Run: `./gradlew test --tests ManhuntRoleIdentifierTest`  
Expected: 7 tests PASS

- [ ] **Step 1.7: Run full test suite to ensure backward compatibility**

Run: `./gradlew test`  
Expected: All 87+ tests PASS

- [ ] **Step 1.8: Commit enum enhancement**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java
git add src/test/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifierTest.java
git commit -m "feat(domain): add explicit type association to ManhuntRoleIdentifier

- Add ManhuntRoleType constructor parameter to all 22 enum values
- Add getRoleType() method for type-safe access
- Add comprehensive unit tests (7 tests)
- Maintains backward compatibility (all existing tests pass)

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 2: Create RoleTypeRegistry

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistry.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistryTest.java`

**Interfaces:**
- Consumes: `ManhuntRoleIdentifier.getRoleType()` (from Task 1)
- Produces: 
  - `static List<ManhuntRoleIdentifier> getRolesByType(ManhuntRoleType type)`
  - `static List<ManhuntRoleIdentifier> getRolesByTypeExcluding(ManhuntRoleType type, ManhuntRoleIdentifier... exclusions)`

---

- [ ] **Step 2.1: Write failing test for getRolesByType()**

Create `src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistryTest.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleTypeRegistryTest {

    @Test
    void testGetRolesByType_ReturnsAllHunters() {
        List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);
        
        assertEquals(7, hunters.size(), "Should have 7 hunter variants");
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_SIMPLE));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CHECKPOINT));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CUTCLEAN));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_LINK));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_PRO_MINER));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_ELF));
        assertTrue(hunters.contains(ManhuntRoleIdentifier.SUPER_HUNTER));
    }

    @Test
    void testGetRolesByType_ReturnsAllSpeedrunners() {
        List<ManhuntRoleIdentifier> speedrunners = RoleTypeRegistry.getRolesByType(ManhuntRoleType.SPEEDRUNNER);
        
        assertEquals(8, speedrunners.size(), "Should have 8 speedrunner variants");
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_LINK));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_ELF));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_WEREWOLF));
        assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_TNT_TACTICAL));
    }

    @Test
    void testGetRolesByType_ReturnsAllAllies() {
        List<ManhuntRoleIdentifier> allies = RoleTypeRegistry.getRolesByType(ManhuntRoleType.ALLY);
        
        assertEquals(1, allies.size(), "Should have 1 ally variant");
        assertTrue(allies.contains(ManhuntRoleIdentifier.ALLY_IMPOSTER));
    }

    @Test
    void testGetRolesByType_ReturnsAllNeutrals() {
        List<ManhuntRoleIdentifier> neutrals = RoleTypeRegistry.getRolesByType(ManhuntRoleType.NEUTRAL);
        
        assertEquals(2, neutrals.size(), "Should have 2 neutral variants");
        assertTrue(neutrals.contains(ManhuntRoleIdentifier.NEUTRAL_GLUER));
        assertTrue(neutrals.contains(ManhuntRoleIdentifier.NEUTRAL_UNDECIDED));
    }
}
```

- [ ] **Step 2.2: Run tests to verify they fail**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: FAIL with "cannot find symbol: class RoleTypeRegistry"

- [ ] **Step 2.3: Create RoleTypeRegistry with getRolesByType()**

Create `src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistry.java`:

```java
package me.flamboyant.manhunt.domain.role.definition;

import java.util.*;
import java.util.stream.Collectors;

public final class RoleTypeRegistry {
    private static final Map<ManhuntRoleType, List<ManhuntRoleIdentifier>> BY_TYPE;

    static {
        BY_TYPE = Arrays.stream(ManhuntRoleIdentifier.values())
            .collect(Collectors.groupingBy(
                ManhuntRoleIdentifier::getRoleType,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    Collections::unmodifiableList
                )
            ));
    }

    private RoleTypeRegistry() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Get all role identifiers of a specific type.
     *
     * @param type the role type to query
     * @return immutable list of matching role identifiers (empty if none)
     */
    public static List<ManhuntRoleIdentifier> getRolesByType(ManhuntRoleType type) {
        return BY_TYPE.getOrDefault(type, Collections.emptyList());
    }
}
```

- [ ] **Step 2.4: Run tests to verify they pass**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: 4 tests PASS

- [ ] **Step 2.5: Add test for immutability**

Add to `RoleTypeRegistryTest.java`:

```java
@Test
void testGetRolesByType_ReturnsImmutableList() {
    List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);
    
    assertThrows(UnsupportedOperationException.class, () -> {
        hunters.add(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
    }, "Returned list should be immutable");
}

@Test
void testGetRolesByType_ReturnsSameInstanceOnMultipleCalls() {
    List<ManhuntRoleIdentifier> hunters1 = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);
    List<ManhuntRoleIdentifier> hunters2 = RoleTypeRegistry.getRolesByType(ManhuntRoleType.HUNTER);
    
    assertSame(hunters1, hunters2, "Should return cached instance");
}
```

- [ ] **Step 2.6: Run tests to verify they pass**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: 6 tests PASS

- [ ] **Step 2.7: Add test for getRolesByTypeExcluding()**

Add to `RoleTypeRegistryTest.java`:

```java
@Test
void testGetRolesByTypeExcluding_SingleExclusion() {
    List<ManhuntRoleIdentifier> speedrunners = RoleTypeRegistry.getRolesByTypeExcluding(
        ManhuntRoleType.SPEEDRUNNER,
        ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
    );
    
    assertEquals(7, speedrunners.size(), "Should have 7 speedrunners (8 - 1 excluded)");
    assertFalse(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
    assertTrue(speedrunners.contains(ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN));
}

@Test
void testGetRolesByTypeExcluding_MultipleExclusions() {
    List<ManhuntRoleIdentifier> hunters = RoleTypeRegistry.getRolesByTypeExcluding(
        ManhuntRoleType.HUNTER,
        ManhuntRoleIdentifier.HUNTER_SIMPLE,
        ManhuntRoleIdentifier.HUNTER_CHECKPOINT
    );
    
    assertEquals(5, hunters.size(), "Should have 5 hunters (7 - 2 excluded)");
    assertFalse(hunters.contains(ManhuntRoleIdentifier.HUNTER_SIMPLE));
    assertFalse(hunters.contains(ManhuntRoleIdentifier.HUNTER_CHECKPOINT));
    assertTrue(hunters.contains(ManhuntRoleIdentifier.HUNTER_CUTCLEAN));
}

@Test
void testGetRolesByTypeExcluding_ExcludeAll() {
    List<ManhuntRoleIdentifier> allies = RoleTypeRegistry.getRolesByTypeExcluding(
        ManhuntRoleType.ALLY,
        ManhuntRoleIdentifier.ALLY_IMPOSTER
    );
    
    assertEquals(0, allies.size(), "Should have 0 allies when all excluded");
}
```

- [ ] **Step 2.8: Run tests to verify they fail**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: FAIL with "cannot find symbol: method getRolesByTypeExcluding"

- [ ] **Step 2.9: Implement getRolesByTypeExcluding()**

Add to `RoleTypeRegistry.java`:

```java
/**
 * Get all role identifiers of a specific type, excluding specified roles.
 *
 * @param type the role type to query
 * @param exclusions role identifiers to exclude from results
 * @return immutable list of matching role identifiers
 */
public static List<ManhuntRoleIdentifier> getRolesByTypeExcluding(
        ManhuntRoleType type,
        ManhuntRoleIdentifier... exclusions) {
    Set<ManhuntRoleIdentifier> excludeSet = Set.of(exclusions);
    return BY_TYPE.getOrDefault(type, Collections.emptyList())
        .stream()
        .filter(id -> !excludeSet.contains(id))
        .toList();
}
```

- [ ] **Step 2.10: Run tests to verify they pass**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: 9 tests PASS

- [ ] **Step 2.11: Add test for private constructor**

Add to `RoleTypeRegistryTest.java`:

```java
@Test
void testCannotInstantiate() {
    assertThrows(AssertionError.class, () -> {
        java.lang.reflect.Constructor<RoleTypeRegistry> constructor = 
            RoleTypeRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }, "Should not be able to instantiate utility class");
}
```

- [ ] **Step 2.12: Run tests to verify they pass**

Run: `./gradlew test --tests RoleTypeRegistryTest`  
Expected: 10 tests PASS

- [ ] **Step 2.13: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ tests PASS (no regressions)

- [ ] **Step 2.14: Commit RoleTypeRegistry**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistry.java
git add src/test/java/me/flamboyant/manhunt/domain/role/definition/RoleTypeRegistryTest.java
git commit -m "feat(domain): add RoleTypeRegistry for type-safe role filtering

- Create immutable registry with pre-computed type groupings
- Add getRolesByType() for type-based lookup
- Add getRolesByTypeExcluding() for filtered lookup
- Add comprehensive unit tests (10 tests)
- O(1) lookup performance vs O(n) string scanning

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 3: Migrate GameRolesManagement (Counting)

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java:49-52`

**Interfaces:**
- Consumes: `ManhuntRoleIdentifier.getRoleType()` (from Task 1)
- Produces: Type-safe role counting in setRandomRolesToEmpty()

---

- [ ] **Step 3.1: Verify existing tests pass**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (baseline)

- [ ] **Step 3.2: Replace string-based ally counting**

In `GameRolesManagement.java`, replace lines 49-50:

```java
// Before:
if (roleId.toString().contains("ALLY"))
    allyCount++;

// After:
if (roleId.getRoleType() == ManhuntRoleType.ALLY)
    allyCount++;
```

- [ ] **Step 3.3: Replace string-based speedrunner counting**

In `GameRolesManagement.java`, replace lines 51-52:

```java
// Before:
if (roleId.toString().contains("SPEEDRUNNER"))
    speedrunnerCount++;

// After:
if (roleId.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
    speedrunnerCount++;
```

- [ ] **Step 3.4: Run tests to verify behavior unchanged**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (same behavior)

- [ ] **Step 3.5: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ tests PASS

- [ ] **Step 3.6: Commit counting migration**

```bash
git add src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java
git commit -m "refactor(roles): replace string-based role counting with type-safe checks

- Replace .toString().contains() with .getRoleType() == for ALLY
- Replace .toString().contains() with .getRoleType() == for SPEEDRUNNER
- No behavior change (all tests pass)

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 4: Migrate GameRolesManagement (Filtering)

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java:83-87`

**Interfaces:**
- Consumes: `RoleTypeRegistry.getRolesByType()`, `RoleTypeRegistry.getRolesByTypeExcluding()` (from Task 2)
- Produces: Type-safe role list building in distributeRoles()

---

- [ ] **Step 4.1: Verify existing tests pass**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (baseline)

- [ ] **Step 4.2: Replace speedrunner list building**

In `GameRolesManagement.java`, replace line 83:

```java
// Before:
List<ManhuntRoleIdentifier> speedrunnerTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE)
    .collect(Collectors.toList());

// After:
List<ManhuntRoleIdentifier> speedrunnerTypes = RoleTypeRegistry.getRolesByTypeExcluding(
    ManhuntRoleType.SPEEDRUNNER,
    ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
);
```

- [ ] **Step 4.3: Replace ally list building**

In `GameRolesManagement.java`, replace line 84:

```java
// Before:
List<ManhuntRoleIdentifier> allyTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("ALLY"))
    .collect(Collectors.toList());

// After:
List<ManhuntRoleIdentifier> allyTypes = RoleTypeRegistry.getRolesByType(
    ManhuntRoleType.ALLY
);
```

- [ ] **Step 4.4: Replace hunter list building**

In `GameRolesManagement.java`, replace line 85:

```java
// Before:
List<ManhuntRoleIdentifier> hunterTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("HUNTER") && v != ManhuntRoleIdentifier.HUNTER_SIMPLE)
    .collect(Collectors.toList());

// After:
List<ManhuntRoleIdentifier> hunterTypes = RoleTypeRegistry.getRolesByTypeExcluding(
    ManhuntRoleType.HUNTER,
    ManhuntRoleIdentifier.HUNTER_SIMPLE
);
```

- [ ] **Step 4.5: Replace neutral list building**

In `GameRolesManagement.java`, replace line 86:

```java
// Before:
List<ManhuntRoleIdentifier> soloTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("NEUTRAL"))
    .collect(Collectors.toList());

// After:
List<ManhuntRoleIdentifier> soloTypes = RoleTypeRegistry.getRolesByType(
    ManhuntRoleType.NEUTRAL
);
```

- [ ] **Step 4.6: Remove unused imports**

Remove from `GameRolesManagement.java`:
- `import java.util.stream.Collectors;` (if no longer used)
- `import java.util.Arrays;` (if no longer used)

- [ ] **Step 4.7: Run tests to verify behavior unchanged**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (same behavior)

- [ ] **Step 4.8: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ tests PASS

- [ ] **Step 4.9: Commit filtering migration**

```bash
git add src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java
git commit -m "refactor(roles): replace string-based role filtering with registry

- Use RoleTypeRegistry.getRolesByTypeExcluding() for speedrunners/hunters
- Use RoleTypeRegistry.getRolesByType() for allies/neutrals
- Remove manual stream filtering and string matching
- Cleaner, more readable code (4 lines vs 12 lines)

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 5: Migrate GameRolesManagement (Hunter Counting)

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java:87`

**Interfaces:**
- Consumes: `ManhuntRoleIdentifier.getRoleType()` (from Task 1)
- Produces: Type-safe hunter counting in distributeRoles()

---

- [ ] **Step 5.1: Verify existing tests pass**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (baseline)

- [ ] **Step 5.2: Replace hunter counting stream**

In `GameRolesManagement.java`, replace line 87:

```java
// Before:
long distributedHunter = playersParameter.values().stream()
    .filter((r) -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("HUNTER"))
    .count();

// After:
long distributedHunter = playersParameter.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.HUNTER)
    .count();
```

- [ ] **Step 5.3: Run tests to verify behavior unchanged**

Run: `./gradlew test --tests GameRolesManagementTest`  
Expected: All tests PASS (same behavior)

- [ ] **Step 5.4: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ tests PASS

- [ ] **Step 5.5: Commit hunter counting migration**

```bash
git add src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java
git commit -m "refactor(roles): replace string-based hunter counting with type-safe check

- Replace .toString().contains(\"HUNTER\") with .getRoleType() == HUNTER
- Remove unnecessary lambda parentheses for consistency
- No behavior change (all tests pass)

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 6: Migrate NewManhuntLauncher

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java:189,193`

**Interfaces:**
- Consumes: `ManhuntRoleIdentifier.getRoleType()` (from Task 1)
- Produces: Type-safe role counting in getSpeedrunnerCount() and getAllyCount()

---

- [ ] **Step 6.1: Verify existing tests pass**

Run: `./gradlew test --tests NewManhuntLauncherTest`  
Expected: All tests PASS (baseline)

- [ ] **Step 6.2: Replace speedrunner counting**

In `NewManhuntLauncher.java`, replace line 189:

```java
// Before (getSpeedrunnerCount method):
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("SPEEDRUNNER"))
    .count();

// After:
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.SPEEDRUNNER)
    .count();
```

- [ ] **Step 6.3: Replace ally counting**

In `NewManhuntLauncher.java`, replace line 193:

```java
// Before (getAllyCount method):
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("ALLY"))
    .count();

// After:
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.ALLY)
    .count();
```

- [ ] **Step 6.4: Run tests to verify behavior unchanged**

Run: `./gradlew test --tests NewManhuntLauncherTest`  
Expected: All tests PASS (same behavior)

- [ ] **Step 6.5: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ tests PASS

- [ ] **Step 6.6: Commit launcher migration**

```bash
git add src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java
git commit -m "refactor(launcher): replace string-based role counting with type-safe checks

- Replace .toString().contains() with .getRoleType() == in getSpeedrunnerCount()
- Replace .toString().contains() with .getRoleType() == in getAllyCount()
- No behavior change (all tests pass)

Part of Priority 4: Fix Primitive Obsession"
```

---

## Task 7: Verification and Cleanup

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java:4` (remove comment)

**Interfaces:**
- Consumes: All previous migrations (Tasks 1-6)
- Produces: Clean codebase with no string-based role type detection

---

- [ ] **Step 7.1: Verify no string-based role type detection remains**

Run grep commands to find remaining instances:

```bash
grep -rn "\.toString()\.contains(\"SPEEDRUNNER\")" src/main/java/
grep -rn "\.toString()\.contains(\"HUNTER\")" src/main/java/
grep -rn "\.toString()\.contains(\"ALLY\")" src/main/java/
grep -rn "\.toString()\.contains(\"NEUTRAL\")" src/main/java/
```

Expected output: Only Link role sword checks (item types, not role types):
- `LinkHunterRole.java:50` - `.contains("SWORD")` (item type, not role type)
- `LinkHunterRole.java:60` - `.contains("SWORD")` (item type, not role type)
- `LinkSpeedrunnerRole.java:47` - `.contains("SWORD")` (item type, not role type)
- `LinkSpeedrunnerRole.java:58` - `.contains("SWORD")` (item type, not role type)

No role type string checks should remain.

- [ ] **Step 7.2: Remove "bad design" comment**

In `ManhuntRoleIdentifier.java`, remove line 4:

```java
// Before:
public enum ManhuntRoleIdentifier {
    // Bad design but f*ck : Names must start with corresponding ManhuntRoleType
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),

// After:
public enum ManhuntRoleIdentifier {
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),
```

- [ ] **Step 7.3: Run full test suite**

Run: `./gradlew test`  
Expected: All 87+ new unit tests + 17 new unit tests = 104+ total tests PASS

- [ ] **Step 7.4: Verify test coverage**

Check that all new tests exist:
- `ManhuntRoleIdentifierTest`: 7 tests
- `RoleTypeRegistryTest`: 10 tests
- Total new tests: 17

- [ ] **Step 7.5: Run integration smoke test**

Run the plugin in a test server and verify:
- Game launches successfully
- Roles are distributed correctly
- No runtime errors in logs

- [ ] **Step 7.6: Commit cleanup**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java
git commit -m "docs(domain): remove 'bad design' comment from ManhuntRoleIdentifier

String-based role type detection has been fully replaced with type-safe
enum relationships and registry lookups. Technical debt resolved.

Part of Priority 4: Fix Primitive Obsession - COMPLETE"
```

- [ ] **Step 7.7: Update REFACTORING_PROGRESS.md**

Mark Priority 4 as completed in `docs/REFACTORING_PROGRESS.md`:

```markdown
### Priority 4: Fix Primitive Obsession (String Types) ✅
**Status:** Completed
**Assigned To:** Claude Sonnet 4.5
**Started:** 2026-06-19
**Completed:** 2026-06-19
**Estimated Effort:** 2-3 hours
**Actual Effort:** [Record actual time]
```

Update all task checkboxes to [x] and add commit references.

- [ ] **Step 7.8: Commit progress update**

```bash
git add docs/REFACTORING_PROGRESS.md
git commit -m "docs: mark Priority 4 complete in refactoring progress tracker

All tasks completed:
- Enum enhancement with type associations
- RoleTypeRegistry creation
- All call sites migrated
- Technical debt comment removed
- 17 new unit tests added
- All 104+ tests passing

Priority 4: Fix Primitive Obsession - COMPLETE"
```

---

## Success Criteria Checklist

After completing all tasks, verify:

- [ ] All 22 role identifiers have explicit type associations
- [ ] `getRoleType()` method works correctly for all identifiers
- [ ] `RoleTypeRegistry` provides correct groupings
- [ ] All string-based role type detection removed from codebase (grep verification)
- [ ] All 87+ existing tests pass
- [ ] 17 new unit tests pass (7 + 10)
- [ ] "Bad design" comment removed
- [ ] Code is more readable and maintainable
- [ ] No performance regression
- [ ] REFACTORING_PROGRESS.md updated

---

## Notes

- **TDD Discipline:** Every change follows Red-Green-Refactor
- **Backward Compatibility:** Each task maintains existing test suite passing
- **Frequent Commits:** Commit after each passing test cycle
- **Verification:** Grep commands ensure no string-based checks remain
- **Integration Testing:** Manual smoke test before final commit

---

## Estimated Time

- Task 1: 30 minutes (enum enhancement + tests)
- Task 2: 45 minutes (registry creation + comprehensive tests)
- Task 3: 10 minutes (counting migration)
- Task 4: 15 minutes (filtering migration)
- Task 5: 10 minutes (hunter counting migration)
- Task 6: 10 minutes (launcher migration)
- Task 7: 20 minutes (verification + cleanup)

**Total: ~2.5 hours**
