package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Set;

/**
 * Represents a game victory condition.
 * Part of the Win Condition bounded context.
 */
public interface WinCondition {

    /**
     * Check if this win condition is met for the given game session.
     *
     * @param session The game session to evaluate
     * @return true if the condition is met and the game should end
     */
    boolean isMet(GameSession session);

    /**
     * Get the team(s) that win if this condition is met.
     *
     * @return Set of winning role types
     */
    Set<ManhuntRoleType> getWinners();

    /**
     * Get a human-readable description of this win condition.
     *
     * @return Description string (e.g., "Tous les speedrunners sont morts !")
     */
    String getDescription();
}
