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
