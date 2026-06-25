package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.utils.ChatHelper;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
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
        // Manual unregistration removed - handled by GameLifecycleService
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        winconMet = false;
        return super.doStart();
    }
    @Override
    protected void broadcastPlayerResultMessage() {
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (winconMet ? "gagné" : "perdu") + " !"));
    }

    @Override
    public String getName() {
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

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.ALLY_IMPOSTER;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0)
            winconMet = true;
    }
}
