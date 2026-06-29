package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.behavior.CompassTarget;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.time.Duration;

public abstract class CompassAbility extends ItemActivatedAbility {

    protected CompassAbility(AbilityContext context, Duration cooldown) {
        super(context, new ItemStack(Material.COMPASS), cooldown);
    }

    @Override
    protected void activate() {
        Player target = selectTarget();
        if (target == null) {
            return;
        }

        updateCompass(target);
    }

    protected abstract Player selectTarget();

    protected void updateCompass(Player target) {
        CompassTarget compassTarget = calculateCompassTarget(target);

        if (compassTarget.isCrossDimension()) {
            context.sendMessage(target.getDisplayName() + " est dans la dimension "
                + compassTarget.getTargetDimensionName());
        }

        applyCompassPointing(compassTarget);
    }

    private CompassTarget calculateCompassTarget(Player target) {
        Location targetLocation = target.getLocation();
        World targetWorld = targetLocation.getWorld();
        World ownerWorld = context.getOwner().getWorld();

        if (ownerWorld == targetWorld) {
            return CompassTarget.sameDimension(targetLocation);
        }

        String ownerWorldName = ownerWorld.getName();
        Location portalLocation;
        if (ownerWorldName.equals("world")) {
            portalLocation = context.getSession().getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            portalLocation = context.getSession().getPortalLocation(target, World.Environment.NETHER);
        } else {
            portalLocation = targetLocation;
        }

        return CompassTarget.crossDimension(portalLocation, targetWorld.getName());
    }

    private void applyCompassPointing(CompassTarget compassTarget) {
        Player owner = context.getOwner();
        Location huntedLocation = compassTarget.getLocation();

        if (owner.getWorld().getName().equalsIgnoreCase("world_nether")) {
            Location lodeStoneLocation = new Location(
                huntedLocation.getWorld(),
                huntedLocation.getBlockX(),
                0,
                huntedLocation.getBlockZ()
            );
            lodeStoneLocation.getBlock().setType(Material.LODESTONE);

            // Find compass in inventory and update it
            for (ItemStack item : owner.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) {
                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                    compassMeta.setLodestone(lodeStoneLocation);
                    compassMeta.setLodestoneTracked(true);
                    item.setItemMeta(compassMeta);
                    break;
                }
            }
        } else {
            for (ItemStack item : owner.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) {
                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                    compassMeta.setLodestone(null);
                    compassMeta.setLodestoneTracked(false);
                    item.setItemMeta(compassMeta);
                    break;
                }
            }
            owner.setCompassTarget(huntedLocation);
        }
    }
}
