package me.flamboyant.manhunt.roles;

import me.flamboyant.configurable.parameters.EnumParameter;
import me.flamboyant.utils.Common;
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

    public boolean setRandomRolesToEmpty(HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playersParameter, int wantedSpeedrunnerCount, int wantedAllyCount, boolean specialOnly) {
        if (wantedAllyCount + wantedSpeedrunnerCount > playersParameter.size()) return false;

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
        wantedAllyCount = wantedAllyCount == 0 ? diceAllyCount(playersParameter.size(), wantedSpeedrunnerCount) : wantedAllyCount;

        if (speedrunnerCount > wantedSpeedrunnerCount || allyCount > wantedAllyCount) {
            berzerkMode = true;
        }
        else {
            wantedSpeedrunnerCount -= speedrunnerCount;
            wantedAllyCount -= allyCount;
        }

        boolean finalBerzerkMode = berzerkMode;
        List<Player> playersToUpdate = playersParameter.keySet().stream().filter(p -> playersParameter.get(p).getSelectedValue() == null || finalBerzerkMode).collect(Collectors.toList());
        distributeRoles(playersToUpdate, playersParameter, wantedSpeedrunnerCount, wantedAllyCount, specialOnly);

        return true;
    }

    private void distributeRoles(List<Player> players, HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playersParameter, int wantedSpeedrunnerCount, int wantedAllyCount, boolean specialOnly) {
        List<ManhuntRoleIdentifier> speedrunnerTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE).collect(Collectors.toList());
        List<ManhuntRoleIdentifier> allyTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> v.toString().contains("ALLY")).collect(Collectors.toList());
        List<ManhuntRoleIdentifier> specialOpponentTypes = Arrays.stream(ManhuntRoleIdentifier.values()).filter(v -> !v.toString().contains("ALLY") && !v.toString().contains("SPEEDRUNNER") && v != ManhuntRoleIdentifier.HUNTER_SIMPLE).collect(Collectors.toList());
        for (Player player : shufflePlayers(players)) {
            ManhuntRoleIdentifier roleId;

            if (wantedSpeedrunnerCount > 0) {
                if (Common.rng.nextInt(100) < 30 && !specialOnly)
                    roleId = ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE;
                else
                    roleId = speedrunnerTypes.get(Common.rng.nextInt(speedrunnerTypes.size()));
                wantedSpeedrunnerCount--;
            }
            else if (wantedAllyCount > 0) {
                roleId = allyTypes.get(Common.rng.nextInt(allyTypes.size()));
                wantedAllyCount--;
            }
            else {
                if (Common.rng.nextBoolean() && !specialOnly)
                    roleId = ManhuntRoleIdentifier.HUNTER_SIMPLE;
                else
                    roleId = specialOpponentTypes.get(Common.rng.nextInt(specialOpponentTypes.size()));
            }

            playersParameter.get(player).setSelectedValue(roleId);
        }
    }

    private int diceAllyCount(int playerCount, int speedrunnerCount) {
        int possibleAllies = (playerCount - speedrunnerCount) / 4;
        if (possibleAllies == 0) return 0;

        int roll = Common.rng.nextInt(100);
        if (roll < 90) return 0;
        return Common.rng.nextInt(possibleAllies) + 1;
    }

    private int diceSpeedrunnerCount(int playerCount) {
        int possibleBonus = playerCount / 6;

        int roll = Common.rng.nextInt(100);
        if (roll < 95) return 1;
        return Common.rng.nextInt(possibleBonus) + 2;
    }

    private List<Player> shufflePlayers(List<Player> players) {
        List<Player> res = new ArrayList<>(players.size());
        List<Integer> availableIndex = new ArrayList<>();

        for (int i = 0; i < players.size(); i++) {
            availableIndex.add(i);
        }

        for (Player p : players) {
            int indexIndex = Common.rng.nextInt(availableIndex.size());
            res.set(availableIndex.get(indexIndex), p);
            availableIndex.remove(indexIndex);
        }

        return res;
    }
}
