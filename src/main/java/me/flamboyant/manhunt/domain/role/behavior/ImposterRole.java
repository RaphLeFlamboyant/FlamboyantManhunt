package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

public class ImposterRole extends HunterRole {
    private static boolean winconMet;

    @Inject
    public ImposterRole(
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
        Bukkit.broadcastMessage(messageService.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (winconMet ? "gagné" : "perdu") + " !"));
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
