package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class UndecidedRole extends HunterRole {
    private BukkitTask changeTeamTask;
    private ManhuntRoleType currentTeam = ManhuntRoleType.ALLY;

    public UndecidedRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(changeTeamTask.getTaskId());
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        changeTeamTask = Bukkit.getScheduler().runTaskTimer(Common.plugin, () -> {
            currentTeam = currentTeam == ManhuntRoleType.ALLY ? ManhuntRoleType.HUNTER : ManhuntRoleType.ALLY;
        }, 15 * 60 * 20, 15 * 60 * 20);
        return super.doStart();
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        if (currentTeam == ManhuntRoleType.HUNTER)
            super.broadcastPlayerResultMessage();
        else
            Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (GameData.remainingSpeedrunner > 0 ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected String getName() {
        return "L'indécis";
    }

    @Override
    protected String getDescription() {
        return "Toute les 15 minutes tu changes de camp. " +
                "Quand la partie se termine, ce camp doit donc être celui qui gagne pour que tu gagnes aussi.";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.NEUTRAL;
    }
}
