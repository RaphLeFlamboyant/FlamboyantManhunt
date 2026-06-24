package me.flamboyant.manhunt.domain.game;

/**
 * Represents the current phase of a manhunt game session.
 */
public enum GamePhase {
    /**
     * Preparation phase - players gather resources without manhunt mechanics.
     * Roles are assigned but not active. No tracking, no role abilities.
     */
    PREPARATION,

    /**
     * Active phase - manhunt is in progress.
     * Roles are active, tracking enabled, all mechanics functional.
     * Game continues until dragon dies or all speedrunners die.
     */
    ACTIVE
}
