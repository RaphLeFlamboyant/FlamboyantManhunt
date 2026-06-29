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
