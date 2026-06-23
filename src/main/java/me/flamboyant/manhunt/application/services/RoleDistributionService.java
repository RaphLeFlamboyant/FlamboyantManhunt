package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.commands.DistributeRolesCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.RoleTypeRegistry;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for distributing roles to players.
 *
 * NOTE: This service contains the complex distribution algorithm
 * copied as-is from GameRolesManagement.distributeRoles() for Priority 6.
 * The algorithm will be refactored in Priority 10 (Extract Role Distribution Strategy).
 * See REFACTORING_PROGRESS.md Priority 10 for follow-up work.
 */
@Singleton
public class RoleDistributionService {
    private final DomainEventPublisher eventPublisher;

    @Inject
    public RoleDistributionService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Distributes roles to players based on configuration.
     * Preserves existing "berzerk mode", probabilities, special role selection.
     *
     * Algorithm copied from GameRolesManagement.distributeRoles() lines 80-174.
     *
     * @param command Role distribution parameters
     * @return Map of player to role identifier assignments
     */
    public Map<Player, ManhuntRoleIdentifier> distributeRoles(DistributeRolesCommand command) {
        Map<Player, ManhuntRoleIdentifier> assignments = performDistribution(command);

        eventPublisher.publish(new RolesDistributedEvent(command.getSessionId(), assignments));

        return assignments;
    }

    /**
     * Performs role distribution algorithm.
     * Copied from GameRolesManagement.distributeRoles() - preserved as-is for Priority 6.
     */
    private Map<Player, ManhuntRoleIdentifier> performDistribution(DistributeRolesCommand command) {
        Map<Player, ManhuntRoleIdentifier> assignments = new HashMap<>(command.getFixedAssignments());

        int wantedSpeedrunnerCount = command.getSpeedrunnerCount();
        int wantedAllyCount = command.getAllyCount();
        boolean specialOnly = command.isSpecialRolesOnly();

        // Validate counts
        if (wantedAllyCount + wantedSpeedrunnerCount > command.getPlayers().size()) {
            Bukkit.getLogger().warning("Too many speedrunners and allies for player count");
            wantedAllyCount = 0;
            wantedSpeedrunnerCount = 1;
        }

        // Count existing assignments
        int speedrunnerCount = 0;
        int allyCount = 0;
        boolean berzerkMode = false;

        for (Map.Entry<Player, ManhuntRoleIdentifier> entry : assignments.entrySet()) {
            ManhuntRoleIdentifier roleId = entry.getValue();
            if (roleId.getRoleType() == ManhuntRoleType.ALLY) {
                allyCount++;
            }
            if (roleId.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunnerCount++;
            }
        }

        // Adjust wanted counts
        wantedSpeedrunnerCount = wantedSpeedrunnerCount == 0
            ? diceSpeedrunnerCount(command.getPlayers().size())
            : wantedSpeedrunnerCount;
        Bukkit.getLogger().info("Wanted speedrunners : " + wantedSpeedrunnerCount);
        wantedAllyCount = wantedAllyCount == 0
            ? diceAllyCount(command.getPlayers().size(), wantedSpeedrunnerCount)
            : wantedAllyCount;
        Bukkit.getLogger().info("Wanted ally : " + wantedAllyCount);

        if (speedrunnerCount > wantedSpeedrunnerCount || allyCount > wantedAllyCount) {
            Bukkit.getLogger().warning("Too many fixed assignments, entering berzerk mode");
            berzerkMode = true;
        } else {
            wantedSpeedrunnerCount -= speedrunnerCount;
            wantedAllyCount -= allyCount;
        }

        // Get players needing assignment
        boolean finalBerzerkMode = berzerkMode;
        List<Player> playersToAssign = command.getPlayers().stream()
            .filter(p -> !assignments.containsKey(p) || finalBerzerkMode)
            .collect(Collectors.toList());

        // Distribute roles
        distributeRolesToPlayers(playersToAssign, assignments, wantedSpeedrunnerCount,
                                wantedAllyCount, specialOnly);

        for (Player p : command.getPlayers()) {
            if (assignments.containsKey(p)) {
                Bukkit.getLogger().info(p.getDisplayName() + " - " + assignments.get(p).toString());
            }
        }

        return assignments;
    }

    /**
     * Internal distribution logic - copied from GameRolesManagement lines 80-129.
     */
    private void distributeRolesToPlayers(List<Player> players,
                                         Map<Player, ManhuntRoleIdentifier> assignments,
                                         int wantedSpeedrunnerCount, int wantedAllyCount,
                                         boolean specialOnly) {
        Bukkit.getLogger().info("Distributing speedrunners : " + wantedSpeedrunnerCount);
        Bukkit.getLogger().info("Distributing allies : " + wantedAllyCount);

        List<ManhuntRoleIdentifier> speedrunnerTypes = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(ManhuntRoleType.SPEEDRUNNER,
                                                     ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE)
        );
        List<ManhuntRoleIdentifier> allyTypes = new ArrayList<>(
            RoleTypeRegistry.getRolesByType(ManhuntRoleType.ALLY)
        );
        List<ManhuntRoleIdentifier> hunterTypes = new ArrayList<>(
            RoleTypeRegistry.getRolesByTypeExcluding(ManhuntRoleType.HUNTER,
                                                     ManhuntRoleIdentifier.HUNTER_SIMPLE)
        );
        List<ManhuntRoleIdentifier> soloTypes = new ArrayList<>(
            RoleTypeRegistry.getRolesByType(ManhuntRoleType.NEUTRAL)
        );

        long distributedHunter = assignments.values().stream()
            .filter(r -> r.getRoleType() == ManhuntRoleType.HUNTER)
            .count();
        boolean speedrunnersHitSpecial = false;

        for (Player player : shufflePlayers(players)) {
            ManhuntRoleIdentifier roleId;

            Bukkit.getLogger().info("Role dice roll for " + player.getDisplayName());
            if (wantedSpeedrunnerCount > 0) {
                if ((Common.rng.nextInt(100) > 50 && !specialOnly) || speedrunnerTypes.size() == 0)
                    roleId = ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE;
                else {
                    speedrunnersHitSpecial = true;
                    roleId = speedrunnerTypes.get(Common.rng.nextInt(speedrunnerTypes.size()));
                    speedrunnerTypes.remove(roleId);
                }
                wantedSpeedrunnerCount--;
            }
            else if (wantedAllyCount > 0 && allyTypes.size() > 0) {
                roleId = allyTypes.get(Common.rng.nextInt(allyTypes.size()));
                allyTypes.remove(roleId);
                wantedAllyCount--;
            }
            else {
                if (distributedHunter != 0 && Common.rng.nextInt(100) < 20 && soloTypes.size() > 0) {
                    roleId = soloTypes.get(Common.rng.nextInt(soloTypes.size()));
                    soloTypes.remove(roleId);
                }
                else {
                    distributedHunter++;
                    if (!speedrunnersHitSpecial
                            || (Common.rng.nextInt(100) > 30 && !specialOnly)
                            || hunterTypes.size() == 0)
                        roleId = ManhuntRoleIdentifier.HUNTER_SIMPLE;
                    else {
                        roleId = hunterTypes.get(Common.rng.nextInt(hunterTypes.size()));
                        hunterTypes.remove(roleId);
                    }
                }
            }

            Bukkit.getLogger().info("- " + roleId.toString());
            assignments.put(player, roleId);
        }
    }

    /**
     * Shuffles player list - copied from GameRolesManagement lines 156-173.
     */
    private List<Player> shufflePlayers(List<Player> players) {
        List<Player> res = new ArrayList<>();
        List<Integer> availableIndex = new ArrayList<>();

        for (int i = 0; i < players.size(); i++) {
            availableIndex.add(i);
            res.add(null);
        }

        for (Player p : players) {
            int indexIndex = Common.rng.nextInt(availableIndex.size());
            Bukkit.getLogger().info("Selecting index " + indexIndex);
            res.set(availableIndex.get(indexIndex), p);
            availableIndex.remove(indexIndex);
        }

        return res;
    }

    /**
     * Dice speedrunner count - copied from GameRolesManagement lines 147-154.
     */
    private int diceSpeedrunnerCount(int playerCount) {
        int possibleBonus = playerCount / 6;

        int roll = Common.rng.nextInt(100);
        if (roll < 95) return 1;
        return Common.rng.nextInt(possibleBonus) + 2;
    }

    /**
     * Dice ally count - copied from GameRolesManagement lines 131-145.
     */
    private int diceAllyCount(int playerCount, int speedrunnerCount) {
        int possibleAllies = (playerCount - speedrunnerCount) / 4;
        if (possibleAllies == 0) return 0;

        int roll = Common.rng.nextInt(100);
        if (roll < 90) return 0;

        int effectiveAllies = 1;
        while (possibleAllies > 0) {
            if (roll < 20)
                effectiveAllies++;
        }

        return effectiveAllies;
    }
}
