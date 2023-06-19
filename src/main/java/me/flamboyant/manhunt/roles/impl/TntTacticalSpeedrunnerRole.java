package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.ItemHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class TntTacticalSpeedrunnerRole extends SpeedrunnerRole {
    private Location lastBlockLocation;

    public TntTacticalSpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getTntActivationItem());
       return super.doStart();
    }

    @Override
    protected boolean doStop() {
        BlockPlaceEvent.getHandlerList().unregister(this);
        BlockBreakEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected String getName() {
        return "Speedrunner Tactique TNT";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() + " Tu as également une télécommande qui fait exploser le" +
                " dernier bloc que tu as posé !";
    }

    @Override
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        super.onPlayerInteract(event);
        if (event.getPlayer() != owner) return;
        if (owner.hasCooldown(Material.RECOVERY_COMPASS)) return;
        if (!ItemHelper.isExactlySameItemKind(event.getItem(), getTntActivationItem())) return;
        event.setCancelled(true);

        if (lastBlockLocation == null) {
            owner.sendMessage(ChatHelper.feedback("Le dernier bloc posé a été cassé."));
            return;
        }
        if (!lastBlockLocation.getChunk().isLoaded()) {
            owner.sendMessage(ChatHelper.feedback("Le dernier bloc posé n'est pas dans une zone chargée !"));
            return;
        }

        lastBlockLocation.getBlock().setType(Material.AIR);
        lastBlockLocation.getWorld().createExplosion(lastBlockLocation, 4f, false, true, owner);
        owner.setCooldown(Material.RECOVERY_COMPASS, 15 * 20);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer() != owner) return;
        lastBlockLocation = event.getBlock().getLocation();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(lastBlockLocation)) {
            lastBlockLocation = null;
        }
    }

    private ItemStack getTntActivationItem() {
        return ItemHelper.generateItem(Material.RECOVERY_COMPASS, 1, "Activation TNT", Arrays.asList("Fait exploser le dernier bloc placé"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
