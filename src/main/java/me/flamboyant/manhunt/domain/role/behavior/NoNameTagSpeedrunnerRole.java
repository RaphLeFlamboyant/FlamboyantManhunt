package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class NoNameTagSpeedrunnerRole extends SpeedrunnerRole {
    private Team team;

    @Inject
    public NoNameTagSpeedrunnerRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner, server, plugin, messageService, itemService, eventRegistration);
    }

    @Override
    protected boolean doStart() {
        // TODO : clean teams when game starts
        for (Team t : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            t.unregister();
        }

        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        HashSet<Player> speedrunners = new HashSet<>();
        if (session != null) {
            for (Player player : session.getPlayers()) {
                AManhuntRole role = session.getRole(player);
                if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                    speedrunners.add(player);
                }
            }
        }

        team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Speedrunners");
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);

        for (Player p : speedrunners) {
            team.addEntry(p.getName());
        }

        Team opponentsTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Opponents");

        for (Player p : server.getOnlinePlayers()) {
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
    public String getName() {
        return "No Nametag Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "Les hunters ne peuvent pas voir les nametags des speedrunners.";
    }

}
