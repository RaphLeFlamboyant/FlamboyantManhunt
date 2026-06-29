package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
