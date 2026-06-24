package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.commands.DistributeRolesCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Random;

/**
 * Service for distributing roles to players.
 * Refactored to use strategy pattern (Priority 10).
 */
@Singleton
public class RoleDistributionService {
    private final DomainEventPublisher eventPublisher;
    private final RoleCountStrategy countStrategy;
    private final ConflictResolutionStrategy conflictStrategy;
    private final RoleAssignmentStrategy assignmentStrategy;
    private final Random rng;

    @Inject
    public RoleDistributionService(
        DomainEventPublisher eventPublisher,
        RoleCountStrategy countStrategy,
        ConflictResolutionStrategy conflictStrategy,
        RoleAssignmentStrategy assignmentStrategy,
        Random rng
    ) {
        this.eventPublisher = eventPublisher;
        this.countStrategy = countStrategy;
        this.conflictStrategy = conflictStrategy;
        this.assignmentStrategy = assignmentStrategy;
        this.rng = rng;
    }

    /**
     * Distributes roles to players using strategy pattern.
     *
     * @param command Role distribution parameters
     * @return Map of player to role identifier assignments
     */
    public Map<Player, ManhuntRoleIdentifier> distributeRoles(DistributeRolesCommand command) {
        // 1. Build config from command
        RoleDistributionConfig config = buildConfigFromCommand(command);

        // 2. Determine desired counts
        RoleCounts desiredCounts = countStrategy.determineRoleCounts(
            command.getPlayers().size(),
            config
        );

        // 3. Resolve conflicts with fixed assignments
        ConflictResolution resolution = conflictStrategy.resolveConflicts(
            command.getFixedAssignments(),
            desiredCounts,
            command.getPlayers()
        );

        // 4. Assign specific roles
        Map<Player, ManhuntRoleIdentifier> assignments = assignmentStrategy.assignRoles(
            resolution,
            config,
            rng
        );

        // 5. Validate and log
        validateAssignments(assignments, command.getPlayers());
        logDistributionSummary(assignments);

        // 6. Publish event
        eventPublisher.publish(new RolesDistributedEvent(command.getSessionId(), assignments));

        return assignments;
    }

    private RoleDistributionConfig buildConfigFromCommand(DistributeRolesCommand command) {
        // Use balanced config as default
        // Command fields will be added in next task
        return RoleDistributionConfig.balanced();
    }

    private void validateAssignments(Map<Player, ManhuntRoleIdentifier> assignments, List<Player> players) {
        if (assignments.size() != players.size()) {
            throw new RoleDistributionException(
                "Assignment count mismatch: expected " + players.size() + ", got " + assignments.size()
            );
        }

        for (Player player : players) {
            if (!assignments.containsKey(player)) {
                throw new RoleDistributionException("Player missing assignment: " + player.getName());
            }
        }
    }

    private void logDistributionSummary(Map<Player, ManhuntRoleIdentifier> assignments) {
        long speedrunners = assignments.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .count();
        long allies = assignments.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.ALLY)
            .count();
        long hunters = assignments.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.HUNTER)
            .count();

        Bukkit.getLogger().info(
            "Role distribution: " + speedrunners + " speedrunners, " +
            allies + " allies, " + hunters + " hunters"
        );
    }
}
