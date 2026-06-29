package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class UndecidedRole extends HunterRole {
    private BukkitTask changeTeamTask;
    private ManhuntRoleType currentTeam = ManhuntRoleType.ALLY;

    @Inject
    public UndecidedRole(
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
        Bukkit.getScheduler().cancelTask(changeTeamTask.getTaskId());
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        changeTeamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            currentTeam = currentTeam == ManhuntRoleType.ALLY ? ManhuntRoleType.HUNTER : ManhuntRoleType.ALLY;
            owner.sendMessage(messageService.importantMessage("Tu es passé dans le camp " + (currentTeam == ManhuntRoleType.ALLY ? "allié du speedrunner" : "hunter")));
        }, 15 * 60 * 20, 15 * 60 * 20);
        return super.doStart();
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        if (currentTeam == ManhuntRoleType.HUNTER) {
            super.broadcastPlayerResultMessage();
        } else {
            GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
            boolean won = session != null && session.getRemainingSpeedrunners() > 0;
            Bukkit.broadcastMessage(messageService.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (won ? "gagné" : "perdu") + " !"));
        }
    }

    @Override
    public String getName() {
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

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.NEUTRAL_UNDECIDED;
    }
}
