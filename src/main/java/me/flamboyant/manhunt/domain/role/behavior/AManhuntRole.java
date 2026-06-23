package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public abstract class AManhuntRole {
    private boolean running;
    protected Player owner;

    public AManhuntRole(Player owner) {
        this.owner = owner;
    }

    public boolean start() {
        if (running || owner == null) {
            return false;
        }

        running = doStart();

        owner.sendMessage(ChatColor.GOLD + "Tu es " + ChatColor.AQUA + ChatColor.BOLD + "[" + getName() + "]"
                + "\n" + ChatColor.RESET + ChatColor.LIGHT_PURPLE + getDescription());

        return running;
    }

    public boolean stop() {
        if (!running) {
            return false;
        }

        broadcastPlayerResultMessage();
        running = doStop();

        return true;
    }

    public boolean isRunning() {
        return running;
    }

    protected abstract boolean doStop();
    protected abstract boolean doStart();
    protected abstract void broadcastPlayerResultMessage();

    protected abstract String getName();
    protected abstract String getDescription();
    public abstract ManhuntRoleType getRoleType();

    protected CompassTarget calculateCompassTarget(Player target, GameSession session) {
        Location targetLocation = target.getLocation();
        World targetWorld = targetLocation.getWorld();
        World ownerWorld = owner.getWorld();

        if (ownerWorld == targetWorld) {
            return CompassTarget.sameDimension(targetLocation);
        }

        String ownerWorldName = ownerWorld.getName();
        Location portalLocation;
        if (ownerWorldName.equals("world")) {
            portalLocation = session.getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            portalLocation = session.getPortalLocation(target, World.Environment.NETHER);
        } else {
            portalLocation = targetLocation;
        }

        return CompassTarget.crossDimension(portalLocation, targetWorld.getName());
    }
}
