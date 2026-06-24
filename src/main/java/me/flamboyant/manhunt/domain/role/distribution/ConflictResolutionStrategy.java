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
