package me.flamboyant.manhunt.roles;

import me.flamboyant.configurable.parameters.EnumParameter;
import me.flamboyant.manhunt.GameData;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GameRolesManagement {
    private static GameRolesManagement instance;
    public static GameRolesManagement getInstance()
    {
        if (instance == null)
        {
            instance = new GameRolesManagement();
        }

        return instance;
    }

    protected GameRolesManagement()
    {
    }

    public boolean setRandomRolesToEmpty(HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playersParameter,
                                         int wantedSpeedrunnerCount,
                                         int wantedAllyCount,
                                         boolean specialOnly) {
        if (wantedAllyCount + wantedSpeedrunnerCount > playersParameter.size()) {
            Bukkit.broadcastMessage(ChatHelper.errorMessage("Vous avez mis trop de speedrunners et d'alliés par rapport au nombre de joueurs"));
            wantedAllyCount = 0;
            wantedSpeedrunnerCount = 1;
        }

        int speedrunnerCount = 0;
        int allyCount = 0;
        boolean berzerkMode = false; // true = the list is shit and must be purified

        for (Player player : playersParameter.keySet()) {
            ManhuntRoleIdentifier roleId = playersParameter.get(player).getSelectedValue();
            if (roleId == null)
                continue;

            if (roleId.toString().contains("ALLY"))
                allyCount++;
            if (roleId.toString().contains("SPEEDRUNNER"))
                speedrunnerCount++;
        }

        wantedSpeedrunnerCount = wantedSpeedrunnerCount == 0 ? diceSpeedrunnerCount(playersParameter.size()) : wantedSpeedrunnerCount;
        Bukkit.getLogger().info("Wanted speedrunners : " + wantedSpeedrunnerCount);
        wantedAllyCount = wantedAllyCount == 0 ? diceAllyCount(playersParameter.size(), wantedSpeedrunnerCount) : wantedAllyCount;
        Bukkit.getLogger().info("Wanted ally : " + wantedAllyCount);

        if (speedrunnerCount > wantedSpeedrunnerCount || allyCount > wantedAllyCount) {
            Bukkit.broadcastMessage(ChatHelper.errorMessage("Le nombre de speedrunners et/ou d'alliés paramétrés par l'admin est trop important par rapport au nombre de speedrunners et/ou alliés voulus dans la partie"));
            berzerkMode = true;
        }
        else {
            wantedSpeedrunnerCount -= speedrunnerCount;
            wantedAllyCount -= allyCount;
        }

        boolean finalBerzerkMode = berzerkMode;
        List<Player> playersToUpdate = playersParameter.keySet().stream().filter(p -> playersParameter.get(p).getSelectedValue() == null || finalBerzerkMode).collect(Collectors.toList());
        distributeRoles(playersToUpdate, playersParameter, wantedSpeedrunnerCount, wantedAllyCount, specialOnly);

        for (Player p : playersParameter.keySet()) {
            Bukkit.getLogger().info(p.getDisplayName() + " - " + playersParameter.get(p).getSelectedValue().toString());
        }

        return true;
    }

    private void distributeRoles(List<Player> players, HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playersParameter, int wantedSpeedrunnerCount, int wantedAllyCount, boolean specialOnly) {
        Bukkit.getLogger().info("Distributing speedrunners : " + wantedSpeedrunnerCount);
        Bukkit.getLogger().info("Distributing allies : " + wantedAllyCount);
        List<ManhuntRoleIdentifier> speedrunnerTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE).collect(Collectors.toList());
        List<ManhuntRoleIdentifier> allyTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("ALLY")).collect(Collectors.toList());
        List<ManhuntRoleIdentifier> hunterTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("HUNTER") && v != ManhuntRoleIdentifier.HUNTER_SIMPLE).collect(Collectors.toList());
        List<ManhuntRoleIdentifier> soloTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("NEUTRAL")).collect(Collectors.toList());
        long distributedHunter = playersParameter.values().stream().filter((r) -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("HUNTER")).count();
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
            playersParameter.get(player).setSelectedValue(roleId);
        }
    }

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

    private int diceSpeedrunnerCount(int playerCount) {
        int possibleBonus = playerCount / 6;

        int roll = Common.rng.nextInt(100);
        Bukkit.broadcastMessage("Roll : " + roll);
        if (roll < 95) return 1;
        return Common.rng.nextInt(possibleBonus) + 2;
    }

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
}
