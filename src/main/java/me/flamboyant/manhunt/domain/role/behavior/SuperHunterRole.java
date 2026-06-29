package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.WinCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionModifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public class SuperHunterRole extends HunterRole implements WinConditionModifier {
    private static final int REQUIRED_KILL_COUNT = 3;
    private int speedRunnerKillCount = 0;

    @Inject
    public SuperHunterRole(
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
    protected void broadcastPlayerResultMessage() {
        boolean wincon = isWinning();
        Bukkit.broadcastMessage(messageService.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (wincon ? "gagné" : "perdu") + " !"));
    }

    @Override
    public String getName() {
        return "Super Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand tu as tué tous les speedrunners de tes propres mains, tu voles alors la victoir aux hunters." +
                " Si un des speedrunners meurt d'une autre façon alors tu as perdu" +
                " Tu détiens une boussole qui te donne sa position.";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.SUPER_HUNTER;
    }

    @Override
    protected boolean doStart() {
        // Register as modifier with evaluator
        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(owner);
        if (session != null && session.getWinConditionEvaluator() != null) {
            session.getWinConditionEvaluator().registerModifier(this);
        }

        return super.doStart();
    }

    // No doStop() override needed - evaluator destroyed with session

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!damager.equals(owner)) return;

        GameSession session = GameSessionManager.getInstance()
            .getActiveSessionForPlayer(owner);
        if (session == null) return;

        AManhuntRole victimRole = session.getRole(victim);
        if (victimRole == null) return;

        if (victimRole.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                speedRunnerKillCount++;
                Bukkit.broadcastMessage(messageService.feedback(
                    owner.getDisplayName() + " (SuperHunter) a tué un speedrunner ! ("
                    + speedRunnerKillCount + "/" + REQUIRED_KILL_COUNT + ")"
                ));

                if (speedRunnerKillCount >= REQUIRED_KILL_COUNT) {
                    Bukkit.broadcastMessage(messageService.feedback(
                        "SuperHunter peut maintenant permettre la victoire des Hunters !"
                    ));
                }
            }
        }
    }

    @Override
    public boolean allowsWin(WinCondition condition, GameSession session) {
        // Only gate AllSpeedrunnersDead (hunter win)
        if (condition instanceof AllSpeedrunnersDeadCondition) {
            return speedRunnerKillCount >= REQUIRED_KILL_COUNT;
        }
        return true; // Don't block other win types
    }

    @Override
    public Optional<WinCondition> getAlternativeWin(GameSession session) {
        return Optional.empty(); // No alternative win path
    }

    /**
     * Increment kill count (for testing).
     */
    public void incrementKillCount() {
        speedRunnerKillCount++;
    }

    private boolean isWinning() {
        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session == null) return false;

        long speedrunnerCount = 0;
        for (Player player : session.getPlayers()) {
            AManhuntRole role = session.getRole(player);
            if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunnerCount++;
            }
        }
        return speedRunnerKillCount >= speedrunnerCount;
    }
}
