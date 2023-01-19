package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ChatColorUtils;
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
    protected boolean doStop() {
        Bukkit.broadcastMessage(ChatColorUtils.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (GameData.remainingSpeedrunner == 0 ? "gagné" : "perdu") + " !"));

        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerRespawnEvent.getHandlerList().unregister(this);
        return true;
    }

    @Override
    protected boolean doStart() {
        speedrunnerList = GameData.playerClassList.keySet().stream().filter(r -> GameData.playerClassList.get(r).getRoleType() == ManhuntRoleType.SPEEDRUNNER).collect(Collectors.toList());

        ItemStack item = new ItemStack(Material.COMPASS);
        owner.getInventory().setItem(0, item);

        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        return true;
    }

    @Override
    protected String getName() {
        return "Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt";
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

        Player player = event.getPlayer();
        player.setCooldown(Material.COMPASS, 30 * 20);

        if (++targetIndex >= speedrunnerList.size())
            targetIndex = 0;

        Player target = speedrunnerList.get(targetIndex);
        logCompassUse(player);
        Location huntedLocation = target.getLocation();
        World huntedWorld = huntedLocation.getWorld();
        if (player.getWorld() != huntedWorld) {
            player.sendMessage("Le speedrunner est dans la dimension " + huntedWorld.getName());

            if (player.getWorld().getName().equals("world"))
                huntedLocation = GameData.overworldLocationBeforePortal.get(target);
            if (player.getWorld().getName().equals("world_nether"))
                huntedLocation = GameData.netherLocationBeforePortal.get(target);
        }

        if (player.getWorld().getName().equalsIgnoreCase("world_nether")){
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
            player.setCompassTarget(huntedLocation);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (speedrunnerList.contains(event.getPlayer())) return;

        ItemStack item = new ItemStack(Material.COMPASS);
        event.getPlayer().getInventory().setItem(0, item);
    }
}
