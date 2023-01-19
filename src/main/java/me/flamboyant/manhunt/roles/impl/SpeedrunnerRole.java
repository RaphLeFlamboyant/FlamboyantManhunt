package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.common.utils.ChatColorUtils;
import me.flamboyant.common.utils.Common;
import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.NewManhuntManager;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitTask;

public class SpeedrunnerRole extends AManhuntRole implements Listener {
    private static BukkitTask onWinConTask;
    private static boolean winconMet;

    public SpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        Bukkit.broadcastMessage(ChatColorUtils.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (winconMet ? "gagné" : "perdu") + " !"));

        EntityPortalEnterEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        return true;
    }

    @Override
    protected boolean doStart() {
        winconMet = false;
        onWinConTask = null;
        GameData.netherLocationBeforePortal.put(owner, owner.getLocation());
        GameData.overworldLocationBeforePortal.put(owner, owner.getLocation());

        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        return true;
    }

    @Override
    protected String getName() {
        return "Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais ut perds si tu meurs avant !";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.SPEEDRUNNER;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0) {
            winconMet = true;
            if (onWinConTask == null) {
                onWinConTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
                    NewManhuntManager.getInstance().stopGame("Le dragon est mort !");
                }, 1);
            }
        }
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (player != owner) return;
        if (event.getLocation().getWorld().getName().equals("world"))
            GameData.overworldLocationBeforePortal.put(player, event.getLocation());
        if (event.getLocation().getWorld().getName().equals("world_nether"))
            GameData.netherLocationBeforePortal.put(player, event.getLocation());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        if (event.getEntity() != owner) return;

        owner.spigot().respawn();

        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
            if (GameData.remainingSpeedrunner > 0)
                owner.setGameMode(GameMode.SPECTATOR);
            }, 1);
    }
}
