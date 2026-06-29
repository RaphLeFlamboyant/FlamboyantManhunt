package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.RoleTypeRegistry;
import me.flamboyant.manhunt.domain.role.distribution.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ProbabilisticRoleAssignment implements RoleAssignmentStrategy {
    @Override
    public Map<Player, ManhuntRoleIdentifier> assignRoles(
        ConflictResolution resolution,
        RoleDistributionConfig config,
        Random rng
    ) {
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>(resolution.getFixedAssignments());
        RoleCounts counts = resolution.getAdjustedCounts();

        // Shuffle players for fairness
        List<Player> shuffled = shuffle(resolution.getPlayersNeedingAssignment(), rng);

        // Build role pools
        List<ManhuntRoleIdentifier> speedrunnerPool = buildRolePool(
            ManhuntRoleType.SPEEDRUNNER,
            counts.getSpeedrunners(),
            config,
            rng
        );
        List<ManhuntRoleIdentifier> allyPool = buildRolePool(
            ManhuntRoleType.ALLY,
            counts.getAllies(),
            config,
            rng
        );
        List<ManhuntRoleIdentifier> hunterPool = buildRolePool(
            ManhuntRoleType.HUNTER,
            counts.getHunters(),
            config,
            rng
        );

        // Assign roles by popping from pools
        int idx = 0;
        for (Player player : shuffled) {
            ManhuntRoleIdentifier role;
            if (idx < speedrunnerPool.size()) {
                role = speedrunnerPool.get(idx);
            } else if (idx - speedrunnerPool.size() < allyPool.size()) {
                role = allyPool.get(idx - speedrunnerPool.size());
            } else {
                role = hunterPool.get(idx - speedrunnerPool.size() - allyPool.size());
            }
            assignments.put(player, role);
            idx++;
        }

        return assignments;
    }

    private List<Player> shuffle(List<Player> players, Random rng) {
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, rng);
        return shuffled;
    }

    private List<ManhuntRoleIdentifier> buildRolePool(
        ManhuntRoleType type,
        int count,
        RoleDistributionConfig config,
        Random rng
    ) {
        if (config.isSpecialRolesOnly()) {
            return selectSpecialRoles(type, count, rng);
        } else {
            return selectMixedRoles(type, count, config.getSpecialRoleProbability(), rng);
        }
    }

    private List<ManhuntRoleIdentifier> selectSpecialRoles(
        ManhuntRoleType type,
        int count,
        Random rng
    ) {
        List<ManhuntRoleIdentifier> pool = new ArrayList<>();
        ManhuntRoleIdentifier simpleRole = getSimpleRole(type);
        List<ManhuntRoleIdentifier> specialRoles = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(type, simpleRole)
        );

        if (specialRoles.isEmpty()) {
            throw new InsufficientRolesException(
                "No special roles available for type: " + type
            );
        }

        for (int i = 0; i < count; i++) {
            if (specialRoles.isEmpty()) {
                // Ran out of special roles, use simple as fallback
                pool.add(simpleRole);
            } else {
                int idx = rng.nextInt(specialRoles.size());
                pool.add(specialRoles.remove(idx));
            }
        }

        return pool;
    }

    private List<ManhuntRoleIdentifier> selectMixedRoles(
        ManhuntRoleType type,
        int count,
        double specialProbability,
        Random rng
    ) {
        List<ManhuntRoleIdentifier> pool = new ArrayList<>();
        ManhuntRoleIdentifier simpleRole = getSimpleRole(type);
        List<ManhuntRoleIdentifier> specialRoles = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(type, simpleRole)
        );

        for (int i = 0; i < count; i++) {
            if (rng.nextDouble() < specialProbability && !specialRoles.isEmpty()) {
                // Pick random special role and remove from pool
                int idx = rng.nextInt(specialRoles.size());
                pool.add(specialRoles.remove(idx));
            } else {
                pool.add(simpleRole);
            }
        }

        return pool;
    }

    private ManhuntRoleIdentifier getSimpleRole(ManhuntRoleType type) {
        switch (type) {
            case SPEEDRUNNER:
                return ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE;
            case ALLY:
                return ManhuntRoleIdentifier.ALLY_SIMPLE;
            case HUNTER:
                return ManhuntRoleIdentifier.HUNTER_SIMPLE;
            case NEUTRAL:
                return ManhuntRoleIdentifier.NEUTRAL_SIMPLE;
            default:
                throw new IllegalArgumentException("Unknown role type: " + type);
        }
    }
}
