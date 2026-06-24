package me.flamboyant.manhunt;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GamePhase;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.behavior.DamageOutcome;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.wincondition.*;
import me.flamboyant.manhunt.domain.event.*;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.Optional;

public class NewManhuntManager implements Listener {
    private GameSession session;
    private final WinConditionEvaluator winConditionEvaluator;
    private final DragonKilledCondition dragonKilledCondition;

    public NewManhuntManager(
            WinConditionEvaluator winConditionEvaluator,
            DragonKilledCondition dragonKilledCondition) {
        this.winConditionEvaluator = winConditionEvaluator;
        this.dragonKilledCondition = dragonKilledCondition;
    }

    /**
     * Register domain event handlers for this game session.
     * Handlers are session-scoped and automatically cleaned up when session ends.
     */
    private void registerEventHandlers(GameSession session) {
        DomainEventPublisher publisher = session.getEventPublisher();

        // Win condition checking on speedrunner death
        publisher.subscribe(SpeedrunnerDiedEvent.class, this::onSpeedrunnerDied);

        // Win condition checking on dragon killed
        publisher.subscribe(DragonKilledEvent.class, this::onDragonKilled);

        // Win condition met notification
        publisher.subscribe(WinConditionMetEvent.class, this::onWinConditionMet);

        // Game ended cleanup
        publisher.subscribe(GameEndedEvent.class, this::onGameEnded);

        // Phase change handler
        publisher.subscribe(PhaseChangedEvent.class, this::onPhaseChanged);
    }

    /**
     * Handle speedrunner death by checking win conditions.
     */
    private void onSpeedrunnerDied(SpeedrunnerDiedEvent event) {
        // Evaluate all win conditions
        Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(session);
        if (outcome.isPresent()) {
            session.notifyGameEnded(outcome.get(), "All speedrunners eliminated!");
        }
    }

    /**
     * Handle dragon killed by marking condition and checking win conditions.
     */
    private void onDragonKilled(DragonKilledEvent event) {
        // Mark dragon killed in condition
        dragonKilledCondition.markDragonKilled();

        // Evaluate win conditions
        Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(session);
        if (outcome.isPresent()) {
            String message = event.hasKiller()
                ? event.getKiller().getDisplayName() + " defeated the dragon!"
                : "The dragon was defeated!";
            session.notifyGameEnded(outcome.get(), message);
        }
    }

    /**
     * Handle win condition met by broadcasting result.
     */
    private void onWinConditionMet(WinConditionMetEvent event) {
        Bukkit.broadcastMessage(ChatHelper.importantMessage(event.getOutcome().getDescription()));
    }

    /**
     * Handle game ended by stopping roles and cleaning up.
     */
    private void onGameEnded(GameEndedEvent event) {
        // Stop all roles
        for (AManhuntRole role : session.getAllRoles().values()) {
            role.stop();
        }

        // Unregister Bukkit listeners
        EntityDamageEvent.getHandlerList().unregister(this);

        // Broadcast reason
        Bukkit.broadcastMessage(ChatHelper.importantMessage(event.getReason()));

        // Session cleanup (will call eventPublisher.unsubscribeAll())
        session.end();
    }

    /**
     * Handle phase changes by activating phase-specific behavior.
     * When entering ACTIVE phase: start roles, register listeners, reveal roles.
     */
    private void onPhaseChanged(PhaseChangedEvent event) {
        if (event.getNewPhase() == GamePhase.ACTIVE) {
            // Start all roles
            for (AManhuntRole role : session.getAllRoles().values()) {
                role.start();
            }

            // Register Bukkit event listeners for manhunt mechanics
            Common.server.getPluginManager().registerEvents(this, Common.plugin);

            // Publish roles revealed event
            session.notifyRolesRevealed();
        }
    }

    public boolean startGame(GameSession session, int roleRevealDelayInMinutes, boolean speedrunnerSurprise) {
        this.session = session;

        // Register domain event handlers for this session
        registerEventHandlers(session);

        session.setRemainingSpeedrunners(0);

        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
            for (Player player : session.getPlayers()) {
                AManhuntRole role = session.getRole(player);
                if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                    session.setRemainingSpeedrunners(session.getRemainingSpeedrunners() + 1);
                }
                role.start();
            }

            if (speedrunnerSurprise)
                Common.server.getPluginManager().registerEvents(this, Common.plugin);
        }, (roleRevealDelayInMinutes * 60 + 1) * 20);

        if (!speedrunnerSurprise)
            Common.server.getPluginManager().registerEvents(this, Common.plugin);

        // Notify that game has started
        session.notifyGameStarted();

        return true;
    }

    public void stopGame(String reason) {
        EntityDamageEvent.getHandlerList().unregister(this);
        Bukkit.broadcastMessage(ChatHelper.importantMessage(reason));

        for (AManhuntRole role : session.getAllRoles().values()) {
            role.stop();
        }

        session.clear();
        this.session = null;
        NewManhuntLauncher.getInstance().stop();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER) return;

        Player player = (Player) event.getEntity();

        // Check if player is in this session
        if (!session.hasRole(player)) return;

        AManhuntRole role = session.getRole(player);

        // Only process speedrunner damage
        if (!(role instanceof SpeedrunnerRole)) return;

        // Check if damage is fatal
        SpeedrunnerRole speedrunner = (SpeedrunnerRole) role;
        DamageOutcome outcome = speedrunner.handleDamage(event.getFinalDamage());

        if (outcome.isDied()) {
            // Publish domain event (win condition checked by handler)
            session.notifySpeedrunnerDied(player);
        }
    }


    /**
     * Get the DragonKilledCondition for infrastructure to notify.
     * This is a temporary access point until event-based architecture.
     *
     * @return DragonKilledCondition instance
     */
    public DragonKilledCondition getDragonKilledCondition() {
        return dragonKilledCondition;
    }
}
