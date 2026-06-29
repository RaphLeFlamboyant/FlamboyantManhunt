package me.flamboyant.manhunt.domain.role.distribution;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Value object representing the result of conflict resolution between fixed role assignments
 * and role count requirements.
 * <p>
 * Immutable value object that encapsulates:
 * - Players who must receive specific roles (fixed assignments)
 * - Adjusted role counts after accounting for fixed assignments
 * - Players who still need role assignment
 */
public class ConflictResolution {
    private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;
    private final RoleCounts adjustedCounts;
    private final List<Player> playersNeedingAssignment;

    /**
     * Creates a new ConflictResolution instance.
     *
     * @param fixedAssignments Players who must receive specific roles
     * @param adjustedCounts Role counts adjusted after accounting for fixed assignments
     * @param playersNeedingAssignment Players who still need role assignment
     * @throws InvalidConfigurationException if any parameter is null or if a player appears
     *                                       in both fixed assignments and needing assignment
     */
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

    /**
     * Returns the map of fixed role assignments (players who must receive specific roles).
     *
     * @return Unmodifiable map of player to role assignments
     */
    public Map<Player, ManhuntRoleIdentifier> getFixedAssignments() {
        return fixedAssignments;
    }

    /**
     * Returns the adjusted role counts after accounting for fixed assignments.
     *
     * @return The adjusted role counts
     */
    public RoleCounts getAdjustedCounts() {
        return adjustedCounts;
    }

    /**
     * Returns the list of players who still need role assignment.
     *
     * @return Unmodifiable list of players needing assignment
     */
    public List<Player> getPlayersNeedingAssignment() {
        return playersNeedingAssignment;
    }
}
