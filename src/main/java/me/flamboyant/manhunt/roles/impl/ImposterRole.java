package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.common.utils.ChatColorUtils;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class ImposterRole extends HunterRole {
    private static boolean winconMet;

    public ImposterRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        Bukkit.broadcastMessage(ChatColorUtils.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (winconMet ? "gagné" : "perdu") + " !"));

        EntityDamageEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        winconMet = false;
        return super.doStart();
    }

    @Override
    protected String getName() {
        return "Imposteur";
    }

    @Override
    protected String getDescription() {
        return "Gagne si les speedrunners gagnent mais les hunters te pensent dans leur équipe !";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.ALLY;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0)
            winconMet = true;
    }
}
