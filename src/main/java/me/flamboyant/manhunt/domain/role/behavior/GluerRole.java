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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class GluerRole extends HunterRole {
    private BukkitTask checkProximityTask;
    private int totalChecks = 0;
    private int validChecks = 0;

    @Inject
    public GluerRole(
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
        Bukkit.getScheduler().cancelTask(checkProximityTask.getTaskId());
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        checkProximityTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
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
        Bukkit.broadcastMessage(messageService.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (totalChecks / validChecks < 2 ? "gagné" : "perdu") + " !"));
    }

    @Override
    public String getName() {
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

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.NEUTRAL_GLUER;
    }
}
