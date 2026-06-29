package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.WinCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/**
 * Win condition modifier for Super Hunter role.
 *
 * <p>Modifies hunter victory conditions by requiring the Super Hunter to personally
 * kill all speedrunners. If any speedrunner dies by other means, the Super Hunter
 * loses and the standard hunter win condition is blocked.</p>
 *
 * <p>Requirements:</p>
 * <ul>
 *   <li>Must kill 3 speedrunners personally to unlock hunter victory</li>
 *   <li>Tracks kills via EntityDamageByEntityEvent</li>
 *   <li>Broadcasts progress to all players</li>
 * </ul>
 */
public class SuperHunterWinModifierAbility implements Ability, WinConditionModifier {
    private static final int REQUIRED_KILL_COUNT = 3;

    private final AbilityContext context;
    private int speedrunnerKillCount = 0;

    public SuperHunterWinModifierAbility(AbilityContext context) {
        this.context = context;
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        // Register as modifier with evaluator
        GameSession session = context.getSession();
        if (session != null && session.getWinConditionEvaluator() != null) {
            session.getWinConditionEvaluator().registerModifier(this);
        }

        // Register event handler for tracking speedrunner kills
        context.registerEventHandler(EntityDamageByEntityEvent.class, this::onEntityDamageByEntity);
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
        // Evaluator cleanup handled by session destruction
    }

    @Override
    public String getName() {
        return "Super Hunter Win Modifier";
    }

    @Override
    public String getDescription() {
        return "Gagne quand tu as tué tous les speedrunners (3) de tes propres mains. " +
               "Si un speedrunner meurt d'une autre façon, les hunters ne peuvent pas gagner.";
    }

    @Override
    public boolean allowsWin(WinCondition condition, GameSession session) {
        // Only gate AllSpeedrunnersDead (hunter win)
        if (condition instanceof AllSpeedrunnersDeadCondition) {
            return speedrunnerKillCount >= REQUIRED_KILL_COUNT;
        }
        return true; // Don't block other win types
    }

    @Override
    public Optional<WinCondition> getAlternativeWin(GameSession session) {
        return Optional.empty(); // No alternative win path
    }

    /**
     * Event handler for tracking speedrunner kills.
     */
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Only track kills by the ability owner
        if (!damager.equals(context.getOwner())) return;

        GameSession session = context.getSession();
        if (session == null) return;

        AManhuntRole victimRole = session.getRole(victim);
        if (victimRole == null) return;

        // Check if victim is a speedrunner and will die from this damage
        if (victimRole.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                onSpeedrunnerKilled(victim);
            }
        }
    }

    /**
     * Called when the Super Hunter kills a speedrunner.
     * Package-private for testing.
     */
    void onSpeedrunnerKilled(Player victim) {
        speedrunnerKillCount++;

        String ownerName = context.getOwner().getDisplayName();
        context.getMessageService().broadcastMessage(
            ownerName + " (SuperHunter) a tué un speedrunner ! ("
            + speedrunnerKillCount + "/" + REQUIRED_KILL_COUNT + ")"
        );

        if (speedrunnerKillCount >= REQUIRED_KILL_COUNT) {
            context.getMessageService().broadcastMessage(
                "SuperHunter peut maintenant permettre la victoire des Hunters !"
            );
        }
    }

    /**
     * Increment kill count. Package-private for testing.
     */
    void incrementKillCount() {
        speedrunnerKillCount++;
    }

    /**
     * Get current kill count. Package-private for testing.
     */
    int getKillCount() {
        return speedrunnerKillCount;
    }
}
