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
