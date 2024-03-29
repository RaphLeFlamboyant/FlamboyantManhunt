package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class NoNameTagSpeedrunnerRole extends SpeedrunnerRole {
    private Team team;
    public NoNameTagSpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        // TODO : clean teams when game starts
        for (Team t : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            t.unregister();
        }

        HashSet<Player> speedrunners = new HashSet<>(GameData.playerClassList.keySet().stream().filter(r -> GameData.playerClassList.get(r).getRoleType() == ManhuntRoleType.SPEEDRUNNER).collect(Collectors.toList()));

        team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Speedrunners");
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);

        for (Player p : speedrunners) {
            team.addEntry(p.getName());
        }

        Team opponentsTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Opponents");

        for (Player p : Common.server.getOnlinePlayers()) {
            opponentsTeam.addEntry(p.getName());
        }

        return super.doStart();
    }

    @Override
    protected boolean doStop() {
        team.unregister();

        return super.doStop();
    }

    @Override
    protected String getName() {
        return "No Nametag Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "Les hunters ne peuvent pas voir les nametags des speedrunners.";
    }

}
