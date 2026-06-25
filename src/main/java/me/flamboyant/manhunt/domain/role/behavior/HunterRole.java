package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HunterRole extends AManhuntRole implements Listener {
    private GameSession session;
    protected List<Player> speedrunnerList;
    protected int targetIndex = 0;

    public HunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session == null) {
            return false;
        }

        // Get speedrunners from current session
        speedrunnerList = new ArrayList<>();
        for (Player player : session.getPlayers()) {
            AManhuntRole role = session.getRole(player);
            if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunnerList.add(player);
            }
        }

        ItemStack item = new ItemStack(Material.COMPASS);
        owner.getInventory().addItem(item);

        // Manual registration removed - handled by StartGameSaga
        return true;
    }

    @Override
    protected boolean doStop() {
        // Manual unregistration removed - handled by GameLifecycleService
        owner.setCooldown(Material.COMPASS, 0);
        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        boolean wincon = session != null && session.getRemainingSpeedrunners() == 0;
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (wincon ? "gagné" : "perdu") + " !"));
    }

    @Override
    public String getName() {
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

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_SIMPLE;
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

        CompassTarget compassTarget = calculateCompassTarget(target, session);

        if (compassTarget.isCrossDimension()) {
            owner.sendMessage("Le speedrunner est dans la dimension " + compassTarget.getTargetDimensionName());
        }

        Location huntedLocation = compassTarget.getLocation();
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
