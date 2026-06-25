package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.domain.event.*;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.tracking.PortalTracker;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import me.flamboyant.manhunt.domain.wincondition.WinConditionEvaluator;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSession {
    private final GameSessionId id;
    private final Map<Player, AManhuntRole> playerRoles;
    private final PortalTracker portalTracker;
    private final DomainEventPublisher eventPublisher;
    private WinConditionEvaluator winConditionEvaluator;
    private int remainingSpeedrunners;
    private GamePhase currentPhase = GamePhase.PREPARATION;
    private HandlerRegistration handlerRegistration;

    private static final Map<GamePhase, Set<GamePhase>> VALID_TRANSITIONS;
    static {
        Map<GamePhase, Set<GamePhase>> transitions = new HashMap<>();
        transitions.put(GamePhase.PREPARATION, Collections.singleton(GamePhase.ACTIVE));
        // ACTIVE has no valid transitions - game ends instead
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    // Constructor with defaults (for backward compatibility)
    public GameSession(GameSessionId id) {
        this(id, new InMemoryPortalTracker(), new InMemoryEventPublisher());
    }

    // Constructor with dependency injection (for testing)
    public GameSession(GameSessionId id, PortalTracker portalTracker, DomainEventPublisher eventPublisher) {
        if (id == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (portalTracker == null) {
            throw new IllegalArgumentException("PortalTracker cannot be null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("EventPublisher cannot be null");
        }
        this.id = id;
        this.playerRoles = new HashMap<>();
        this.portalTracker = portalTracker;
        this.eventPublisher = eventPublisher;
        this.remainingSpeedrunners = 0;
    }

    public GameSessionId getId() {
        return id;
    }

    public DomainEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    /**
     * Get the win condition evaluator for this session.
     * @return the win condition evaluator, or null if not configured
     */
    public WinConditionEvaluator getWinConditionEvaluator() {
        return winConditionEvaluator;
    }

    /**
     * Set the win condition evaluator for this session.
     * @param evaluator the win condition evaluator
     */
    public void setWinConditionEvaluator(WinConditionEvaluator evaluator) {
        this.winConditionEvaluator = evaluator;
    }

    /**
     * Set the handler registration for this session.
     * Used to track Bukkit event handlers for automatic cleanup.
     *
     * @param registration the handler registration
     */
    public void setHandlerRegistration(HandlerRegistration registration) {
        this.handlerRegistration = registration;
    }

    /**
     * Get the handler registration for this session.
     *
     * @return the handler registration, or null if not set
     */
    public HandlerRegistration getHandlerRegistration() {
        return handlerRegistration;
    }

    /**
     * Get the current game phase.
     *
     * @return current phase
     */
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Check if transition to target phase is valid from current phase.
     *
     * @param targetPhase the phase to transition to
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(GamePhase targetPhase) {
        Set<GamePhase> validTargets = VALID_TRANSITIONS.get(currentPhase);
        return validTargets != null && validTargets.contains(targetPhase);
    }

    /**
     * Transition to a new game phase.
     * Validates the transition and publishes PhaseChangedEvent.
     *
     * @param newPhase the phase to transition to
     * @throws IllegalStateException if transition is invalid
     */
    public void transitionTo(GamePhase newPhase) {
        if (newPhase == null) {
            throw new IllegalArgumentException("New phase cannot be null");
        }

        if (!canTransitionTo(newPhase)) {
            throw new IllegalStateException(
                String.format("Invalid transition from %s to %s", currentPhase, newPhase)
            );
        }

        GamePhase oldPhase = currentPhase;
        currentPhase = newPhase;

        eventPublisher.publish(new PhaseChangedEvent(id, oldPhase, newPhase));
    }

    public void assignRole(Player player, AManhuntRole role) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        playerRoles.put(player, role);
        eventPublisher.publish(new RoleAssignedEvent(id, player, role.getRoleIdentifier()));
    }

    public AManhuntRole getRole(Player player) {
        return playerRoles.get(player);
    }

    public boolean hasRole(Player player) {
        return playerRoles.containsKey(player);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(playerRoles.keySet());
    }

    public Map<Player, AManhuntRole> getAllRoles() {
        return Collections.unmodifiableMap(playerRoles);
    }

    public void recordPortalEntry(Player player, Location location, World.Environment environment) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        portalTracker.setPortalLocation(player, environment, location);
    }

    public Location getPortalLocation(Player player, World.Environment environment) {
        return portalTracker.getPortalLocation(player, environment).orElse(null);
    }

    public void setRemainingSpeedrunners(int count) {
        this.remainingSpeedrunners = count;
    }

    public int getRemainingSpeedrunners() {
        return remainingSpeedrunners;
    }

    public int decrementSpeedrunners() {
        if (remainingSpeedrunners > 0) {
            remainingSpeedrunners--;
        }
        return remainingSpeedrunners;
    }

    /**
     * Publish GameStartedEvent to notify handlers that the game has begun.
     */
    public void notifyGameStarted() {
        eventPublisher.publish(new GameStartedEvent(id, getPlayers(), remainingSpeedrunners));
    }

    /**
     * Publish SpeedrunnerDiedEvent when a speedrunner dies.
     * Decrements remaining speedrunner count and publishes event.
     *
     * @param player The speedrunner who died
     */
    public void notifySpeedrunnerDied(Player player) {
        int remaining = decrementSpeedrunners();
        eventPublisher.publish(new SpeedrunnerDiedEvent(id, player, remaining));
    }

    /**
     * Publish DragonKilledEvent when the Ender Dragon is killed.
     *
     * @param killer The player who killed the dragon (may be null)
     */
    public void notifyDragonKilled(Player killer) {
        eventPublisher.publish(new DragonKilledEvent(id, killer));
    }

    /**
     * Publish RolesRevealedEvent when surprise mode ends.
     */
    public void notifyRolesRevealed() {
        eventPublisher.publish(new RolesRevealedEvent(id));
    }

    /**
     * Publish GameEndedEvent when the game ends.
     *
     * @param outcome The win outcome
     * @param reason Human-readable reason for game end
     */
    public void notifyGameEnded(WinOutcome outcome, String reason) {
        eventPublisher.publish(new GameEndedEvent(id, outcome, reason));
    }

    public void end() {
        eventPublisher.unsubscribeAll(); // Cleanup handlers to prevent leaks
        clear();
    }

    public void clear() {
        // Clear all portals for all players in this session
        for (Player player : playerRoles.keySet()) {
            portalTracker.clearPortals(player);
        }
        playerRoles.clear();
        remainingSpeedrunners = 0;
    }
}
