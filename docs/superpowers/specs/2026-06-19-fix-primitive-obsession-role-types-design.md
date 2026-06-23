# Design: Fix Primitive Obsession (String-Based Role Types)

**Date:** 2026-06-19  
**Status:** Approved  
**Priority:** 4 (Phase 1: Core Domain)  
**Estimated Effort:** 2-3 hours

---

## Problem Statement

The current codebase uses string-based role type detection throughout the domain layer. Role identifiers are filtered using `.toString().contains("SPEEDRUNNER")`, `.toString().contains("HUNTER")`, etc. This approach has several problems:

1. **Primitive obsession** - Using strings instead of type-safe enum relationships
2. **Fragile** - Relies on naming conventions that could break with refactoring
3. **No compile-time safety** - Typos or incorrect strings won't be caught until runtime
4. **Poor IDE support** - No autocomplete or refactoring support for string matching
5. **Acknowledged technical debt** - Comment in code: "Bad design but f*ck: Names must start with corresponding ManhuntRoleType"

**Example of current approach:**
```java
// GameRolesManagement.java, line 83
List<ManhuntRoleIdentifier> speedrunnerTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE)
    .collect(Collectors.toList());
```

---

## Goals

1. Replace string-based role type detection with type-safe enum relationships
2. Add `getRoleType()` method to `ManhuntRoleIdentifier` enum
3. Create centralized registry for type-based filtering
4. Maintain backward compatibility during migration
5. Remove "bad design" comment once migration is complete

---

## Architecture Overview

### Components

1. **ManhuntRoleIdentifier** (enhanced) - Add explicit type association via constructor
2. **RoleTypeRegistry** (new) - Pre-computed, immutable registry for type-based lookups

### Package Placement

Both components live in `domain.role.definition` package alongside existing `ManhuntRoleType`.

### Migration Strategy

1. Add new functionality first (backward compatible)
2. Migrate call sites incrementally
3. Remove string-based detection
4. Remove technical debt comment

---

## Detailed Design

### 1. ManhuntRoleIdentifier Enhancement

**Current state:**
```java
public enum ManhuntRoleIdentifier {
    // Bad design but f*ck : Names must start with corresponding ManhuntRoleType
    HUNTER_SIMPLE,
    HUNTER_CHECKPOINT,
    // ... 22 total values
}
```

**New design:**
```java
public enum ManhuntRoleIdentifier {
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

**Design decisions:**
- Explicit type association for each enum value
- Private final field ensures immutability
- Simple getter provides type-safe access
- Naming convention still maintained for readability

---

### 2. RoleTypeRegistry Class

**Purpose:** Centralized, pre-computed registry for efficient type-based lookups.

**Location:** `domain.role.definition.RoleTypeRegistry`

**Implementation:**
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
}
```

**Design decisions:**
- **Static initialization** - Registry computed once at class load time
- **Immutable collections** - Returns unmodifiable lists for safety
- **Private constructor** - Pure utility class, no instances allowed
- **Empty list fallback** - Safe handling of unknown types
- **Exclusion helper** - Common pattern in GameRolesManagement (e.g., "all speedrunners except SPEEDRUNNER_SIMPLE")
- **Performance** - O(1) lookup by type, pre-computed grouping

---

## Migration Plan

### Phase 1: Add New Functionality (Backward Compatible)

1. Add type association to `ManhuntRoleIdentifier` enum
2. Create `RoleTypeRegistry` class
3. Write unit tests for both components
4. Verify all existing tests still pass

**No breaking changes** - String-based detection continues to work during migration.

### Phase 2: Migrate Call Sites

Three main locations use string-based type detection:

#### Location 1: GameRolesManagement.java

**Lines 49-52 (Counting roles):**

Before:
```java
if (roleId.toString().contains("ALLY"))
    allyCount++;
if (roleId.toString().contains("SPEEDRUNNER"))
    speedrunnerCount++;
```

After:
```java
if (roleId.getRoleType() == ManhuntRoleType.ALLY)
    allyCount++;
if (roleId.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
    speedrunnerCount++;
```

**Lines 83-86 (Building filtered lists):**

Before:
```java
List<ManhuntRoleIdentifier> speedrunnerTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE)
    .collect(Collectors.toList());
List<ManhuntRoleIdentifier> allyTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("ALLY"))
    .collect(Collectors.toList());
List<ManhuntRoleIdentifier> hunterTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("HUNTER") && v != ManhuntRoleIdentifier.HUNTER_SIMPLE)
    .collect(Collectors.toList());
List<ManhuntRoleIdentifier> soloTypes = Arrays.stream(ManhuntRoleIdentifier.values())
    .filter(v -> v.toString().contains("NEUTRAL"))
    .collect(Collectors.toList());
```

After:
```java
List<ManhuntRoleIdentifier> speedrunnerTypes = RoleTypeRegistry.getRolesByTypeExcluding(
    ManhuntRoleType.SPEEDRUNNER, 
    ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE
);
List<ManhuntRoleIdentifier> allyTypes = RoleTypeRegistry.getRolesByType(
    ManhuntRoleType.ALLY
);
List<ManhuntRoleIdentifier> hunterTypes = RoleTypeRegistry.getRolesByTypeExcluding(
    ManhuntRoleType.HUNTER, 
    ManhuntRoleIdentifier.HUNTER_SIMPLE
);
List<ManhuntRoleIdentifier> soloTypes = RoleTypeRegistry.getRolesByType(
    ManhuntRoleType.NEUTRAL
);
```

**Line 87 (Counting hunters):**

Before:
```java
long distributedHunter = playersParameter.values().stream()
    .filter((r) -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("HUNTER"))
    .count();
```

After:
```java
long distributedHunter = playersParameter.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.HUNTER)
    .count();
```

#### Location 2: NewManhuntLauncher.java

**Line 189 (getSpeedrunnerCount):**

Before:
```java
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("SPEEDRUNNER"))
    .count();
```

After:
```java
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.SPEEDRUNNER)
    .count();
```

**Line 193 (getAllyCount):**

Before:
```java
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("ALLY"))
    .count();
```

After:
```java
return (int) playerRoles.values().stream()
    .filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.ALLY)
    .count();
```

### Phase 3: Cleanup

1. Verify no string-based type detection remains:
   ```bash
   grep -r "\.toString()\.contains(\"SPEEDRUNNER\")" src/
   grep -r "\.toString()\.contains(\"HUNTER\")" src/
   grep -r "\.toString()\.contains(\"ALLY\")" src/
   grep -r "\.toString()\.contains(\"NEUTRAL\")" src/
   ```
   (Excluding Link role sword checks which are for item types, not role types)

2. Remove "bad design" comment from ManhuntRoleIdentifier (line 4)

3. Run full test suite (87+ tests must pass)

---

## Testing Strategy

### Unit Tests: ManhuntRoleIdentifier

**Test file:** `ManhuntRoleIdentifierTest.java`

**Test cases:**
1. `testGetRoleType_AllIdentifiersReturnCorrectType()` - Verify each identifier returns correct type
2. `testGetRoleType_NoNullTypes()` - Ensure all identifiers have non-null types
3. `testRoleTypeConsistency_NamingMatchesType()` - Verify naming convention matches type (e.g., HUNTER_* returns HUNTER)
4. `testGetRoleType_HunterVariants()` - Specifically test all 6 hunter variants
5. `testGetRoleType_SpeedrunnerVariants()` - Specifically test all 8 speedrunner variants
6. `testGetRoleType_AllyVariants()` - Test ALLY_IMPOSTER returns ALLY
7. `testGetRoleType_NeutralVariants()` - Test both neutral roles

### Unit Tests: RoleTypeRegistry

**Test file:** `RoleTypeRegistryTest.java`

**Test cases:**
1. `testGetRolesByType_ReturnsAllHunters()` - Verify 7 hunters returned (6 variants + SUPER_HUNTER)
2. `testGetRolesByType_ReturnsAllSpeedrunners()` - Verify 8 speedrunners returned
3. `testGetRolesByType_ReturnsAllAllies()` - Verify 1 ally returned
4. `testGetRolesByType_ReturnsAllNeutrals()` - Verify 2 neutrals returned
5. `testGetRolesByType_ReturnsImmutableList()` - Ensure returned list throws UnsupportedOperationException on modification
6. `testGetRolesByTypeExcluding_SingleExclusion()` - Test excluding SPEEDRUNNER_SIMPLE
7. `testGetRolesByTypeExcluding_MultipleExclusions()` - Test excluding multiple roles
8. `testGetRolesByTypeExcluding_ExcludeAll()` - Test excluding all roles of a type returns empty list
9. `testGetRolesByType_ReturnsSameInstanceOnMultipleCalls()` - Verify caching works (same list object)
10. `testCannotInstantiate()` - Verify private constructor prevents instantiation

### Integration Tests

**Update existing tests:**
- `GameRolesManagementTest` - Verify role distribution still works correctly
- `NewManhuntLauncherTest` - Verify counting methods return correct values

### Verification Checklist

After migration:
- [ ] All 87+ existing tests pass
- [ ] New unit tests pass (14+ new tests)
- [ ] No string-based role type detection remains (grep verification)
- [ ] "Bad design" comment removed
- [ ] Code is more readable and maintainable
- [ ] No performance regression (registry should be faster than string contains)

---

## Benefits

### Immediate Benefits

1. **Type safety** - Compile-time checking instead of runtime string matching
2. **Performance** - O(1) lookup vs string scanning
3. **Maintainability** - Clear type relationships, easier to understand
4. **IDE support** - Autocomplete, refactoring, find usages all work properly
5. **Removes technical debt** - Eliminates acknowledged "bad design"

### Long-term Benefits

1. **Extensibility** - Easy to add new role types or identifiers
2. **Testability** - Type relationships can be unit tested
3. **Documentation** - Enum constructor makes relationships explicit
4. **Refactoring safety** - IDE refactoring works correctly

---

## Non-Goals

This refactoring does NOT address:
- Role factory pattern (Priority 7)
- Role distribution strategy (Priority 10)
- Role behavior abstractions (Priority 13)

These are separate priorities and should be tackled independently.

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing functionality | Incremental migration with backward compatibility, full test suite runs |
| Incorrect type assignments | Unit tests verify all 22 identifiers, naming consistency check |
| Performance regression | Registry pre-computes at class load, faster than string operations |
| Missed call sites | Grep verification before removing old approach |

---

## Success Criteria

- [ ] All 22 role identifiers have explicit type associations
- [ ] `getRoleType()` method works correctly for all identifiers
- [ ] `RoleTypeRegistry` provides correct groupings
- [ ] All string-based type detection removed from codebase
- [ ] All 87+ existing tests pass
- [ ] 14+ new unit tests pass
- [ ] "Bad design" comment removed
- [ ] Code review approval

---

## References

- **REFACTORING_PROGRESS.md** - Priority 4 tasks and status
- **PROBLEMS_PRIORITY_SUMMARY.md** - Original problem analysis
- **ManhuntRoleIdentifier.java** - Current enum implementation
- **ManhuntRoleType.java** - Existing type enum
- **GameRolesManagement.java** - Primary consumer of type filtering
