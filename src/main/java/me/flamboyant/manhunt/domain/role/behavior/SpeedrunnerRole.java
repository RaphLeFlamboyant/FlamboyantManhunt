package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView;
import me.flamboyant.manhunt.NewManhuntManager;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.stream.Collectors;

public class SpeedrunnerRole extends AManhuntRole implements Listener {
    private static BukkitTask onWinConTask;
    private static boolean winconMet;

    private GameSession session;
    private PlayerSelectionView trackView;
    private ItemStack lastCompassUsed;

    public SpeedrunnerRole(Player owner) {
        super(owner);
    }

    public DamageOutcome handleDamage(double damageAmount) {
        return DamageOutcome.of(owner.getHealth(), damageAmount);
    }

    @Override
    protected boolean doStop() {
        EntityPortalEnterEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        owner.setCooldown(Material.COMPASS, 0);
        return true;
    }

    @Override
    protected boolean doStart() {
        winconMet = false;
        onWinConTask = null;

        // Get session from GameSessionManager
        session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session == null) {
            return false;
        }

        session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
        session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);

        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        trackView = new PlayerSelectionView(
            session.getPlayers().stream().filter(p -> p != owner).collect(Collectors.toList()),
            "Track Selection"
        );
        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (winconMet ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected String getName() {
        return "Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! Utiliser une boussole te " +
                "donne la localisation d'un hunter, avec un cooldown de 15 minutes (mais il te faudra la fabriquer ou la trouver).";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.SPEEDRUNNER;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() == EntityType.ENDER_DRAGON) {
            EnderDragon dragon = (EnderDragon) event.getEntity();
            if (dragon.getHealth() - event.getFinalDamage() <= 0) {
                winconMet = true;
                if (onWinConTask == null) {
                    onWinConTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
                        NewManhuntManager.getInstance().stopGame("Le dragon est mort !");
                    }, 1);
                }
            }
            return;
        }

        if (event.getEntity() == owner && owner.getHealth() - event.getFinalDamage() <= 0) {
            owner.setGameMode(GameMode.SPECTATOR);
            event.setCancelled(true);
            doStop();
        }
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (player != owner) return;

        String worldName = event.getLocation().getWorld().getName();
        if (worldName.equals("world")) {
            session.recordPortalEntry(player, event.getLocation(), World.Environment.NORMAL);
        } else if (worldName.equals("world_nether")) {
            session.recordPortalEntry(player, event.getLocation(), World.Environment.NETHER);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (owner != event.getPlayer()) return;
        if (!event.hasItem() || event.getItem().getType() != Material.COMPASS) return;
        if (owner.hasCooldown(Material.COMPASS)) return;

        lastCompassUsed = event.getItem();
        Common.server.getPluginManager().registerEvents(trackView, Common.plugin);
        owner.openInventory(trackView.getView());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != owner) return;
        if (event.getInventory() != trackView.getView()) return;

        if (trackView.getSelectedPlayer() != null) {
            doCompassEffect(trackView.getSelectedPlayer());
            addCompassCooldown();
        }
        trackView.close();
    }

    protected void doCompassEffect(Player target) {
        CompassTarget compassTarget = calculateCompassTarget(target, session);

        if (compassTarget.isCrossDimension()) {
            owner.sendMessage(target.getDisplayName() + " est dans la dimension " + compassTarget.getTargetDimensionName());
        }

        Location huntedLocation = compassTarget.getLocation();
        if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
            Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
            lodeStoneLocation.getBlock().setType(Material.LODESTONE);

            CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
            compassMeta.setLodestone(lodeStoneLocation);
            compassMeta.setLodestoneTracked(true);
            lastCompassUsed.setItemMeta(compassMeta);
        }
        else {
            CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
            compassMeta.setLodestone(null);
            compassMeta.setLodestoneTracked(false);
            lastCompassUsed.setItemMeta(compassMeta);
            owner.setCompassTarget(huntedLocation);
        }
    }

    protected void addCompassCooldown() {
        owner.setCooldown(Material.COMPASS, 15 * 60 * 20);
    }
}
