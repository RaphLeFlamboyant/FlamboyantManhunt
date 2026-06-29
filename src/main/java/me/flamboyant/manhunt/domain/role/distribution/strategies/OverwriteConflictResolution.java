package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.distribution.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class OverwriteConflictResolution implements ConflictResolutionStrategy {
    @Override
    public ConflictResolution resolveConflicts(
        Map<Player, ManhuntRoleIdentifier> fixedAssignments,
        RoleCounts desiredCounts,
        List<Player> allPlayers
    ) {
        // Count fixed assignments by type
        RoleCounts fixedCounts = countByType(fixedAssignments);

        // Check if conflict exists
        if (fixedCounts.exceedsAny(desiredCounts)) {
            // BERZERK MODE: Clear everything and start fresh
            Bukkit.getLogger().warning(
                "Fixed assignments conflict with desired counts. Overwriting all assignments."
            );
            return new ConflictResolution(
                Collections.emptyMap(),
                desiredCounts,
                allPlayers
            );
        }

        // No conflict: keep fixed, adjust counts down
        RoleCounts adjustedCounts = desiredCounts.subtract(fixedCounts);
        List<Player> needingAssignment = allPlayers.stream()
            .filter(p -> !fixedAssignments.containsKey(p))
            .collect(Collectors.toList());

        return new ConflictResolution(fixedAssignments, adjustedCounts, needingAssignment);
    }

    private RoleCounts countByType(Map<Player, ManhuntRoleIdentifier> assignments) {
        int speedrunners = 0;
        int allies = 0;
        int hunters = 0;
        int neutrals = 0;

        for (ManhuntRoleIdentifier roleId : assignments.values()) {
            ManhuntRoleType type = roleId.getRoleType();
            if (type == ManhuntRoleType.SPEEDRUNNER) {
                speedrunners++;
            } else if (type == ManhuntRoleType.ALLY) {
                allies++;
            } else if (type == ManhuntRoleType.HUNTER) {
                hunters++;
            } else if (type == ManhuntRoleType.NEUTRAL) {
                neutrals++;
            }
        }

        return new RoleCounts(speedrunners, allies, hunters, neutrals);
    }
}
