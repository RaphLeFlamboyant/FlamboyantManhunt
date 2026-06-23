package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WinConditionTest {

    @Test
    void shouldCreateWinOutcomeFromCondition() {
        // Given
        GameSession session = mock(GameSession.class);
        WinCondition condition = new WinCondition() {
            @Override
            public boolean isMet(GameSession session) {
                return true;
            }

            @Override
            public Set<ManhuntRoleType> getWinners() {
                return Set.of(ManhuntRoleType.HUNTER);
            }

            @Override
            public String getDescription() {
                return "Test win condition";
            }
        };

        // When
        WinOutcome outcome = WinOutcome.of(condition);

        // Then
        assertNotNull(outcome);
        assertTrue(outcome.getWinners().contains(ManhuntRoleType.HUNTER));
        assertEquals("Test win condition", outcome.getDescription());
    }

    @Test
    void shouldBeImmutable() {
        // Given
        WinCondition condition = new WinCondition() {
            @Override
            public boolean isMet(GameSession session) { return true; }
            @Override
            public Set<ManhuntRoleType> getWinners() { return Set.of(ManhuntRoleType.HUNTER); }
            @Override
            public String getDescription() { return "Test"; }
        };

        // When
        WinOutcome outcome = WinOutcome.of(condition);
        Set<ManhuntRoleType> winners = outcome.getWinners();

        // Then - attempting to modify should fail
        assertThrows(UnsupportedOperationException.class, () -> {
            winners.add(ManhuntRoleType.SPEEDRUNNER);
        });
    }

    // --- AllSpeedrunnersDeadCondition Tests ---

    @Test
    void allSpeedrunnersDeadCondition_shouldBeMet_whenNoSpeedrunnersRemain() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertTrue(isMet);
        assertTrue(condition.getWinners().contains(ManhuntRoleType.HUNTER));
        assertFalse(condition.getDescription().isEmpty());
    }

    @Test
    void allSpeedrunnersDeadCondition_shouldNotBeMet_whenSpeedrunnersRemain() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(2);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertFalse(isMet);
    }

    @Test
    void allSpeedrunnersDeadCondition_shouldHandleEdgeCase_whenNegativeSpeedrunners() {
        // Given (shouldn't happen, but defensive)
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(-1);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then - treat negative as "all dead"
        assertTrue(isMet);
    }

    // --- DragonKilledCondition Tests ---

    @Test
    void dragonKilledCondition_shouldNotBeMet_initially() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertFalse(isMet);
    }

    @Test
    void dragonKilledCondition_shouldBeMet_afterDragonKilled() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();

        // When
        condition.markDragonKilled();
        boolean isMet = condition.isMet(session);

        // Then
        assertTrue(isMet);
        assertTrue(condition.getWinners().contains(ManhuntRoleType.SPEEDRUNNER));
        assertFalse(condition.getDescription().isEmpty());
    }

    @Test
    void dragonKilledCondition_shouldReset() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();
        condition.markDragonKilled();
        assertTrue(condition.isMet(session));

        // When
        condition.reset();

        // Then
        assertFalse(condition.isMet(session));
    }

    // --- WinConditionEvaluator Tests ---

    @Test
    void evaluator_shouldReturnEmpty_whenNoConditionsMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(3);

        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),
                new DragonKilledCondition()
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertFalse(outcome.isPresent());
    }

    @Test
    void evaluator_shouldReturnOutcome_whenConditionMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);

        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),
                new DragonKilledCondition()
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().getWinners().contains(ManhuntRoleType.HUNTER));
    }

    @Test
    void evaluator_shouldReturnFirstMetCondition_whenMultipleMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);

        DragonKilledCondition dragonCondition = new DragonKilledCondition();
        dragonCondition.markDragonKilled();

        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),  // First in list
                dragonCondition
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertTrue(outcome.isPresent());
        // Should be hunter win (first condition), not speedrunner
        assertTrue(outcome.get().getWinners().contains(ManhuntRoleType.HUNTER));
    }

    @Test
    void evaluator_shouldThrow_whenNullSession() {
        // Given
        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(new AllSpeedrunnersDeadCondition())
        );

        // When/Then
        assertThrows(NullPointerException.class, () -> {
            evaluator.evaluate(null);
        });
    }

    @Test
    void evaluator_shouldThrow_whenNullConditions() {
        // When/Then
        assertThrows(NullPointerException.class, () -> {
            new WinConditionEvaluator(null);
        });
    }
}
