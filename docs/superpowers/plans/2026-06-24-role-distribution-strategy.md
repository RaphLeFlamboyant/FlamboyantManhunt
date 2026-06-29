# Role Distribution Strategy Pattern Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the monolithic role distribution algorithm into composable Strategy pattern with three focused interfaces: RoleCountStrategy, ConflictResolutionStrategy, and RoleAssignmentStrategy.

**Architecture:** Three strategy interfaces orchestrated by RoleDistributionService. Each strategy handles one responsibility (count determination, conflict resolution, role assignment). Injectable Random for testability. Value objects for type safety.

**Tech Stack:** Java 8, JUnit 5, Mockito, Google Guice DI, Bukkit API

## Global Constraints

- Java 8 compatibility required
- Use existing test framework: JUnit 5 with Mockito
- Follow existing DDD package structure: `domain/`, `application/`, `infrastructure/`
- Use Guice for dependency injection (existing pattern)
- All tests must use seeded Random for determinism
- TDD discipline: write failing test → implement → verify pass → commit
- Package structure: `me.flamboyant.manhunt.domain.role.distribution.*`

---

## File Structure

### New Files (Phase 1 - Domain Layer)

**Strategy Interfaces:**
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleCountStrategy.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolutionStrategy.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleAssignmentStrategy.java`

**Value Objects:**
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleCounts.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolution.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfig.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTier.java`

**Exceptions:**
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionException.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InvalidConfigurationException.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InsufficientPlayersException.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InsufficientRolesException.java`

**Strategy Implementations:**
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/TieredRoleCountStrategy.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/OverwriteConflictResolution.java`
- `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/ProbabilisticRoleAssignment.java`

**Test Files:**
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleCountsTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTierTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfigTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolutionTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/TieredRoleCountStrategyTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/OverwriteConflictResolutionTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/ProbabilisticRoleAssignmentTest.java`
- `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionTestFixtures.java`

### Modified Files (Phase 2 - Integration)

- `src/main/java/me/flamboyant/manhunt/application/services/RoleDistributionService.java` (refactor to use strategies)
- `src/main/java/me/flamboyant/manhunt/application/commands/DistributeRolesCommand.java` (add optional fields)
- `src/main/java/me/flamboyant/manhunt/infrastructure/di/ManhuntModule.java` (add strategy providers)
- `src/test/java/me/flamboyant/manhunt/application/services/RoleDistributionServiceTest.java` (integration tests)

### Deleted Files (Phase 3 - Cleanup)

- `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java` (deprecated)

---

## Task 1: Exception Hierarchy

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionException.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InvalidConfigurationException.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InsufficientPlayersException.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/InsufficientRolesException.java`

**Interfaces:**
- Consumes: Nothing (foundation)
- Produces: Exception types used by all other components

- [ ] **Step 1: Create base exception**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class RoleDistributionException extends RuntimeException {
    public RoleDistributionException(String message) {
        super(message);
    }

    public RoleDistributionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Create InvalidConfigurationException**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class InvalidConfigurationException extends RoleDistributionException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Create InsufficientPlayersException**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class InsufficientPlayersException extends RoleDistributionException {
    public InsufficientPlayersException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Create InsufficientRolesException**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class InsufficientRolesException extends RoleDistributionException {
    public InsufficientRolesException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/*.java
git commit -m "feat(domain): add role distribution exception hierarchy (Priority 10)"
```

---

## Task 2: RoleCounts Value Object

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleCounts.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleCountsTest.java`

**Interfaces:**
- Consumes: `RoleDistributionException`, `InvalidConfigurationException`
- Produces: `RoleCounts` class with methods: `getSpeedrunners()`, `getAllies()`, `getHunters()`, `getNeutrals()`, `total()`, `subtract(RoleCounts)`, `exceedsAny(RoleCounts)`

- [ ] **Step 1: Write failing test for construction with valid counts**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleCountsTest {
    @Test
    void testConstructor_validCounts() {
        RoleCounts counts = new RoleCounts(2, 1, 5, 0);
        
        assertEquals(2, counts.getSpeedrunners());
        assertEquals(1, counts.getAllies());
        assertEquals(5, counts.getHunters());
        assertEquals(0, counts.getNeutrals());
        assertEquals(8, counts.total());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoleCountsTest#testConstructor_validCounts`
Expected: FAIL with "cannot find symbol: class RoleCounts"

- [ ] **Step 3: Implement RoleCounts class**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class RoleCounts {
    private final int speedrunners;
    private final int allies;
    private final int hunters;
    private final int neutrals;

    public RoleCounts(int speedrunners, int allies, int hunters, int neutrals) {
        if (speedrunners < 0 || allies < 0 || hunters < 0 || neutrals < 0) {
            throw new InvalidConfigurationException(
                "Role counts must be non-negative: speedrunners=" + speedrunners +
                ", allies=" + allies + ", hunters=" + hunters + ", neutrals=" + neutrals
            );
        }
        this.speedrunners = speedrunners;
        this.allies = allies;
        this.hunters = hunters;
        this.neutrals = neutrals;
    }

    public int getSpeedrunners() {
        return speedrunners;
    }

    public int getAllies() {
        return allies;
    }

    public int getHunters() {
        return hunters;
    }

    public int getNeutrals() {
        return neutrals;
    }

    public int total() {
        return speedrunners + allies + hunters + neutrals;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoleCountsTest#testConstructor_validCounts`
Expected: PASS

- [ ] **Step 5: Write test for negative count validation**

```java
@Test
void testConstructor_negativeSpeedrunners_throwsException() {
    InvalidConfigurationException ex = assertThrows(
        InvalidConfigurationException.class,
        () -> new RoleCounts(-1, 0, 5, 0)
    );
    assertTrue(ex.getMessage().contains("non-negative"));
}

@Test
void testConstructor_negativeAllies_throwsException() {
    assertThrows(
        InvalidConfigurationException.class,
        () -> new RoleCounts(1, -1, 5, 0)
    );
}
```

- [ ] **Step 6: Run test to verify validation works**

Run: `mvn test -Dtest=RoleCountsTest`
Expected: All tests PASS

- [ ] **Step 7: Write test for subtract method**

```java
@Test
void testSubtract_validSubtraction() {
    RoleCounts original = new RoleCounts(3, 2, 10, 1);
    RoleCounts toSubtract = new RoleCounts(1, 1, 3, 0);
    
    RoleCounts result = original.subtract(toSubtract);
    
    assertEquals(2, result.getSpeedrunners());
    assertEquals(1, result.getAllies());
    assertEquals(7, result.getHunters());
    assertEquals(1, result.getNeutrals());
}
```

- [ ] **Step 8: Run test to verify it fails**

Run: `mvn test -Dtest=RoleCountsTest#testSubtract_validSubtraction`
Expected: FAIL with "cannot find symbol: method subtract"

- [ ] **Step 9: Implement subtract method**

```java
public RoleCounts subtract(RoleCounts other) {
    return new RoleCounts(
        this.speedrunners - other.speedrunners,
        this.allies - other.allies,
        this.hunters - other.hunters,
        this.neutrals - other.neutrals
    );
}
```

- [ ] **Step 10: Run test to verify it passes**

Run: `mvn test -Dtest=RoleCountsTest#testSubtract_validSubtraction`
Expected: PASS

- [ ] **Step 11: Write test for exceedsAny method**

```java
@Test
void testExceedsAny_speedrunnersExceed_returnsTrue() {
    RoleCounts limit = new RoleCounts(2, 1, 5, 0);
    RoleCounts actual = new RoleCounts(3, 1, 5, 0);
    
    assertTrue(actual.exceedsAny(limit));
}

@Test
void testExceedsAny_withinLimits_returnsFalse() {
    RoleCounts limit = new RoleCounts(2, 1, 5, 0);
    RoleCounts actual = new RoleCounts(1, 0, 4, 0);
    
    assertFalse(actual.exceedsAny(limit));
}
```

- [ ] **Step 12: Run test to verify it fails**

Run: `mvn test -Dtest=RoleCountsTest#testExceedsAny*`
Expected: FAIL with "cannot find symbol: method exceedsAny"

- [ ] **Step 13: Implement exceedsAny method**

```java
public boolean exceedsAny(RoleCounts limit) {
    return this.speedrunners > limit.speedrunners
        || this.allies > limit.allies
        || this.hunters > limit.hunters
        || this.neutrals > limit.neutrals;
}
```

- [ ] **Step 14: Run all tests to verify they pass**

Run: `mvn test -Dtest=RoleCountsTest`
Expected: All tests PASS

- [ ] **Step 15: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleCounts.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleCountsTest.java
git commit -m "feat(domain): add RoleCounts value object with validation (Priority 10)"
```

---

## Task 3: PlayerCountTier Value Object

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTier.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTierTest.java`

**Interfaces:**
- Consumes: `InvalidConfigurationException`
- Produces: `PlayerCountTier` class with methods: `getMinPlayers()`, `getMaxPlayers()`, `getSpeedrunners()`, `getAllies()`, `matches(int)`

- [ ] **Step 1: Write failing test for tier matching**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerCountTierTest {
    @Test
    void testMatches_playerCountWithinRange_returnsTrue() {
        PlayerCountTier tier = new PlayerCountTier(4, 7, 1, 0);
        
        assertTrue(tier.matches(4));
        assertTrue(tier.matches(5));
        assertTrue(tier.matches(7));
    }
    
    @Test
    void testMatches_playerCountOutsideRange_returnsFalse() {
        PlayerCountTier tier = new PlayerCountTier(4, 7, 1, 0);
        
        assertFalse(tier.matches(3));
        assertFalse(tier.matches(8));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PlayerCountTierTest`
Expected: FAIL with "cannot find symbol: class PlayerCountTier"

- [ ] **Step 3: Implement PlayerCountTier class**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public class PlayerCountTier {
    private final int minPlayers;
    private final int maxPlayers;
    private final int speedrunners;
    private final int allies;

    public PlayerCountTier(int minPlayers, int maxPlayers, int speedrunners, int allies) {
        if (minPlayers < 1) {
            throw new InvalidConfigurationException("minPlayers must be at least 1");
        }
        if (maxPlayers < minPlayers) {
            throw new InvalidConfigurationException(
                "maxPlayers (" + maxPlayers + ") must be >= minPlayers (" + minPlayers + ")"
            );
        }
        if (speedrunners < 0 || allies < 0) {
            throw new InvalidConfigurationException("Role counts must be non-negative");
        }
        
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.speedrunners = speedrunners;
        this.allies = allies;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getSpeedrunners() {
        return speedrunners;
    }

    public int getAllies() {
        return allies;
    }

    public boolean matches(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=PlayerCountTierTest`
Expected: PASS

- [ ] **Step 5: Write validation tests**

```java
@Test
void testConstructor_invalidMinPlayers_throwsException() {
    assertThrows(
        InvalidConfigurationException.class,
        () -> new PlayerCountTier(0, 5, 1, 0)
    );
}

@Test
void testConstructor_maxLessThanMin_throwsException() {
    assertThrows(
        InvalidConfigurationException.class,
        () -> new PlayerCountTier(8, 4, 1, 0)
    );
}
```

- [ ] **Step 6: Run validation tests to verify they pass**

Run: `mvn test -Dtest=PlayerCountTierTest`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTier.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/PlayerCountTierTest.java
git commit -m "feat(domain): add PlayerCountTier value object (Priority 10)"
```

---

## Task 4: RoleDistributionConfig Value Object

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfig.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfigTest.java`

**Interfaces:**
- Consumes: `PlayerCountTier`, `InvalidConfigurationException`
- Produces: `RoleDistributionConfig` class with methods: `getSpeedrunnerCount()`, `getAllyCount()`, `isSpecialRolesOnly()`, `getSpecialRoleProbability()`, `getTiers()`, static factory methods `balanced()`, `custom(int, int)`, `specialOnly()`

- [ ] **Step 1: Write failing test for balanced config**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleDistributionConfigTest {
    @Test
    void testBalanced_createsConfigWithDefaultTiers() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        
        assertEquals(0, config.getSpeedrunnerCount()); // auto-calculate
        assertEquals(0, config.getAllyCount()); // auto-calculate
        assertFalse(config.isSpecialRolesOnly());
        assertEquals(0.3, config.getSpecialRoleProbability(), 0.001);
        assertFalse(config.getTiers().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoleDistributionConfigTest#testBalanced_createsConfigWithDefaultTiers`
Expected: FAIL with "cannot find symbol: class RoleDistributionConfig"

- [ ] **Step 3: Implement RoleDistributionConfig class**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoleDistributionConfig {
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final double specialRoleProbability;
    private final List<PlayerCountTier> tiers;

    public RoleDistributionConfig(int speedrunnerCount, int allyCount, boolean specialRolesOnly,
                                  double specialRoleProbability, List<PlayerCountTier> tiers) {
        if (speedrunnerCount < 0 || allyCount < 0) {
            throw new InvalidConfigurationException("Counts must be non-negative");
        }
        if (specialRoleProbability < 0.0 || specialRoleProbability > 1.0) {
            throw new InvalidConfigurationException(
                "specialRoleProbability must be between 0.0 and 1.0, got: " + specialRoleProbability
            );
        }
        if (tiers == null || tiers.isEmpty()) {
            throw new InvalidConfigurationException("Tiers cannot be null or empty");
        }
        
        this.speedrunnerCount = speedrunnerCount;
        this.allyCount = allyCount;
        this.specialRolesOnly = specialRolesOnly;
        this.specialRoleProbability = specialRoleProbability;
        this.tiers = Collections.unmodifiableList(new ArrayList<>(tiers));
    }

    public int getSpeedrunnerCount() {
        return speedrunnerCount;
    }

    public int getAllyCount() {
        return allyCount;
    }

    public boolean isSpecialRolesOnly() {
        return specialRolesOnly;
    }

    public double getSpecialRoleProbability() {
        return specialRoleProbability;
    }

    public List<PlayerCountTier> getTiers() {
        return tiers;
    }

    public static RoleDistributionConfig balanced() {
        List<PlayerCountTier> defaultTiers = new ArrayList<>();
        defaultTiers.add(new PlayerCountTier(4, 7, 1, 0));
        defaultTiers.add(new PlayerCountTier(8, 12, 2, 0));
        defaultTiers.add(new PlayerCountTier(13, 16, 2, 1));
        defaultTiers.add(new PlayerCountTier(17, 999, 3, 1));
        
        return new RoleDistributionConfig(0, 0, false, 0.3, defaultTiers);
    }

    public static RoleDistributionConfig custom(int speedrunners, int allies) {
        return new RoleDistributionConfig(
            speedrunners,
            allies,
            false,
            0.3,
            balanced().getTiers()
        );
    }

    public static RoleDistributionConfig specialOnly() {
        return new RoleDistributionConfig(0, 0, true, 1.0, balanced().getTiers());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDistributionConfigTest#testBalanced_createsConfigWithDefaultTiers`
Expected: PASS

- [ ] **Step 5: Write test for custom config**

```java
@Test
void testCustom_createsConfigWithExplicitCounts() {
    RoleDistributionConfig config = RoleDistributionConfig.custom(2, 1);
    
    assertEquals(2, config.getSpeedrunnerCount());
    assertEquals(1, config.getAllyCount());
    assertFalse(config.isSpecialRolesOnly());
    assertEquals(0.3, config.getSpecialRoleProbability(), 0.001);
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDistributionConfigTest#testCustom_createsConfigWithExplicitCounts`
Expected: PASS

- [ ] **Step 7: Write test for specialOnly config**

```java
@Test
void testSpecialOnly_createsConfigWithSpecialFlag() {
    RoleDistributionConfig config = RoleDistributionConfig.specialOnly();
    
    assertEquals(0, config.getSpeedrunnerCount());
    assertTrue(config.isSpecialRolesOnly());
    assertEquals(1.0, config.getSpecialRoleProbability(), 0.001);
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDistributionConfigTest#testSpecialOnly_createsConfigWithSpecialFlag`
Expected: PASS

- [ ] **Step 9: Write validation tests**

```java
@Test
void testConstructor_invalidProbability_throwsException() {
    List<PlayerCountTier> tiers = RoleDistributionConfig.balanced().getTiers();
    
    assertThrows(
        InvalidConfigurationException.class,
        () -> new RoleDistributionConfig(0, 0, false, 1.5, tiers)
    );
}

@Test
void testConstructor_emptyTiers_throwsException() {
    assertThrows(
        InvalidConfigurationException.class,
        () -> new RoleDistributionConfig(0, 0, false, 0.3, Collections.emptyList())
    );
}
```

- [ ] **Step 10: Run all tests to verify they pass**

Run: `mvn test -Dtest=RoleDistributionConfigTest`
Expected: All tests PASS

- [ ] **Step 11: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfig.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionConfigTest.java
git commit -m "feat(domain): add RoleDistributionConfig with factory methods (Priority 10)"
```

---

## Task 5: ConflictResolution Value Object

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolution.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolutionTest.java`

**Interfaces:**
- Consumes: `RoleCounts`, `InvalidConfigurationException`, `ManhuntRoleIdentifier` (existing), `Player` (Bukkit)
- Produces: `ConflictResolution` class with methods: `getFixedAssignments()`, `getAdjustedCounts()`, `getPlayersNeedingAssignment()`

- [ ] **Step 1: Write failing test for construction**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConflictResolutionTest {
    @Test
    void testConstructor_validResolution() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        
        RoleCounts counts = new RoleCounts(1, 0, 5, 0);
        List<Player> needing = Arrays.asList(p2);
        
        ConflictResolution resolution = new ConflictResolution(fixed, counts, needing);
        
        assertEquals(1, resolution.getFixedAssignments().size());
        assertEquals(counts, resolution.getAdjustedCounts());
        assertEquals(1, resolution.getPlayersNeedingAssignment().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ConflictResolutionTest#testConstructor_validResolution`
Expected: FAIL with "cannot find symbol: class ConflictResolution"

- [ ] **Step 3: Implement ConflictResolution class**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.*;

public class ConflictResolution {
    private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;
    private final RoleCounts adjustedCounts;
    private final List<Player> playersNeedingAssignment;

    public ConflictResolution(Map<Player, ManhuntRoleIdentifier> fixedAssignments,
                             RoleCounts adjustedCounts,
                             List<Player> playersNeedingAssignment) {
        if (fixedAssignments == null || adjustedCounts == null || playersNeedingAssignment == null) {
            throw new InvalidConfigurationException("ConflictResolution parameters cannot be null");
        }
        
        // Validate no player in both fixed and needing assignment
        Set<Player> fixedPlayers = fixedAssignments.keySet();
        for (Player player : playersNeedingAssignment) {
            if (fixedPlayers.contains(player)) {
                throw new InvalidConfigurationException(
                    "Player cannot be in both fixed assignments and needing assignment: " + player.getName()
                );
            }
        }
        
        this.fixedAssignments = Collections.unmodifiableMap(new HashMap<>(fixedAssignments));
        this.adjustedCounts = adjustedCounts;
        this.playersNeedingAssignment = Collections.unmodifiableList(new ArrayList<>(playersNeedingAssignment));
    }

    public Map<Player, ManhuntRoleIdentifier> getFixedAssignments() {
        return fixedAssignments;
    }

    public RoleCounts getAdjustedCounts() {
        return adjustedCounts;
    }

    public List<Player> getPlayersNeedingAssignment() {
        return playersNeedingAssignment;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ConflictResolutionTest#testConstructor_validResolution`
Expected: PASS

- [ ] **Step 5: Write test for validation**

```java
@Test
void testConstructor_playerInBothFixedAndNeeding_throwsException() {
    Player p1 = mock(Player.class);
    when(p1.getName()).thenReturn("Player1");
    
    Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
    fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
    
    RoleCounts counts = new RoleCounts(1, 0, 5, 0);
    List<Player> needing = Arrays.asList(p1); // Same player!
    
    InvalidConfigurationException ex = assertThrows(
        InvalidConfigurationException.class,
        () -> new ConflictResolution(fixed, counts, needing)
    );
    assertTrue(ex.getMessage().contains("Player1"));
}
```

- [ ] **Step 6: Run validation test to verify it passes**

Run: `mvn test -Dtest=ConflictResolutionTest`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolution.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolutionTest.java
git commit -m "feat(domain): add ConflictResolution value object (Priority 10)"
```

---

## Task 6: Strategy Interfaces

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleCountStrategy.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/ConflictResolutionStrategy.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/RoleAssignmentStrategy.java`

**Interfaces:**
- Consumes: `RoleCounts`, `RoleDistributionConfig`, `ConflictResolution`, `Player`, `ManhuntRoleIdentifier`
- Produces: Three strategy interfaces with signatures defined

- [ ] **Step 1: Create RoleCountStrategy interface**

```java
package me.flamboyant.manhunt.domain.role.distribution;

public interface RoleCountStrategy {
    /**
     * Determines role counts based on player count and configuration.
     * 
     * @param playerCount Total number of players in the game
     * @param config Distribution configuration (may contain count hints)
     * @return Role counts for each type
     */
    RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config);
}
```

- [ ] **Step 2: Create ConflictResolutionStrategy interface**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public interface ConflictResolutionStrategy {
    /**
     * Resolves conflicts when fixed assignments don't match desired counts.
     * 
     * @param fixedAssignments Admin-defined player→role assignments
     * @param desiredCounts Target counts from RoleCountStrategy
     * @param allPlayers All players in the game
     * @return Resolution result (which assignments to keep, which counts to adjust)
     */
    ConflictResolution resolveConflicts(
        Map<Player, ManhuntRoleIdentifier> fixedAssignments,
        RoleCounts desiredCounts,
        List<Player> allPlayers
    );
}
```

- [ ] **Step 3: Create RoleAssignmentStrategy interface**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public interface RoleAssignmentStrategy {
    /**
     * Assigns specific roles to players.
     * 
     * @param resolution Result from conflict resolution
     * @param config Distribution configuration (special-only flag, probabilities)
     * @param rng Random number generator (injected for testability)
     * @return Complete player→role assignments
     */
    Map<Player, ManhuntRoleIdentifier> assignRoles(
        ConflictResolution resolution,
        RoleDistributionConfig config,
        Random rng
    );
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/*Strategy.java
git commit -m "feat(domain): add role distribution strategy interfaces (Priority 10)"
```

---

## Task 7: TieredRoleCountStrategy Implementation

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/TieredRoleCountStrategy.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/TieredRoleCountStrategyTest.java`

**Interfaces:**
- Consumes: `RoleCountStrategy`, `RoleCounts`, `RoleDistributionConfig`, `PlayerCountTier`
- Produces: `TieredRoleCountStrategy` implementation with method `determineRoleCounts(int, RoleDistributionConfig)`

- [ ] **Step 1: Write failing test for tier matching**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.distribution.strategies.TieredRoleCountStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TieredRoleCountStrategyTest {
    private final TieredRoleCountStrategy strategy = new TieredRoleCountStrategy();
    
    @Test
    void testDetermineRoleCounts_4players_returns1Speedrunner() {
        RoleDistributionConfig config = RoleDistributionConfig.balanced();
        
        RoleCounts counts = strategy.determineRoleCounts(4, config);
        
        assertEquals(1, counts.getSpeedrunners());
        assertEquals(0, counts.getAllies());
        assertEquals(3, counts.getHunters());
        assertEquals(0, counts.getNeutrals());
        assertEquals(4, counts.total());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=TieredRoleCountStrategyTest#testDetermineRoleCounts_4players_returns1Speedrunner`
Expected: FAIL with "cannot find symbol: class TieredRoleCountStrategy"

- [ ] **Step 3: Implement TieredRoleCountStrategy class**

```java
package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.distribution.*;

public class TieredRoleCountStrategy implements RoleCountStrategy {
    @Override
    public RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config) {
        // If admin specified explicit counts, use those
        if (config.getSpeedrunnerCount() > 0 || config.getAllyCount() > 0) {
            int speedrunners = config.getSpeedrunnerCount();
            int allies = config.getAllyCount();
            int hunters = playerCount - speedrunners - allies;
            return new RoleCounts(speedrunners, allies, hunters, 0);
        }
        
        // Find matching tier
        PlayerCountTier matchingTier = config.getTiers().stream()
            .filter(tier -> tier.matches(playerCount))
            .findFirst()
            .orElseThrow(() -> new InsufficientPlayersException(
                "No tier found for player count: " + playerCount
            ));
        
        int speedrunners = matchingTier.getSpeedrunners();
        int allies = matchingTier.getAllies();
        int hunters = playerCount - speedrunners - allies;
        
        return new RoleCounts(speedrunners, allies, hunters, 0);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TieredRoleCountStrategyTest#testDetermineRoleCounts_4players_returns1Speedrunner`
Expected: PASS

- [ ] **Step 5: Write test for other tier boundaries**

```java
@Test
void testDetermineRoleCounts_7players_returns1Speedrunner() {
    RoleDistributionConfig config = RoleDistributionConfig.balanced();
    RoleCounts counts = strategy.determineRoleCounts(7, config);
    
    assertEquals(1, counts.getSpeedrunners());
    assertEquals(0, counts.getAllies());
    assertEquals(6, counts.getHunters());
}

@Test
void testDetermineRoleCounts_8players_returns2Speedrunners() {
    RoleDistributionConfig config = RoleDistributionConfig.balanced();
    RoleCounts counts = strategy.determineRoleCounts(8, config);
    
    assertEquals(2, counts.getSpeedrunners());
    assertEquals(0, counts.getAllies());
    assertEquals(6, counts.getHunters());
}

@Test
void testDetermineRoleCounts_13players_returns2Speedrunners1Ally() {
    RoleDistributionConfig config = RoleDistributionConfig.balanced();
    RoleCounts counts = strategy.determineRoleCounts(13, config);
    
    assertEquals(2, counts.getSpeedrunners());
    assertEquals(1, counts.getAllies());
    assertEquals(10, counts.getHunters());
}

@Test
void testDetermineRoleCounts_20players_returns3Speedrunners1Ally() {
    RoleDistributionConfig config = RoleDistributionConfig.balanced();
    RoleCounts counts = strategy.determineRoleCounts(20, config);
    
    assertEquals(3, counts.getSpeedrunners());
    assertEquals(1, counts.getAllies());
    assertEquals(16, counts.getHunters());
}
```

- [ ] **Step 6: Run tier tests to verify they pass**

Run: `mvn test -Dtest=TieredRoleCountStrategyTest`
Expected: All tests PASS

- [ ] **Step 7: Write test for explicit count override**

```java
@Test
void testDetermineRoleCounts_explicitCounts_overridesTiers() {
    RoleDistributionConfig config = RoleDistributionConfig.custom(3, 2);
    RoleCounts counts = strategy.determineRoleCounts(10, config);
    
    assertEquals(3, counts.getSpeedrunners());
    assertEquals(2, counts.getAllies());
    assertEquals(5, counts.getHunters());
}
```

- [ ] **Step 8: Run explicit count test to verify it passes**

Run: `mvn test -Dtest=TieredRoleCountStrategyTest#testDetermineRoleCounts_explicitCounts_overridesTiers`
Expected: PASS

- [ ] **Step 9: Run all tests to verify they pass**

Run: `mvn test -Dtest=TieredRoleCountStrategyTest`
Expected: All tests PASS

- [ ] **Step 10: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/TieredRoleCountStrategy.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/TieredRoleCountStrategyTest.java
git commit -m "feat(domain): implement TieredRoleCountStrategy (Priority 10)"
```

---

## Task 8: OverwriteConflictResolution Implementation

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/OverwriteConflictResolution.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/OverwriteConflictResolutionTest.java`

**Interfaces:**
- Consumes: `ConflictResolutionStrategy`, `ConflictResolution`, `RoleCounts`, `ManhuntRoleIdentifier`, `ManhuntRoleType`, `Player`
- Produces: `OverwriteConflictResolution` implementation with method `resolveConflicts(Map, RoleCounts, List)`

- [ ] **Step 1: Write failing test for no conflict**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.distribution.strategies.OverwriteConflictResolution;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OverwriteConflictResolutionTest {
    private final OverwriteConflictResolution strategy = new OverwriteConflictResolution();
    
    @Test
    void testResolveConflicts_noConflict_keepsFixedAndAdjustsCounts() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        
        Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
        fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
        
        RoleCounts desired = new RoleCounts(2, 0, 5, 0);
        List<Player> all = Arrays.asList(p1, p2, p3);
        
        ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);
        
        assertEquals(1, result.getFixedAssignments().size());
        assertEquals(1, result.getAdjustedCounts().getSpeedrunners()); // 2 - 1 fixed
        assertEquals(0, result.getAdjustedCounts().getAllies());
        assertEquals(2, result.getPlayersNeedingAssignment().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OverwriteConflictResolutionTest#testResolveConflicts_noConflict_keepsFixedAndAdjustsCounts`
Expected: FAIL with "cannot find symbol: class OverwriteConflictResolution"

- [ ] **Step 3: Implement OverwriteConflictResolution class**

```java
package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.distribution.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class OverwriteConflictResolution implements ConflictResolutionStrategy {
    @Override
    public ConflictResolution resolveConflicts(
        Map<Player, ManhuntRoleIdentifier> fixedAssignments,
        RoleCounts desiredCounts,
        List<Player> allPlayers
    ) {
        // Count fixed assignments by type
        RoleCounts fixedCounts = countByType(fixedAssignments);
        
        // Check if conflict exists
        if (fixedCounts.exceedsAny(desiredCounts)) {
            // BERZERK MODE: Clear everything and start fresh
            Bukkit.getLogger().warning(
                "Fixed assignments conflict with desired counts. Overwriting all assignments."
            );
            return new ConflictResolution(
                Collections.emptyMap(),
                desiredCounts,
                allPlayers
            );
        }
        
        // No conflict: keep fixed, adjust counts down
        RoleCounts adjustedCounts = desiredCounts.subtract(fixedCounts);
        List<Player> needingAssignment = allPlayers.stream()
            .filter(p -> !fixedAssignments.containsKey(p))
            .collect(Collectors.toList());
        
        return new ConflictResolution(fixedAssignments, adjustedCounts, needingAssignment);
    }
    
    private RoleCounts countByType(Map<Player, ManhuntRoleIdentifier> assignments) {
        int speedrunners = 0;
        int allies = 0;
        int hunters = 0;
        int neutrals = 0;
        
        for (ManhuntRoleIdentifier roleId : assignments.values()) {
            ManhuntRoleType type = roleId.getRoleType();
            if (type == ManhuntRoleType.SPEEDRUNNER) {
                speedrunners++;
            } else if (type == ManhuntRoleType.ALLY) {
                allies++;
            } else if (type == ManhuntRoleType.HUNTER) {
                hunters++;
            } else if (type == ManhuntRoleType.NEUTRAL) {
                neutrals++;
            }
        }
        
        return new RoleCounts(speedrunners, allies, hunters, neutrals);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OverwriteConflictResolutionTest#testResolveConflicts_noConflict_keepsFixedAndAdjustsCounts`
Expected: PASS

- [ ] **Step 5: Write test for conflict (berzerk mode)**

```java
@Test
void testResolveConflicts_conflict_clearsAllFixed() {
    Player p1 = mock(Player.class);
    Player p2 = mock(Player.class);
    Player p3 = mock(Player.class);
    
    Map<Player, ManhuntRoleIdentifier> fixed = new HashMap<>();
    fixed.put(p1, ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
    fixed.put(p2, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);
    fixed.put(p3, ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER);
    
    RoleCounts desired = new RoleCounts(1, 0, 5, 0); // Want 1, have 3 fixed!
    List<Player> all = Arrays.asList(p1, p2, p3);
    
    ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);
    
    assertEquals(0, result.getFixedAssignments().size()); // Cleared
    assertEquals(1, result.getAdjustedCounts().getSpeedrunners()); // Back to desired
    assertEquals(3, result.getPlayersNeedingAssignment().size()); // All players need assignment
}
```

- [ ] **Step 6: Run conflict test to verify it passes**

Run: `mvn test -Dtest=OverwriteConflictResolutionTest#testResolveConflicts_conflict_clearsAllFixed`
Expected: PASS

- [ ] **Step 7: Write test for empty fixed assignments**

```java
@Test
void testResolveConflicts_emptyFixed_returnsAllPlayersNeedingAssignment() {
    Player p1 = mock(Player.class);
    Player p2 = mock(Player.class);
    
    Map<Player, ManhuntRoleIdentifier> fixed = Collections.emptyMap();
    RoleCounts desired = new RoleCounts(1, 0, 5, 0);
    List<Player> all = Arrays.asList(p1, p2);
    
    ConflictResolution result = strategy.resolveConflicts(fixed, desired, all);
    
    assertEquals(0, result.getFixedAssignments().size());
    assertEquals(desired, result.getAdjustedCounts());
    assertEquals(2, result.getPlayersNeedingAssignment().size());
}
```

- [ ] **Step 8: Run all tests to verify they pass**

Run: `mvn test -Dtest=OverwriteConflictResolutionTest`
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/OverwriteConflictResolution.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/OverwriteConflictResolutionTest.java
git commit -m "feat(domain): implement OverwriteConflictResolution strategy (Priority 10)"
```

---

## Task 9: Test Fixtures Utility

**Files:**
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionTestFixtures.java`

**Interfaces:**
- Consumes: `Player`, `ManhuntRoleIdentifier`, `RoleDistributionConfig`
- Produces: Utility methods: `createMockPlayers(int)`, `balancedConfig()`, `specialOnlyConfig()`, `seededRandom(long)`

- [ ] **Step 1: Create test fixtures class**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.mockito.Mockito.*;

public class RoleDistributionTestFixtures {
    public static List<Player> createMockPlayers(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("Player" + (i + 1));
            when(player.getDisplayName()).thenReturn("Player" + (i + 1));
            players.add(player);
        }
        return players;
    }
    
    public static RoleDistributionConfig balancedConfig() {
        return RoleDistributionConfig.balanced();
    }
    
    public static RoleDistributionConfig specialOnlyConfig() {
        return RoleDistributionConfig.specialOnly();
    }
    
    public static Random seededRandom(long seed) {
        return new Random(seed);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/RoleDistributionTestFixtures.java
git commit -m "test(domain): add role distribution test fixtures utility (Priority 10)"
```

---

## Task 10: ProbabilisticRoleAssignment Implementation

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/ProbabilisticRoleAssignment.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/role/distribution/ProbabilisticRoleAssignmentTest.java`

**Interfaces:**
- Consumes: `RoleAssignmentStrategy`, `ConflictResolution`, `RoleDistributionConfig`, `Random`, `ManhuntRoleIdentifier`, `ManhuntRoleType`, `RoleTypeRegistry`
- Produces: `ProbabilisticRoleAssignment` implementation with method `assignRoles(ConflictResolution, RoleDistributionConfig, Random)`

- [ ] **Step 1: Write failing test for special-only mode**

```java
package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.distribution.strategies.ProbabilisticRoleAssignment;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProbabilisticRoleAssignmentTest {
    private final ProbabilisticRoleAssignment strategy = new ProbabilisticRoleAssignment();
    
    @Test
    void testAssignRoles_specialOnlyMode_onlySpecialRoles() {
        List<Player> players = RoleDistributionTestFixtures.createMockPlayers(5);
        RoleCounts counts = new RoleCounts(2, 0, 3, 0);
        ConflictResolution resolution = new ConflictResolution(
            Collections.emptyMap(),
            counts,
            players
        );
        RoleDistributionConfig config = RoleDistributionConfig.specialOnly();
        Random rng = RoleDistributionTestFixtures.seededRandom(12345L);
        
        Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);
        
        assertEquals(5, result.size());
        // Verify no simple roles
        assertFalse(result.containsValue(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE));
        assertFalse(result.containsValue(ManhuntRoleIdentifier.HUNTER_SIMPLE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ProbabilisticRoleAssignmentTest#testAssignRoles_specialOnlyMode_onlySpecialRoles`
Expected: FAIL with "cannot find symbol: class ProbabilisticRoleAssignment"

- [ ] **Step 3: Implement ProbabilisticRoleAssignment class**

```java
package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.RoleTypeRegistry;
import me.flamboyant.manhunt.domain.role.distribution.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ProbabilisticRoleAssignment implements RoleAssignmentStrategy {
    @Override
    public Map<Player, ManhuntRoleIdentifier> assignRoles(
        ConflictResolution resolution,
        RoleDistributionConfig config,
        Random rng
    ) {
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>(resolution.getFixedAssignments());
        RoleCounts counts = resolution.getAdjustedCounts();
        
        // Shuffle players for fairness
        List<Player> shuffled = shuffle(resolution.getPlayersNeedingAssignment(), rng);
        
        // Build role pools
        List<ManhuntRoleIdentifier> speedrunnerPool = buildRolePool(
            ManhuntRoleType.SPEEDRUNNER,
            counts.getSpeedrunners(),
            config,
            rng
        );
        List<ManhuntRoleIdentifier> allyPool = buildRolePool(
            ManhuntRoleType.ALLY,
            counts.getAllies(),
            config,
            rng
        );
        List<ManhuntRoleIdentifier> hunterPool = buildRolePool(
            ManhuntRoleType.HUNTER,
            counts.getHunters(),
            config,
            rng
        );
        
        // Assign roles by popping from pools
        int idx = 0;
        for (Player player : shuffled) {
            ManhuntRoleIdentifier role;
            if (idx < speedrunnerPool.size()) {
                role = speedrunnerPool.get(idx);
            } else if (idx - speedrunnerPool.size() < allyPool.size()) {
                role = allyPool.get(idx - speedrunnerPool.size());
            } else {
                role = hunterPool.get(idx - speedrunnerPool.size() - allyPool.size());
            }
            assignments.put(player, role);
            idx++;
        }
        
        return assignments;
    }
    
    private List<Player> shuffle(List<Player> players, Random rng) {
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, rng);
        return shuffled;
    }
    
    private List<ManhuntRoleIdentifier> buildRolePool(
        ManhuntRoleType type,
        int count,
        RoleDistributionConfig config,
        Random rng
    ) {
        if (config.isSpecialRolesOnly()) {
            return selectSpecialRoles(type, count, rng);
        } else {
            return selectMixedRoles(type, count, config.getSpecialRoleProbability(), rng);
        }
    }
    
    private List<ManhuntRoleIdentifier> selectSpecialRoles(
        ManhuntRoleType type,
        int count,
        Random rng
    ) {
        List<ManhuntRoleIdentifier> pool = new ArrayList<>();
        ManhuntRoleIdentifier simpleRole = getSimpleRole(type);
        List<ManhuntRoleIdentifier> specialRoles = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(type, simpleRole)
        );
        
        if (specialRoles.isEmpty()) {
            throw new InsufficientRolesException(
                "No special roles available for type: " + type
            );
        }
        
        for (int i = 0; i < count; i++) {
            if (specialRoles.isEmpty()) {
                // Ran out of special roles, use simple as fallback
                pool.add(simpleRole);
            } else {
                int idx = rng.nextInt(specialRoles.size());
                pool.add(specialRoles.remove(idx));
            }
        }
        
        return pool;
    }
    
    private List<ManhuntRoleIdentifier> selectMixedRoles(
        ManhuntRoleType type,
        int count,
        double specialProbability,
        Random rng
    ) {
        List<ManhuntRoleIdentifier> pool = new ArrayList<>();
        ManhuntRoleIdentifier simpleRole = getSimpleRole(type);
        List<ManhuntRoleIdentifier> specialRoles = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(type, simpleRole)
        );
        
        for (int i = 0; i < count; i++) {
            if (rng.nextDouble() < specialProbability && !specialRoles.isEmpty()) {
                // Pick random special role and remove from pool
                int idx = rng.nextInt(specialRoles.size());
                pool.add(specialRoles.remove(idx));
            } else {
                pool.add(simpleRole);
            }
        }
        
        return pool;
    }
    
    private ManhuntRoleIdentifier getSimpleRole(ManhuntRoleType type) {
        switch (type) {
            case SPEEDRUNNER:
                return ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE;
            case ALLY:
                return ManhuntRoleIdentifier.ALLY_SIMPLE;
            case HUNTER:
                return ManhuntRoleIdentifier.HUNTER_SIMPLE;
            case NEUTRAL:
                return ManhuntRoleIdentifier.NEUTRAL_SIMPLE;
            default:
                throw new IllegalArgumentException("Unknown role type: " + type);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ProbabilisticRoleAssignmentTest#testAssignRoles_specialOnlyMode_onlySpecialRoles`
Expected: PASS

- [ ] **Step 5: Write test for probability mode with seeded RNG**

```java
@Test
void testAssignRoles_probabilityMode_deterministic() {
    List<Player> players = RoleDistributionTestFixtures.createMockPlayers(10);
    RoleCounts counts = new RoleCounts(2, 1, 7, 0);
    ConflictResolution resolution = new ConflictResolution(
        Collections.emptyMap(),
        counts,
        players
    );
    RoleDistributionConfig config = RoleDistributionTestFixtures.balancedConfig();
    Random rng = RoleDistributionTestFixtures.seededRandom(99999L);
    
    Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);
    
    assertEquals(10, result.size());
    
    // Count role types
    long speedrunners = result.values().stream()
        .filter(r -> r.getRoleType() == me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.SPEEDRUNNER)
        .count();
    long allies = result.values().stream()
        .filter(r -> r.getRoleType() == me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.ALLY)
        .count();
    long hunters = result.values().stream()
        .filter(r -> r.getRoleType() == me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.HUNTER)
        .count();
    
    assertEquals(2, speedrunners);
    assertEquals(1, allies);
    assertEquals(7, hunters);
}
```

- [ ] **Step 6: Run probability test to verify it passes**

Run: `mvn test -Dtest=ProbabilisticRoleAssignmentTest#testAssignRoles_probabilityMode_deterministic`
Expected: PASS

- [ ] **Step 7: Write test for assignment count correctness**

```java
@Test
void testAssignRoles_everyPlayerGetsOneRole() {
    List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
    RoleCounts counts = new RoleCounts(1, 0, 7, 0);
    ConflictResolution resolution = new ConflictResolution(
        Collections.emptyMap(),
        counts,
        players
    );
    RoleDistributionConfig config = RoleDistributionTestFixtures.balancedConfig();
    Random rng = RoleDistributionTestFixtures.seededRandom(42L);
    
    Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(resolution, config, rng);
    
    assertEquals(8, result.size());
    for (Player player : players) {
        assertTrue(result.containsKey(player), "Player " + player.getName() + " missing assignment");
    }
}
```

- [ ] **Step 8: Run all tests to verify they pass**

Run: `mvn test -Dtest=ProbabilisticRoleAssignmentTest`
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/distribution/strategies/ProbabilisticRoleAssignment.java
git add src/test/java/me/flamboyant/manhunt/domain/role/distribution/ProbabilisticRoleAssignmentTest.java
git commit -m "feat(domain): implement ProbabilisticRoleAssignment strategy (Priority 10)"
```

---

## Task 11: Refactor RoleDistributionService

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/application/services/RoleDistributionService.java`

**Interfaces:**
- Consumes: `RoleCountStrategy`, `ConflictResolutionStrategy`, `RoleAssignmentStrategy`, `Random`, `RoleDistributionConfig`, all existing dependencies
- Produces: Refactored `RoleDistributionService.distributeRoles(DistributeRolesCommand)` using strategy composition

- [ ] **Step 1: Read current RoleDistributionService**

Run: `cat src/main/java/me/flamboyant/manhunt/application/services/RoleDistributionService.java | head -60`

- [ ] **Step 2: Add strategy fields and update constructor**

Replace constructor in `RoleDistributionService.java`:

```java
@Singleton
public class RoleDistributionService {
    private final DomainEventPublisher eventPublisher;
    private final RoleCountStrategy countStrategy;
    private final ConflictResolutionStrategy conflictStrategy;
    private final RoleAssignmentStrategy assignmentStrategy;
    private final Random rng;

    @Inject
    public RoleDistributionService(
        DomainEventPublisher eventPublisher,
        RoleCountStrategy countStrategy,
        ConflictResolutionStrategy conflictStrategy,
        RoleAssignmentStrategy assignmentStrategy,
        Random rng
    ) {
        this.eventPublisher = eventPublisher;
        this.countStrategy = countStrategy;
        this.conflictStrategy = conflictStrategy;
        this.assignmentStrategy = assignmentStrategy;
        this.rng = rng;
    }
```

- [ ] **Step 3: Add buildConfigFromCommand helper method**

Add to `RoleDistributionService.java`:

```java
private RoleDistributionConfig buildConfigFromCommand(DistributeRolesCommand command) {
    // Use balanced config as default
    // Command fields will be added in next task
    return RoleDistributionConfig.balanced();
}
```

- [ ] **Step 4: Replace distributeRoles method with strategy orchestration**

Replace `distributeRoles` method in `RoleDistributionService.java`:

```java
public Map<Player, ManhuntRoleIdentifier> distributeRoles(DistributeRolesCommand command) {
    // 1. Build config from command
    RoleDistributionConfig config = buildConfigFromCommand(command);
    
    // 2. Determine desired counts
    RoleCounts desiredCounts = countStrategy.determineRoleCounts(
        command.getPlayers().size(),
        config
    );
    
    // 3. Resolve conflicts with fixed assignments
    ConflictResolution resolution = conflictStrategy.resolveConflicts(
        command.getFixedAssignments(),
        desiredCounts,
        command.getPlayers()
    );
    
    // 4. Assign specific roles
    Map<Player, ManhuntRoleIdentifier> assignments = assignmentStrategy.assignRoles(
        resolution,
        config,
        rng
    );
    
    // 5. Validate and log
    validateAssignments(assignments, command.getPlayers());
    logDistributionSummary(assignments);
    
    // 6. Publish event
    eventPublisher.publish(new RolesDistributedEvent(command.getSessionId(), assignments));
    
    return assignments;
}
```

- [ ] **Step 5: Add validateAssignments helper method**

Add to `RoleDistributionService.java`:

```java
private void validateAssignments(Map<Player, ManhuntRoleIdentifier> assignments, List<Player> players) {
    if (assignments.size() != players.size()) {
        throw new RoleDistributionException(
            "Assignment count mismatch: expected " + players.size() + ", got " + assignments.size()
        );
    }
    
    for (Player player : players) {
        if (!assignments.containsKey(player)) {
            throw new RoleDistributionException("Player missing assignment: " + player.getName());
        }
    }
}
```

- [ ] **Step 6: Add logDistributionSummary helper method**

Add to `RoleDistributionService.java`:

```java
private void logDistributionSummary(Map<Player, ManhuntRoleIdentifier> assignments) {
    long speedrunners = assignments.values().stream()
        .filter(r -> r.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
        .count();
    long allies = assignments.values().stream()
        .filter(r -> r.getRoleType() == ManhuntRoleType.ALLY)
        .count();
    long hunters = assignments.values().stream()
        .filter(r -> r.getRoleType() == ManhuntRoleType.HUNTER)
        .count();
    
    Bukkit.getLogger().info(
        "Role distribution: " + speedrunners + " speedrunners, " +
        allies + " allies, " + hunters + " hunters"
    );
}
```

- [ ] **Step 7: Delete old performDistribution and helper methods**

Remove these methods from `RoleDistributionService.java`:
- `performDistribution(DistributeRolesCommand)`
- `distributeRolesToPlayers(...)`
- `shufflePlayers(List<Player>)`
- `diceSpeedrunnerCount(int)`
- `diceAllyCount(int, int)`

- [ ] **Step 8: Add necessary imports**

Add to imports in `RoleDistributionService.java`:

```java
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import java.util.Random;
```

- [ ] **Step 9: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/application/services/RoleDistributionService.java
git commit -m "refactor(application): use strategy pattern in RoleDistributionService (Priority 10)"
```

---

## Task 12: Update ManhuntModule for Dependency Injection

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/infrastructure/di/ManhuntModule.java`

**Interfaces:**
- Consumes: `RoleCountStrategy`, `ConflictResolutionStrategy`, `RoleAssignmentStrategy`, `Random`, strategy implementations
- Produces: Guice provider methods for strategies

- [ ] **Step 1: Add provider method for RoleCountStrategy**

Add to `ManhuntModule.java`:

```java
@Provides
@Singleton
public RoleCountStrategy provideRoleCountStrategy() {
    return new TieredRoleCountStrategy();
}
```

- [ ] **Step 2: Add provider method for ConflictResolutionStrategy**

Add to `ManhuntModule.java`:

```java
@Provides
@Singleton
public ConflictResolutionStrategy provideConflictResolutionStrategy() {
    return new OverwriteConflictResolution();
}
```

- [ ] **Step 3: Add provider method for RoleAssignmentStrategy**

Add to `ManhuntModule.java`:

```java
@Provides
@Singleton
public RoleAssignmentStrategy provideRoleAssignmentStrategy() {
    return new ProbabilisticRoleAssignment();
}
```

- [ ] **Step 4: Add provider method for Random**

Add to `ManhuntModule.java`:

```java
@Provides
@Singleton
public Random provideRandom() {
    return Common.rng;
}
```

- [ ] **Step 5: Add necessary imports**

Add to imports in `ManhuntModule.java`:

```java
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import java.util.Random;
```

- [ ] **Step 6: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/infrastructure/di/ManhuntModule.java
git commit -m "feat(infrastructure): add strategy providers to ManhuntModule (Priority 10)"
```

---

## Task 13: Integration Tests for RoleDistributionService

**Files:**
- Modify: `src/test/java/me/flamboyant/manhunt/application/services/RoleDistributionServiceTest.java`

**Interfaces:**
- Consumes: `RoleDistributionService`, all strategies, `DistributeRolesCommand`, `GameSessionId`
- Produces: Integration tests verifying full distribution flow

- [ ] **Step 1: Write test for full distribution flow with no fixed assignments**

Add to `RoleDistributionServiceTest.java`:

```java
@Test
void testDistributeRoles_noFixedAssignments_fullFlow() {
    // Arrange
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
    ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
    RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
    Random rng = new Random(12345L);
    
    RoleDistributionService service = new RoleDistributionService(
        eventPublisher,
        countStrategy,
        conflictStrategy,
        assignmentStrategy,
        rng
    );
    
    List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
    GameSessionId sessionId = GameSessionId.generate();
    DistributeRolesCommand command = new DistributeRolesCommand(
        sessionId,
        players,
        0, 0, // auto-calculate
        false,
        Collections.emptyMap()
    );
    
    // Act
    Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);
    
    // Assert
    assertEquals(8, result.size());
    verify(eventPublisher, times(1)).publish(any(RolesDistributedEvent.class));
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDistributionServiceTest#testDistributeRoles_noFixedAssignments_fullFlow`
Expected: PASS

- [ ] **Step 3: Write test for distribution with fixed assignments (no conflict)**

Add to `RoleDistributionServiceTest.java`:

```java
@Test
void testDistributeRoles_fixedAssignmentsNoConflict_keepsFixed() {
    // Arrange
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
    ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
    RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
    Random rng = new Random(12345L);
    
    RoleDistributionService service = new RoleDistributionService(
        eventPublisher,
        countStrategy,
        conflictStrategy,
        assignmentStrategy,
        rng
    );
    
    List<Player> players = RoleDistributionTestFixtures.createMockPlayers(8);
    Player fixedPlayer = players.get(0);
    Map<Player, ManhuntRoleIdentifier> fixedAssignments = new HashMap<>();
    fixedAssignments.put(fixedPlayer, ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);
    
    GameSessionId sessionId = GameSessionId.generate();
    DistributeRolesCommand command = new DistributeRolesCommand(
        sessionId,
        players,
        0, 0,
        false,
        fixedAssignments
    );
    
    // Act
    Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);
    
    // Assert
    assertEquals(8, result.size());
    assertEquals(ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT, result.get(fixedPlayer));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoleDistributionServiceTest#testDistributeRoles_fixedAssignmentsNoConflict_keepsFixed`
Expected: PASS

- [ ] **Step 5: Write test for distribution with conflicting fixed assignments (berzerk mode)**

Add to `RoleDistributionServiceTest.java`:

```java
@Test
void testDistributeRoles_conflictingFixedAssignments_overwritesAll() {
    // Arrange
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RoleCountStrategy countStrategy = new TieredRoleCountStrategy();
    ConflictResolutionStrategy conflictStrategy = new OverwriteConflictResolution();
    RoleAssignmentStrategy assignmentStrategy = new ProbabilisticRoleAssignment();
    Random rng = new Random(12345L);
    
    RoleDistributionService service = new RoleDistributionService(
        eventPublisher,
        countStrategy,
        conflictStrategy,
        assignmentStrategy,
        rng
    );
    
    List<Player> players = RoleDistributionTestFixtures.createMockPlayers(5);
    // Fix 3 speedrunners, but tier only wants 1
    Map<Player, ManhuntRoleIdentifier> fixedAssignments = new HashMap<>();
    fixedAssignments.put(players.get(0), ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE);
    fixedAssignments.put(players.get(1), ManhuntRoleIdentifier.SPEEDRUNNER_CHECKPOINT);
    fixedAssignments.put(players.get(2), ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER);
    
    GameSessionId sessionId = GameSessionId.generate();
    DistributeRolesCommand command = new DistributeRolesCommand(
        sessionId,
        players,
        0, 0,
        false,
        fixedAssignments
    );
    
    // Act
    Map<Player, ManhuntRoleIdentifier> result = service.distributeRoles(command);
    
    // Assert
    assertEquals(5, result.size());
    // Conflict mode should have cleared fixed assignments
    // So players may no longer have their fixed roles
}
```

- [ ] **Step 6: Run all integration tests to verify they pass**

Run: `mvn test -Dtest=RoleDistributionServiceTest`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/test/java/me/flamboyant/manhunt/application/services/RoleDistributionServiceTest.java
git commit -m "test(application): add integration tests for refactored RoleDistributionService (Priority 10)"
```

---

## Task 14: Run All Tests

**Files:**
- N/A (verification step)

**Interfaces:**
- Consumes: All test files
- Produces: Test report

- [ ] **Step 1: Run all tests**

Run: `mvn test`
Expected: All tests PASS

- [ ] **Step 2: Check test coverage**

Run: `mvn test jacoco:report` (if jacoco plugin available)

- [ ] **Step 3: Fix any failing tests**

If tests fail, debug and fix issues before proceeding.

- [ ] **Step 4: Verify no compilation warnings**

Run: `mvn compile -X | grep -i warning`
Expected: No warnings related to new code

---

## Task 15: Delete Deprecated GameRolesManagement

**Files:**
- Delete: `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java`

**Interfaces:**
- Consumes: N/A
- Produces: Removed deprecated class

- [ ] **Step 1: Verify GameRolesManagement is no longer referenced**

Run: `grep -r "GameRolesManagement" src/main/java --exclude-dir=roles`
Expected: No matches (or only in comments)

- [ ] **Step 2: Delete GameRolesManagement.java**

Run: `git rm src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java`

- [ ] **Step 3: Verify compilation still succeeds**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Verify all tests still pass**

Run: `mvn test`
Expected: All tests PASS

- [ ] **Step 5: Commit deletion**

```bash
git commit -m "refactor(roles): delete deprecated GameRolesManagement class (Priority 10)"
```

---

## Task 16: Update REFACTORING_PROGRESS.md

**Files:**
- Modify: `docs/REFACTORING_PROGRESS.md`

**Interfaces:**
- Consumes: N/A
- Produces: Updated progress tracker

- [ ] **Step 1: Mark Priority 10 as completed in REFACTORING_PROGRESS.md**

Update the Priority 10 section:

```markdown
### Priority 10: Extract Role Distribution Strategy ✅
**Status:** Completed
**Assigned To:** Claude Sonnet 4.5
**Started:** 2026-06-24
**Completed:** 2026-06-24
**Estimated Effort:** 4-5 hours
**Actual Effort:** ~5 hours

#### Tasks
- [x] 10.1 Create strategy interfaces (RoleCountStrategy, ConflictResolutionStrategy, RoleAssignmentStrategy)
- [x] 10.2 Create value objects (RoleCounts, ConflictResolution, RoleDistributionConfig, PlayerCountTier)
- [x] 10.3 Implement TieredRoleCountStrategy
- [x] 10.4 Implement OverwriteConflictResolution
- [x] 10.5 Implement ProbabilisticRoleAssignment
- [x] 10.6 Refactor RoleDistributionService to use strategies
- [x] 10.7 Update ManhuntModule for dependency injection
- [x] 10.8 Write comprehensive unit tests
- [x] 10.9 Write integration tests
- [x] 10.10 Delete deprecated GameRolesManagement
- [x] 10.11 Update documentation

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Three separate strategy interfaces for maximum composability
  - Injectable Random for deterministic testing
  - Redesigned algorithm with balanced distribution (no bugs from old code)
  - Default 30% special role probability
  - Tiered player count scaling: 4-7: 1SR, 8-12: 2SR, 13-16: 2SR+1A, 17+: 3SR+1A
- **Commits:** See git log for Priority 10 commits
```

- [ ] **Step 2: Update Phase 3 completion percentage**

Update Phase 3 section:

```markdown
- [ ] Phase 3: Tactical Patterns (2/3 complete) - **Priorities 7, 10 DONE** ✅
```

- [ ] **Step 3: Update total progress**

Update overall progress:

```markdown
### Total Progress: 9/13 priorities completed (69%)
```

- [ ] **Step 4: Commit documentation update**

```bash
git add docs/REFACTORING_PROGRESS.md
git commit -m "docs: mark Priority 10 (Role Distribution Strategy) as complete"
```

---

## Task 17: Create Architecture Documentation

**Files:**
- Create: `docs/architecture/role-distribution.md`

**Interfaces:**
- Consumes: N/A
- Produces: Architecture documentation

- [ ] **Step 1: Create role-distribution.md**

```markdown
# Role Distribution Architecture

**Last Updated:** 2026-06-24  
**Priority:** 10 (DDD Refactoring)

---

## Overview

The role distribution system assigns roles (Speedrunner, Ally, Hunter, Neutral) to players at game start. It uses a composed Strategy pattern with three focused interfaces:

1. **RoleCountStrategy**: Determines how many of each role type
2. **ConflictResolutionStrategy**: Handles conflicts between admin-fixed assignments and desired counts
3. **RoleAssignmentStrategy**: Assigns specific role identifiers to players

---

## Architecture

### Strategy Composition

```
DistributeRolesCommand
    ↓
RoleDistributionConfig (built from command)
    ↓
RoleCountStrategy → RoleCounts
    ↓
ConflictResolutionStrategy → ConflictResolution
    ↓
RoleAssignmentStrategy → Map<Player, ManhuntRoleIdentifier>
    ↓
RolesDistributedEvent
```

Strategies are injected via Guice and orchestrated by `RoleDistributionService`.

---

## Default Strategies

### TieredRoleCountStrategy

**Player Count Tiers:**
- 4-7 players: 1 speedrunner, 0 allies
- 8-12 players: 2 speedrunners, 0 allies
- 13-16 players: 2 speedrunners, 1 ally
- 17+ players: 3 speedrunners, 1-2 allies

Admin can override with explicit counts.

### OverwriteConflictResolution

**Conflict Detection:**
- If fixed assignments exceed desired counts → clear all fixed (berzerk mode)
- Otherwise → keep fixed, adjust counts down

**Example:** Want 1 speedrunner, but 3 players have fixed speedrunner roles → clear all fixed assignments and redistribute.

### ProbabilisticRoleAssignment

**Special Role Selection:**
- **Normal mode**: 30% probability for special variant (configurable)
- **Special-only mode**: Only select special role variants

**Algorithm:**
1. Shuffle players randomly
2. Build role pools (speedrunners, allies, hunters)
3. Assign roles in order from pools

---

## Value Objects

### RoleCounts
- Immutable counts per role type
- Validation: non-negative counts
- Operations: `total()`, `subtract()`, `exceedsAny()`

### ConflictResolution
- Fixed assignments (kept after conflict resolution)
- Adjusted counts (desired - fixed)
- Players needing assignment (not in fixed)

### RoleDistributionConfig
- Speedrunner/ally count hints (0 = auto-calculate)
- Special-only flag
- Special role probability (0.0-1.0, default 0.3)
- Player count tiers

### PlayerCountTier
- Player count range (min-max)
- Role counts for that range
- Matching predicate

---

## Testability

### Injectable Random

`Random` is injected via Guice, enabling deterministic tests with seeded RNG:

```java
Random seeded = new Random(12345L);
Map<Player, ManhuntRoleIdentifier> result = strategy.assignRoles(..., seeded);
// Same seed = same distribution every time
```

### Strategy Isolation

Each strategy is unit tested independently with mocks. Integration tests verify full orchestration.

---

## Extensibility

New strategies can be added without modifying existing code:

**Example: Ratio-Based Count Strategy**
```java
public class RatioBasedRoleCountStrategy implements RoleCountStrategy {
    public RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config) {
        int speedrunners = (int) (playerCount * 0.15); // 15% speedrunners
        int allies = (int) (playerCount * 0.05); // 5% allies
        int hunters = playerCount - speedrunners - allies;
        return new RoleCounts(speedrunners, allies, hunters, 0);
    }
}
```

Swap in `ManhuntModule`:
```java
@Provides
@Singleton
public RoleCountStrategy provideRoleCountStrategy() {
    return new RatioBasedRoleCountStrategy();
}
```

---

## Migration from Old System

**Before (Priority 6-9):**
- Monolithic `performDistribution()` method (174 lines)
- Complex nested conditionals
- Hardcoded probabilities (50%, 20%, 30%)
- Untestable (global `Common.rng`)
- Bugs: infinite loop in `diceAllyCount`, broken shuffle

**After (Priority 10):**
- Three focused strategies (~60 lines each)
- Clear responsibilities
- Configurable probabilities
- Injectable RNG (deterministic tests)
- No known bugs (redesigned from scratch)

---

## See Also

- `docs/superpowers/specs/2026-06-24-role-distribution-strategy-design.md`: Design specification
- `docs/PROBLEMS_PRIORITY_SUMMARY.md`: Priority 10 original problem description
- `REFACTORING_PROGRESS.md`: Implementation tracking
```

- [ ] **Step 2: Commit documentation**

```bash
git add docs/architecture/role-distribution.md
git commit -m "docs: add role distribution architecture documentation (Priority 10)"
```

---

## Success Criteria Verification

- [ ] ✅ Three strategy interfaces implemented with default implementations
- [ ] ✅ All value objects created with validation
- [ ] ✅ `RoleDistributionService` refactored to use strategies
- [ ] ✅ Dependency injection configured in `ManhuntModule`
- [ ] ✅ Comprehensive unit tests (8+ test classes)
- [ ] ✅ Integration tests pass
- [ ] ✅ Old `GameRolesManagement` deleted
- [ ] ✅ Documentation updated
- [ ] ✅ All existing tests still pass
- [ ] ✅ Strategies are swappable without code changes (via DI)

---

**End of Implementation Plan**
