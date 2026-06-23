package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages portal location storage for players across different dimensions.
 * Part of the Player Tracking bounded context.
 */
public interface PortalTracker {

    /**
     * Store a portal location for a player in a specific dimension.
     *
     * @param player The player who owns this portal
     * @param dimension The dimension where the portal leads
     * @param location The portal location
     */
    void setPortalLocation(Player player, World.Environment dimension, Location location);

    /**
     * Retrieve a portal location for a player in a specific dimension.
     *
     * @param player The player
     * @param dimension The dimension
     * @return Optional containing location if portal exists, empty otherwise
     */
    Optional<Location> getPortalLocation(Player player, World.Environment dimension);

    /**
     * Clear all portal locations for a player.
     *
     * @param player The player
     */
    void clearPortals(Player player);

    /**
     * Check if a player has a portal in a specific dimension.
     *
     * @param player The player
     * @param dimension The dimension
     * @return true if portal exists
     */
    boolean hasPortal(Player player, World.Environment dimension);
}

/**
 * In-memory implementation of PortalTracker.
 * Stores portal locations in nested HashMaps.
 */
public class InMemoryPortalTracker implements PortalTracker {

    private final Map<Player, Map<World.Environment, Location>> portalLocations;

    public InMemoryPortalTracker() {
        this.portalLocations = new HashMap<>();
    }

    @Override
    public void setPortalLocation(Player player, World.Environment dimension, Location location) {
        portalLocations.computeIfAbsent(player, k -> new HashMap<>())
                      .put(dimension, location);
    }

    @Override
    public Optional<Location> getPortalLocation(Player player, World.Environment dimension) {
        Map<World.Environment, Location> playerPortals = portalLocations.get(player);
        if (playerPortals == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerPortals.get(dimension));
    }

    @Override
    public void clearPortals(Player player) {
        portalLocations.remove(player);
    }

    @Override
    public boolean hasPortal(Player player, World.Environment dimension) {
        return getPortalLocation(player, dimension).isPresent();
    }
}
