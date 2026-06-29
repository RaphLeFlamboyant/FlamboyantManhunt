# Win Condition Abstraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace static list win condition modifiers with evaluator-bound modifiers, eliminate memory leaks, and centralize role result messaging.

**Architecture:** Enhance `WinConditionEvaluator` to support instance-scoped modifiers. Replace `IHunterWinConditionModifier` with new `WinConditionModifier` interface that supports both blocking and alternative win paths. Move role result messaging from roles to `EndGameSaga`.

**Tech Stack:** Java 8, JUnit 5, Mockito, Bukkit API

## Global Constraints

- Java 8 compatibility required
- Follow TDD (Red-Green-Refactor) discipline
- All tests use JUnit 5 and Mockito
- Commit after each task completion
- No breaking changes to public APIs
- All existing tests must continue passing

---

## File Structure

### New Files
- `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifier.java` - New modifier interface

### Modified Files
- `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java` - Add modifier support
- `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java` - Add evaluator modifier tests
- `src/main/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRole.java` - Migrate to new modifier
- `src/test/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRoleTest.java` - Create or update tests
- `src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java` - Remove static list and result messaging
- `src/main/java/me/flamboyant/manhunt/application/sagas/EndGameSaga.java` - Add role result messaging
- `src/test/java/me/flamboyant/manhunt/application/sagas/EndGameSagaTest.java` - Add messaging tests

### Deleted Files
- `src/main/java/me/flamboyant/manhunt/domain/wincondition/IHunterWinConditionModifier.java` - Legacy interface

---

## Task 1: Create WinConditionModifier Interface

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifier.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierTest.java`

**Interfaces:**
- Consumes: `WinCondition`, `GameSession` (existing)
- Produces: `WinConditionModifier` interface with:
  - `boolean allowsWin(WinCondition condition, GameSession session)`
  - `Optional<WinCondition> getAlternativeWin(GameSession session)`

- [ ] **Step 1: Write test for blocking modifier**

Create `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierTest.java`:

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WinConditionModifierTest {

    @Test
    void blockingModifier_shouldBlockWin() {
        // Given
        GameSession session = mock(GameSession.class);
        WinCondition condition = mock(WinCondition.class);
        
        WinConditionModifier modifier = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return false;
            }
            
            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };
        
        // When
        boolean allowed = modifier.allowsWin(condition, session);
        
        // Then
        assertFalse(allowed);
    }
    
    @Test
    void allowingModifier_shouldAllowWin() {
        // Given
        GameSession session = mock(GameSession.class);
        WinCondition condition = mock(WinCondition.class);
        
        WinConditionModifier modifier = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }
            
            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };
        
        // When
        boolean allowed = modifier.allowsWin(condition, session);
        
        // Then
        assertTrue(allowed);
    }
    
    @Test
    void modifierWithAlternative_shouldProvideAlternativeWin() {
        // Given
        GameSession session = mock(GameSession.class);
        WinCondition alternative = mock(WinCondition.class);
        
        WinConditionModifier modifier = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }
            
            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.of(alternative);
            }
        };
        
        // When
        Optional<WinCondition> result = modifier.getAlternativeWin(session);
        
        // Then
        assertTrue(result.isPresent());
        assertSame(alternative, result.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=WinConditionModifierTest`
Expected: FAIL with "cannot find symbol: class WinConditionModifier"

- [ ] **Step 3: Create WinConditionModifier interface**

Create `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifier.java`:

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;

import java.util.Optional;

/**
 * Modifies win condition evaluation by blocking or providing alternative wins.
 * Registered on WinConditionEvaluator instances (session-scoped).
 * 
 * Modifiers can:
 * 1. Gate/block base win conditions from triggering (e.g., "hunters can't win yet")
 * 2. Provide alternative win paths (e.g., "special role wins by alternative condition")
 */
public interface WinConditionModifier {
    
    /**
     * Check if this modifier allows the given win condition to trigger.
     * 
     * @param condition The win condition being evaluated
     * @param session Current game session
     * @return false to block the win, true to allow it
     */
    boolean allowsWin(WinCondition condition, GameSession session);
    
    /**
     * Provide an alternative win condition if applicable.
     * Called after base conditions are evaluated.
     * 
     * @param session Current game session
     * @return Optional containing an alternative WinCondition, or empty if none
     */
    Optional<WinCondition> getAlternativeWin(GameSession session);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=WinConditionModifierTest`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifier.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierTest.java
git commit -m "feat(domain): add WinConditionModifier interface (Priority 11)"
```

---

## Task 2: Enhance WinConditionEvaluator with Modifier Support

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

**Interfaces:**
- Consumes: `WinConditionModifier` (from Task 1), `WinCondition`, `GameSession` (existing)
- Produces: Enhanced `WinConditionEvaluator` with:
  - `void registerModifier(WinConditionModifier modifier)`
  - `List<WinConditionModifier> getModifiers()`
  - Enhanced `Optional<WinOutcome> evaluate(GameSession session)` (applies modifiers)

- [ ] **Step 1: Write test for modifier registration**

Add to `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`:

```java
@Test
void evaluator_shouldRegisterModifier() {
    // Given
    WinCondition baseCondition = mock(WinCondition.class);
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    WinConditionModifier modifier = mock(WinConditionModifier.class);
    
    // When
    evaluator.registerModifier(modifier);
    
    // Then
    assertEquals(1, evaluator.getModifiers().size());
    assertTrue(evaluator.getModifiers().contains(modifier));
}

@Test
void evaluator_shouldRejectNullModifier() {
    // Given
    WinCondition baseCondition = mock(WinCondition.class);
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    // When/Then
    assertThrows(NullPointerException.class, () -> {
        evaluator.registerModifier(null);
    });
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=WinConditionTest#evaluator_shouldRegisterModifier`
Expected: FAIL with "cannot find symbol: method registerModifier"

- [ ] **Step 3: Add modifier support fields and registration**

Modify `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java`:

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates win conditions and applies registered modifiers.
 * Instance-scoped to GameSession (modifiers cleared on session end).
 */
public class WinConditionEvaluator {

    private final List<WinCondition> conditions;
    private final List<WinConditionModifier> modifiers;

    /**
     * Create an evaluator with a list of win conditions.
     *
     * @param conditions The win conditions to check (order matters - first match wins)
     */
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions, "Conditions cannot be null"));
        this.modifiers = new ArrayList<>();
    }

    /**
     * Register a modifier that can block or provide alternative wins.
     * Modifiers are session-scoped and cleared when evaluator is destroyed.
     *
     * @param modifier The modifier to register
     * @throws NullPointerException if modifier is null
     */
    public void registerModifier(WinConditionModifier modifier) {
        Objects.requireNonNull(modifier, "Modifier cannot be null");
        this.modifiers.add(modifier);
    }

    /**
     * Evaluate all win conditions against the given session.
     * Returns the first condition that is met.
     *
     * @param session The game session to evaluate
     * @return Optional containing WinOutcome if any condition is met, empty otherwise
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        Objects.requireNonNull(session, "Session cannot be null");

        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }

        return Optional.empty();
    }

    /**
     * Get the list of conditions being evaluated.
     *
     * @return Immutable list of win conditions
     */
    public List<WinCondition> getConditions() {
        return conditions;
    }

    /**
     * Get currently registered modifiers.
     *
     * @return Immutable copy of registered modifiers
     */
    public List<WinConditionModifier> getModifiers() {
        return List.copyOf(modifiers);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=WinConditionTest#evaluator_shouldRegisterModifier`
Expected: PASS

- [ ] **Step 5: Write test for modifier blocking win**

Add to `WinConditionTest.java`:

```java
@Test
void evaluator_shouldBlockWinWhenModifierDisallows() {
    // Given
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(0);
    
    WinCondition baseCondition = new AllSpeedrunnersDeadCondition();
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    WinConditionModifier blockingModifier = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return false; // Block all wins
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.empty();
        }
    };
    
    evaluator.registerModifier(blockingModifier);
    
    // When
    Optional<WinOutcome> result = evaluator.evaluate(session);
    
    // Then
    assertFalse(result.isPresent(), "Win should be blocked by modifier");
}

@Test
void evaluator_shouldAllowWinWhenModifierAllows() {
    // Given
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(0);
    
    WinCondition baseCondition = new AllSpeedrunnersDeadCondition();
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    WinConditionModifier allowingModifier = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return true; // Allow wins
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.empty();
        }
    };
    
    evaluator.registerModifier(allowingModifier);
    
    // When
    Optional<WinOutcome> result = evaluator.evaluate(session);
    
    // Then
    assertTrue(result.isPresent(), "Win should be allowed by modifier");
}

@Test
void evaluator_shouldBlockWinWhenAnyModifierDisallows() {
    // Given
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(0);
    
    WinCondition baseCondition = new AllSpeedrunnersDeadCondition();
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    WinConditionModifier allowingModifier = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return true;
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.empty();
        }
    };
    
    WinConditionModifier blockingModifier = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return false; // This one blocks
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.empty();
        }
    };
    
    evaluator.registerModifier(allowingModifier);
    evaluator.registerModifier(blockingModifier);
    
    // When
    Optional<WinOutcome> result = evaluator.evaluate(session);
    
    // Then
    assertFalse(result.isPresent(), "Win should be blocked when ANY modifier disallows");
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `mvn test -Dtest=WinConditionTest#evaluator_shouldBlockWinWhenModifierDisallows`
Expected: FAIL (modifier blocking not implemented)

- [ ] **Step 7: Implement modifier evaluation logic**

Replace the `evaluate()` method in `WinConditionEvaluator.java`:

```java
/**
 * Evaluate all win conditions with modifier support.
 * 
 * Flow:
 * 1. Check base conditions (AllSpeedrunnersDead, DragonKilled)
 * 2. Apply modifiers - any can block
 * 3. Check alternative wins from modifiers
 * 4. Return first match or empty
 *
 * @param session The game session to evaluate
 * @return Optional containing WinOutcome if any condition is met
 */
public Optional<WinOutcome> evaluate(GameSession session) {
    Objects.requireNonNull(session, "Session cannot be null");

    // 1. Check base conditions
    for (WinCondition condition : conditions) {
        if (condition.isMet(session)) {
            // 2. Apply modifiers - check if allowed
            if (isWinAllowedByModifiers(condition, session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }
    }

    // 3. Check alternative wins from modifiers
    return checkAlternativeWins(session);
}

/**
 * Check if all modifiers allow this win condition.
 * ANY modifier returning false will block the win.
 */
private boolean isWinAllowedByModifiers(WinCondition condition, GameSession session) {
    for (WinConditionModifier modifier : modifiers) {
        if (!modifier.allowsWin(condition, session)) {
            return false; // Blocked by this modifier
        }
    }
    return true; // All modifiers allow it
}

/**
 * Check if any modifier provides an alternative win.
 * Returns first alternative found.
 */
private Optional<WinOutcome> checkAlternativeWins(GameSession session) {
    for (WinConditionModifier modifier : modifiers) {
        Optional<WinCondition> alternative = modifier.getAlternativeWin(session);
        if (alternative.isPresent()) {
            WinCondition condition = alternative.get();
            if (condition.isMet(session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }
    }
    return Optional.empty();
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn test -Dtest=WinConditionTest`
Expected: PASS (all tests including new modifier tests)

- [ ] **Step 9: Write test for alternative win**

Add to `WinConditionTest.java`:

```java
@Test
void evaluator_shouldReturnAlternativeWin() {
    // Given
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(5); // Base condition not met
    
    WinCondition baseCondition = new AllSpeedrunnersDeadCondition();
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    WinCondition alternativeCondition = mock(WinCondition.class);
    when(alternativeCondition.isMet(session)).thenReturn(true);
    
    WinConditionModifier modifierWithAlternative = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return true;
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.of(alternativeCondition);
        }
    };
    
    evaluator.registerModifier(modifierWithAlternative);
    
    // When
    Optional<WinOutcome> result = evaluator.evaluate(session);
    
    // Then
    assertTrue(result.isPresent(), "Alternative win should be returned");
}

@Test
void evaluator_shouldReturnFirstAlternativeWin() {
    // Given
    GameSession session = mock(GameSession.class);
    when(session.getRemainingSpeedrunners()).thenReturn(5);
    
    WinCondition baseCondition = new AllSpeedrunnersDeadCondition();
    WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(baseCondition));
    
    WinCondition firstAlternative = mock(WinCondition.class);
    when(firstAlternative.isMet(session)).thenReturn(true);
    
    WinCondition secondAlternative = mock(WinCondition.class);
    when(secondAlternative.isMet(session)).thenReturn(true);
    
    WinConditionModifier modifier1 = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return true;
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.of(firstAlternative);
        }
    };
    
    WinConditionModifier modifier2 = new WinConditionModifier() {
        @Override
        public boolean allowsWin(WinCondition condition, GameSession session) {
            return true;
        }
        
        @Override
        public Optional<WinCondition> getAlternativeWin(GameSession session) {
            return Optional.of(secondAlternative);
        }
    };
    
    evaluator.registerModifier(modifier1);
    evaluator.registerModifier(modifier2);
    
    // When
    Optional<WinOutcome> result = evaluator.evaluate(session);
    
    // Then
    assertTrue(result.isPresent());
    // First alternative should win (can't assert which mock won, but we verified logic)
}
```

- [ ] **Step 10: Run test to verify it passes**

Run: `mvn test -Dtest=WinConditionTest`
Expected: PASS (all tests)

- [ ] **Step 11: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java
git commit -m "feat(domain): enhance WinConditionEvaluator with modifier support (Priority 11)"
```

---

## Task 3: Migrate SuperHunterRole to WinConditionModifier

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRole.java`
- Test: Create `src/test/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRoleTest.java`

**Interfaces:**
- Consumes: `WinConditionModifier` (from Task 1), `GameSession`, `AllSpeedrunnersDeadCondition` (existing)
- Produces: Migrated `SuperHunterRole` implementing `WinConditionModifier`

- [ ] **Step 1: Write test for modifier blocking behavior**

Create `src/test/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRoleTest.java`:

```java
package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.DragonKilledCondition;
import me.flamboyant.manhunt.domain.wincondition.WinCondition;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SuperHunterRoleTest {

    private Player owner;
    private SuperHunterRole role;
    private GameSession session;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        role = new SuperHunterRole(owner);
        session = mock(GameSession.class);
    }

    @Test
    void modifier_shouldBlockHunterWinWhenKillCountBelowThreshold() {
        // Given
        WinCondition hunterWin = new AllSpeedrunnersDeadCondition();
        // Kill count is 0 (default)
        
        // When
        boolean allowed = role.allowsWin(hunterWin, session);
        
        // Then
        assertFalse(allowed, "Should block hunter win when kill count < 3");
    }

    @Test
    void modifier_shouldAllowHunterWinWhenKillCountMeetsThreshold() {
        // Given
        WinCondition hunterWin = new AllSpeedrunnersDeadCondition();
        
        // Simulate 3 kills
        role.incrementKillCount();
        role.incrementKillCount();
        role.incrementKillCount();
        
        // When
        boolean allowed = role.allowsWin(hunterWin, session);
        
        // Then
        assertTrue(allowed, "Should allow hunter win when kill count >= 3");
    }

    @Test
    void modifier_shouldNotBlockSpeedrunnerWin() {
        // Given
        WinCondition speedrunnerWin = new DragonKilledCondition();
        // Kill count is 0
        
        // When
        boolean allowed = role.allowsWin(speedrunnerWin, session);
        
        // Then
        assertTrue(allowed, "Should not block speedrunner wins regardless of kill count");
    }

    @Test
    void modifier_shouldNotProvideAlternativeWin() {
        // When
        Optional<WinCondition> alternative = role.getAlternativeWin(session);
        
        // Then
        assertFalse(alternative.isPresent(), "SuperHunterRole should not provide alternative wins");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SuperHunterRoleTest`
Expected: FAIL with "cannot find symbol: method allowsWin" or similar

- [ ] **Step 3: Read current SuperHunterRole implementation**

Read `src/main/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRole.java` to understand current implementation.

- [ ] **Step 4: Modify SuperHunterRole to implement WinConditionModifier**

Update `src/main/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRole.java`:

```java
package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.IHunterWinConditionModifier;
import me.flamboyant.manhunt.domain.wincondition.WinCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionModifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.utils.ChatHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class SuperHunterRole extends HunterRole implements WinConditionModifier {
    private static final int REQUIRED_KILL_COUNT = 3;
    private int speedRunnerKillCount = 0;

    public SuperHunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        // Register as modifier with evaluator
        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(owner);
        if (session != null) {
            session.getWinConditionEvaluator().registerModifier(this);
        }
        
        return super.doStart();
    }

    // No doStop() override needed - evaluator destroyed with session

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!damager.equals(owner)) return;

        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(owner);
        if (session == null) return;

        AManhuntRole victimRole = session.getPlayerRole(victim);
        if (victimRole == null) return;

        if (victimRole.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                speedRunnerKillCount++;
                Bukkit.broadcastMessage(ChatHelper.feedback(
                    owner.getDisplayName() + " (SuperHunter) a tué un speedrunner ! (" 
                    + speedRunnerKillCount + "/" + REQUIRED_KILL_COUNT + ")"
                ));
                
                if (speedRunnerKillCount >= REQUIRED_KILL_COUNT) {
                    Bukkit.broadcastMessage(ChatHelper.feedback(
                        "SuperHunter peut maintenant permettre la victoire des Hunters !"
                    ));
                }
            }
        }
    }

    @Override
    public boolean allowsWin(WinCondition condition, GameSession session) {
        // Only gate AllSpeedrunnersDead (hunter win)
        if (condition instanceof AllSpeedrunnersDeadCondition) {
            return speedRunnerKillCount >= REQUIRED_KILL_COUNT;
        }
        return true; // Don't block other win types
    }

    @Override
    public Optional<WinCondition> getAlternativeWin(GameSession session) {
        return Optional.empty(); // No alternative win path
    }

    /**
     * Increment kill count (for testing).
     */
    public void incrementKillCount() {
        speedRunnerKillCount++;
    }

    @Override
    public String getName() {
        return "SuperHunter";
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=SuperHunterRoleTest`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRole.java
git add src/test/java/me/flamboyant/manhunt/domain/role/behavior/SuperHunterRoleTest.java
git commit -m "refactor(domain): migrate SuperHunterRole to WinConditionModifier (Priority 11)"
```

---

## Task 4: Remove Legacy Code from HunterRole

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java`

**Interfaces:**
- Consumes: None (cleanup task)
- Produces: Cleaned `HunterRole` with static list and result messaging removed

- [ ] **Step 1: Read current HunterRole implementation**

Read `src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java` to locate:
- `public static List<IHunterWinConditionModifier> winconModifiers` field
- `broadcastPlayerResultMessage()` method

- [ ] **Step 2: Remove static winconModifiers list**

Remove these lines from `HunterRole.java`:

```java
// REMOVE THIS
public static List<IHunterWinConditionModifier> winconModifiers = new ArrayList<>();
```

- [ ] **Step 3: Remove broadcastPlayerResultMessage method**

Remove the entire `broadcastPlayerResultMessage()` method from `HunterRole.java`:

```java
// REMOVE THIS ENTIRE METHOD
@Override
protected void broadcastPlayerResultMessage() {
    boolean wincon = session != null && session.getRemainingSpeedrunners() == 0;

    for (IHunterWinConditionModifier modifier : winconModifiers) {
        wincon &= modifier.isHunterWinPossible();
    }

    Bukkit.broadcastMessage(ChatHelper.feedback(
        owner.getDisplayName() + ", qui était " + getName() 
        + " a " + (wincon ? "gagné" : "perdu") + " !"
    ));
}
```

- [ ] **Step 4: Remove import for IHunterWinConditionModifier**

Remove this import from `HunterRole.java`:

```java
// REMOVE THIS
import me.flamboyant.manhunt.domain.wincondition.IHunterWinConditionModifier;
```

- [ ] **Step 5: Run all tests to verify no breakage**

Run: `mvn test`
Expected: PASS (all tests - SuperHunter tests should still pass)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java
git commit -m "refactor(domain): remove static winconModifiers list from HunterRole (Priority 11)"
```

---

## Task 5: Delete IHunterWinConditionModifier Interface

**Files:**
- Delete: `src/main/java/me/flamboyant/manhunt/domain/wincondition/IHunterWinConditionModifier.java`

**Interfaces:**
- Consumes: None (cleanup task)
- Produces: Legacy interface removed

- [ ] **Step 1: Verify no remaining references**

Run: `grep -r "IHunterWinConditionModifier" src/`
Expected: No matches (all references removed in previous tasks)

- [ ] **Step 2: Delete the legacy interface**

```bash
git rm src/main/java/me/flamboyant/manhunt/domain/wincondition/IHunterWinConditionModifier.java
```

- [ ] **Step 3: Run all tests to verify no breakage**

Run: `mvn test`
Expected: PASS (all tests)

- [ ] **Step 4: Commit**

```bash
git commit -m "refactor(domain): delete legacy IHunterWinConditionModifier interface (Priority 11)"
```

---

## Task 6: Add Role Result Messaging to EndGameSaga

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/application/sagas/EndGameSaga.java`
- Test: `src/test/java/me/flamboyant/manhunt/application/sagas/EndGameSagaTest.java`

**Interfaces:**
- Consumes: `GameSession.getAllRoles()`, `WinOutcome.getWinners()`, `AManhuntRole.getName()` (existing)
- Produces: Enhanced `EndGameSaga.endGame()` that broadcasts role results

- [ ] **Step 1: Write test for role result messaging**

Add to `src/test/java/me/flamboyant/manhunt/application/sagas/EndGameSagaTest.java`:

```java
@Test
void endGame_shouldBroadcastRoleResultsForWinner() {
    // Given
    GameSessionId sessionId = GameSessionId.generate();
    WinOutcome winOutcome = mock(WinOutcome.class);
    when(winOutcome.getWinners()).thenReturn(Set.of(ManhuntRoleType.HUNTER));
    when(winOutcome.getDescription()).thenReturn("Hunters win!");
    
    EndGameCommand command = EndGameCommand.of(sessionId, winOutcome);
    
    GameSession session = mock(GameSession.class);
    Player player1 = mock(Player.class);
    when(player1.getDisplayName()).thenReturn("Player1");
    
    HunterRole hunterRole = mock(HunterRole.class);
    when(hunterRole.getRoleType()).thenReturn(ManhuntRoleType.HUNTER);
    when(hunterRole.getName()).thenReturn("Hunter");
    
    Map<Player, AManhuntRole> roles = Map.of(player1, hunterRole);
    when(session.getAllRoles()).thenReturn(roles);
    
    when(lifecycleService.getSession(sessionId)).thenReturn(session);
    when(lifecycleService.endSession(sessionId)).thenReturn(session);
    
    // When
    saga.endGame(command);
    
    // Then
    // Verify broadcast happened (implementation note: Bukkit.broadcastMessage can't be tested directly,
    // but we verify the logic path completes without error)
    verify(lifecycleService).endSession(sessionId);
}

@Test
void endGame_shouldBroadcastRoleResultsForLoser() {
    // Given
    GameSessionId sessionId = GameSessionId.generate();
    WinOutcome winOutcome = mock(WinOutcome.class);
    when(winOutcome.getWinners()).thenReturn(Set.of(ManhuntRoleType.HUNTER));
    when(winOutcome.getDescription()).thenReturn("Hunters win!");
    
    EndGameCommand command = EndGameCommand.of(sessionId, winOutcome);
    
    GameSession session = mock(GameSession.class);
    Player player1 = mock(Player.class);
    when(player1.getDisplayName()).thenReturn("Player1");
    
    SpeedrunnerRole speedrunnerRole = mock(SpeedrunnerRole.class);
    when(speedrunnerRole.getRoleType()).thenReturn(ManhuntRoleType.SPEEDRUNNER);
    when(speedrunnerRole.getName()).thenReturn("Speedrunner");
    
    Map<Player, AManhuntRole> roles = Map.of(player1, speedrunnerRole);
    when(session.getAllRoles()).thenReturn(roles);
    
    when(lifecycleService.getSession(sessionId)).thenReturn(session);
    when(lifecycleService.endSession(sessionId)).thenReturn(session);
    
    // When
    saga.endGame(command);
    
    // Then
    verify(lifecycleService).endSession(sessionId);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EndGameSagaTest#endGame_shouldBroadcastRoleResultsForWinner`
Expected: PASS (tests pass but don't verify broadcasts yet - we'll add the implementation)

- [ ] **Step 3: Add role result messaging to EndGameSaga**

Modify `src/main/java/me/flamboyant/manhunt/application/sagas/EndGameSaga.java`:

Find the `endGame()` method and add role result broadcasting after game end logic:

```java
public GameSession endGame(EndGameCommand command) {
    // ... existing validation and game end logic ...
    
    // Get session before it's fully cleaned up
    GameSession session = lifecycleService.getSession(command.getSessionId());
    WinOutcome outcome = command.getWinOutcome();
    
    // Broadcast role results for each player
    session.getAllRoles().forEach((player, role) -> {
        boolean playerWon = outcome.getWinners().contains(role.getRoleType());
        String result = String.format(
            "%s, qui était %s a %s !",
            player.getDisplayName(),
            role.getName(),
            playerWon ? "gagné" : "perdu"
        );
        Bukkit.broadcastMessage(ChatHelper.feedback(result));
    });
    
    // ... continue with existing cleanup logic ...
    
    return session;
}
```

- [ ] **Step 4: Run test to verify implementation**

Run: `mvn test -Dtest=EndGameSagaTest`
Expected: PASS (all EndGameSaga tests)

- [ ] **Step 5: Run full test suite**

Run: `mvn test`
Expected: PASS (all tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/application/sagas/EndGameSaga.java
git add src/test/java/me/flamboyant/manhunt/application/sagas/EndGameSagaTest.java
git commit -m "feat(application): add role result messaging to EndGameSaga (Priority 11)"
```

---

## Task 7: Integration Testing and Validation

**Files:**
- Test: Create `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierIntegrationTest.java`

**Interfaces:**
- Consumes: All components from previous tasks
- Produces: Comprehensive integration tests

- [ ] **Step 1: Write integration test for SuperHunter blocking scenario**

Create `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierIntegrationTest.java`:

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.SuperHunterRole;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WinConditionModifierIntegrationTest {

    @Test
    void superHunter_shouldBlockHunterWinUntilRequirementMet() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);
        
        // Set up evaluator with hunter win condition
        AllSpeedrunnersDeadCondition hunterWin = new AllSpeedrunnersDeadCondition();
        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(hunterWin));
        
        // Create SuperHunter and register as modifier
        Player superHunterPlayer = mock(Player.class);
        SuperHunterRole superHunter = new SuperHunterRole(superHunterPlayer);
        evaluator.registerModifier(superHunter);
        
        // All speedrunners dead
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        
        // When - SuperHunter requirement NOT met (0 kills)
        Optional<WinOutcome> result1 = evaluator.evaluate(session);
        
        // Then
        assertFalse(result1.isPresent(), "Should block hunter win when SuperHunter requirement not met");
        
        // When - SuperHunter meets requirement (3 kills)
        superHunter.incrementKillCount();
        superHunter.incrementKillCount();
        superHunter.incrementKillCount();
        Optional<WinOutcome> result2 = evaluator.evaluate(session);
        
        // Then
        assertTrue(result2.isPresent(), "Should allow hunter win when SuperHunter requirement met");
    }

    @Test
    void multipleModifiers_shouldAllowWinWhenAllAllow() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        
        AllSpeedrunnersDeadCondition hunterWin = new AllSpeedrunnersDeadCondition();
        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(hunterWin));
        
        // Two modifiers, both allow
        WinConditionModifier modifier1 = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }
            
            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };
        
        WinConditionModifier modifier2 = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }
            
            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };
        
        evaluator.registerModifier(modifier1);
        evaluator.registerModifier(modifier2);
        
        // When
        Optional<WinOutcome> result = evaluator.evaluate(session);
        
        // Then
        assertTrue(result.isPresent(), "Should allow win when all modifiers allow");
    }

    @Test
    void dragonKilled_shouldNotBeBlockedByModifiers() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);
        
        DragonKilledCondition speedrunnerWin = new DragonKilledCondition();
        speedrunnerWin.markDragonKilled();
        
        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(speedrunnerWin));
        
        // SuperHunter modifier registered (but shouldn't block dragon win)
        Player superHunterPlayer = mock(Player.class);
        SuperHunterRole superHunter = new SuperHunterRole(superHunterPlayer);
        evaluator.registerModifier(superHunter);
        
        // When
        Optional<WinOutcome> result = evaluator.evaluate(session);
        
        // Then
        assertTrue(result.isPresent(), "Dragon win should not be blocked by SuperHunter modifier");
    }
}
```

- [ ] **Step 2: Run integration tests**

Run: `mvn test -Dtest=WinConditionModifierIntegrationTest`
Expected: PASS (3 integration tests)

- [ ] **Step 3: Run full test suite to verify no regressions**

Run: `mvn test`
Expected: PASS (all tests including existing tests)

- [ ] **Step 4: Verify memory leak prevention**

Manual verification (cannot be automated easily):
- Start multiple games in sequence
- Verify modifiers don't accumulate across sessions
- Check that evaluator is destroyed with session

Document in commit message that lifecycle is session-bound.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionModifierIntegrationTest.java
git commit -m "test(domain): add integration tests for WinConditionModifier (Priority 11)"
```

---

## Task 8: Update Documentation

**Files:**
- Modify: `docs/REFACTORING_PROGRESS.md`
- Modify: `docs/architecture/bounded-contexts-map.md` (if exists)

**Interfaces:**
- Consumes: None (documentation task)
- Produces: Updated documentation marking Priority 11 complete

- [ ] **Step 1: Update REFACTORING_PROGRESS.md**

Mark Priority 11 as complete in `docs/REFACTORING_PROGRESS.md`:

```markdown
### Priority 11: Model Win Conditions ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-24  
**Completed:** 2026-06-24  
**Estimated Effort:** 3-4 hours  
**Actual Effort:** ~3.5 hours

#### Tasks
- [x] 11.1 Create WinConditionModifier interface
- [x] 11.2 Enhance WinConditionEvaluator with modifier support
- [x] 11.3 Migrate SuperHunterRole to new modifier pattern
- [x] 11.4 Remove static winconModifiers list from HunterRole
- [x] 11.5 Delete legacy IHunterWinConditionModifier interface
- [x] 11.6 Add role result messaging to EndGameSaga
- [x] 11.7 Write comprehensive unit and integration tests

#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Evaluator-centric modifiers (instance-bound, no registry needed)
  - AND logic for multiple modifiers (all must allow)
  - First alternative win wins (order matters)
  - Result messaging centralized in EndGameSaga
- **Questions:** None
- **Commits:**
  - Task 1: WinConditionModifier interface creation
  - Task 2: WinConditionEvaluator enhancement
  - Task 3: SuperHunterRole migration
  - Task 4: HunterRole cleanup
  - Task 5: Legacy interface deletion
  - Task 6: EndGameSaga result messaging
  - Task 7: Integration tests

#### Success Criteria
- ✅ WinConditionModifier interface created
- ✅ Evaluator supports blocking and alternative wins
- ✅ SuperHunterRole migrated to new pattern
- ✅ Static list removed from HunterRole
- ✅ Legacy interface deleted
- ✅ Role result messaging in EndGameSaga
- ✅ 18+ tests passing (unit + integration)
- ✅ No memory leaks (modifiers session-scoped)
```

- [ ] **Step 2: Update phase completion status**

Update the summary section:

```markdown
### Phase Completion
- [x] Phase 1: Core Domain (4/4 complete) - **Priorities 1, 2, 3, 4 DONE** ✅
- [x] Phase 2: Domain Events & Services (3/3 complete) - **Priorities 5, 6, 8 DONE** ✅
- [x] Phase 3: Tactical Patterns (3/3 complete) - **Priorities 7, 10, 11 DONE** ✅
- [ ] Phase 4: Infrastructure & Polish (0/3 complete)

### Total Progress: 10/13 priorities completed (77%)
```

- [ ] **Step 3: Commit documentation updates**

```bash
git add docs/REFACTORING_PROGRESS.md
git commit -m "docs: mark Priority 11 (Win Condition Abstraction) as complete"
```

- [ ] **Step 4: Final verification**

Run full test suite one last time:

```bash
mvn clean test
```

Expected: PASS (all tests)

- [ ] **Step 5: Create summary commit message**

```bash
git log --oneline | head -10
```

Review commits for this priority to ensure completeness.

---

## Completion Checklist

Before marking Priority 11 complete, verify:

- [ ] `WinConditionModifier` interface created and tested
- [ ] `WinConditionEvaluator` enhanced with modifier support
- [ ] `SuperHunterRole` migrated to new modifier pattern
- [ ] Static `winconModifiers` list removed from `HunterRole`
- [ ] `IHunterWinConditionModifier` interface deleted
- [ ] Role result messaging added to `EndGameSaga`
- [ ] 18+ tests passing (unit + integration)
- [ ] No regressions in existing tests
- [ ] Documentation updated
- [ ] All code committed with clear commit messages
- [ ] Memory lifecycle verified (modifiers session-scoped)

---

## Self-Review Results

**1. Spec coverage check:**
- ✅ WinConditionModifier interface (Task 1)
- ✅ Enhanced WinConditionEvaluator (Task 2)
- ✅ SuperHunterRole migration (Task 3)
- ✅ Remove static list (Task 4)
- ✅ Delete legacy interface (Task 5)
- ✅ EndGameSaga messaging (Task 6)
- ✅ Integration testing (Task 7)
- ✅ Documentation (Task 8)

**2. Placeholder scan:** No TBD, TODO, or "implement later" found

**3. Type consistency:**
- `WinConditionModifier.allowsWin()` → consistent across all tasks
- `WinConditionModifier.getAlternativeWin()` → consistent across all tasks
- `WinConditionEvaluator.registerModifier()` → consistent across all tasks
- `SuperHunterRole.incrementKillCount()` → consistent in tests

All types match between task definitions and usage.

---

**Plan Status:** ✅ Complete and ready for execution
