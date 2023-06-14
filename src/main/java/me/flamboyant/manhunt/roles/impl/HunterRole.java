package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.List;
import java.util.stream.Collectors;

public class HunterRole extends AManhuntRole implements Listener {
    protected List<Player> speedrunnerList;
    protected int targetIndex = 0;

    public HunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        speedrunnerList = GameData.playerClassList.keySet().stream().filter(r -> GameData.playerClassList.get(r).getRoleType() == ManhuntRoleType.SPEEDRUNNER).collect(Collectors.toList());

        ItemStack item = new ItemStack(Material.COMPASS);
        owner.getInventory().addItem(item);

        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        return true;
    }

    @Override
    protected boolean doStop() {
        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerRespawnEvent.getHandlerList().unregister(this);
        owner.setCooldown(Material.COMPASS, 0);
        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (GameData.remainingSpeedrunner == 0 ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected String getName() {
        return "Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position.";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.HUNTER;
    }

    private void logCompassUse(Player p) {
        Player sprFound = speedrunnerList.get(targetIndex);
        String log = "[CMPS] " + p.getDisplayName() + " [" + p.getWorld().getName() + ": " + p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + "]; Spdr found [" + sprFound.getWorld().getName() + ": " + sprFound.getLocation().getBlockX() + " " + sprFound.getLocation().getBlockY() + " " + sprFound.getLocation().getBlockZ() + "]";
        Bukkit.getLogger().warning(log);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (owner != event.getPlayer()) return;
        if (!event.hasItem() || event.getItem().getType() != Material.COMPASS) return;
        if (owner.hasCooldown(Material.COMPASS)) return;

        owner.setCooldown(Material.COMPASS, 30 * 20);

        if (++targetIndex >= speedrunnerList.size())
            targetIndex = 0;

        Player target = speedrunnerList.get(targetIndex);
        logCompassUse(owner);
        Location huntedLocation = target.getLocation();
        World huntedWorld = huntedLocation.getWorld();
        if (owner.getWorld() != huntedWorld) {
            owner.sendMessage("Le speedrunner est dans la dimension " + huntedWorld.getName());

            if (owner.getWorld().getName().equals("world"))
                huntedLocation = GameData.overworldLocationBeforePortal.get(target);
            if (owner.getWorld().getName().equals("world_nether"))
                huntedLocation = GameData.netherLocationBeforePortal.get(target);
        }

        if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
            Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
            lodeStoneLocation.getBlock().setType(Material.LODESTONE);

            CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
            compassMeta.setLodestone(lodeStoneLocation);
            compassMeta.setLodestoneTracked(true);
            event.getItem().setItemMeta(compassMeta);
        }
        else {
            CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
            compassMeta.setLodestone(null);
            compassMeta.setLodestoneTracked(false);
            event.getItem().setItemMeta(compassMeta);
            owner.setCompassTarget(huntedLocation);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (event.getPlayer() != owner) return;

        ItemStack item = new ItemStack(Material.COMPASS);
        event.getPlayer().getInventory().addItem(item);
    }
}
