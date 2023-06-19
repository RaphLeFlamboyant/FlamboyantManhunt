package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class GluerRole extends HunterRole {
    private BukkitTask checkProximityTask;
    private int totalChecks = 0;
    private int validChecks = 0;

    public GluerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(checkProximityTask.getTaskId());
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        checkProximityTask = Bukkit.getScheduler().runTaskTimer(Common.plugin, () -> {
            totalChecks++;
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                if (p.getWorld() == owner.getWorld() && p.getLocation().distance(owner.getLocation()) < 50) {
                    validChecks++;
                    break;
                }
            }
        }, 20, 20);
        return super.doStart();
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (totalChecks / validChecks < 2 ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected String getName() {
        return "Pot de Colle";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes si tu as passé plus de la moitié de la partie à moins de 50 blocs d'un joueur";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.NEUTRAL;
    }
}
