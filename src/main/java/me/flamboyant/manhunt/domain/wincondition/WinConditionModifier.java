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
