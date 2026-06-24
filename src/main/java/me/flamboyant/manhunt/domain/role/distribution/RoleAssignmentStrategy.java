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
