package me.flamboyant.manhunt;

import me.flamboyant.utils.Common;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class NewManhuntManager implements Listener {
    private static NewManhuntManager instance;
    public static NewManhuntManager getInstance()
    {
        if (instance == null)
        {
            instance = new NewManhuntManager();
        }

        return instance;
    }

    protected NewManhuntManager()
    {
    }

    public boolean startGame(int roleRevealDelayInMinutes, boolean speedrunnerSurprise) {
        GameData.remainingSpeedrunner = 0;

        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
            for (Player player : GameData.playerClassList.keySet()) {
                AManhuntRole role = GameData.playerClassList.get(player);
                if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                    GameData.remainingSpeedrunner++;
                }
                role.start();
            }

            if (speedrunnerSurprise)
                Common.server.getPluginManager().registerEvents(this, Common.plugin);
        }, (roleRevealDelayInMinutes * 60 + 1) * 20);

        if (!speedrunnerSurprise)
            Common.server.getPluginManager().registerEvents(this, Common.plugin);

        return true;
    }

    public void stopGame(String reason) {
        PlayerDeathEvent.getHandlerList().unregister(this);
        Bukkit.broadcastMessage(reason);

        for (AManhuntRole role : GameData.playerClassList.values()) {
            role.stop();
        }

        NewManhuntLauncher.getInstance().stop();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        if (GameData.playerClassList.get(event.getEntity()).getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            if (--GameData.remainingSpeedrunner == 0) {
                Bukkit.getScheduler().runTaskLater(Common.plugin,
                        () -> stopGame("L'équipe SPEEDRUNNER a perdu !!!"),
                        1);
                return;
            }
        }
    }
}
