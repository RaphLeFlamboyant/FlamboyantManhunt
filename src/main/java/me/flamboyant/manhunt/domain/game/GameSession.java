package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.domain.event.*;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.tracking.PortalTracker;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
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
    private int remainingSpeedrunners;
    private GamePhase currentPhase = GamePhase.PREPARATION;

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
     * Get the current game phase.
     *
     * @return current phase
     */
    public GamePhase getCurrentPhase() {
        return currentPhase;
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
