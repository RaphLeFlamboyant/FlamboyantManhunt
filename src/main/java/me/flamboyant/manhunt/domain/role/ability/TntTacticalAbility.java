package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Arrays;

public class TntTacticalAbility extends ItemActivatedAbility {
    private Location lastBlockLocation;
    private final ItemStack activationItem;

    public TntTacticalAbility(AbilityContext context) {
        super(context, createActivationItem(context), Duration.ofSeconds(15));
        this.activationItem = createActivationItem(context);
    }

    private static ItemStack createActivationItem(AbilityContext context) {
        return context.getItemService().createItem(
            Material.RECOVERY_COMPASS,
            "&6Activation TNT",
            "&7Fait exploser le dernier bloc placé"
        );
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        context.getOwner().getInventory().addItem(activationItem);

        context.registerEventHandler(BlockPlaceEvent.class, this::onBlockPlace);
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }

    @Override
    protected void activate() {
        if (lastBlockLocation == null) {
            context.sendMessage("Le dernier bloc posé a été cassé.");
            return;
        }

        if (!lastBlockLocation.getChunk().isLoaded()) {
            context.sendMessage("Le dernier bloc posé n'est pas dans une zone chargée !");
            return;
        }

        lastBlockLocation.getBlock().setType(Material.AIR);
        lastBlockLocation.getWorld().createExplosion(
            lastBlockLocation,
            4f,
            false,
            true,
            context.getOwner()
        );
    }

    private void onBlockPlace(BlockPlaceEvent event) {
        lastBlockLocation = event.getBlock().getLocation();
    }

    private void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(lastBlockLocation)) {
            lastBlockLocation = null;
        }
    }

    @Override
    protected void handleInteractEvent(PlayerInteractEvent event) {
        if (!context.getItemService().isSameItemKind(event.getItem(), triggerItem)) {
            return;
        }
        event.setCancelled(true);

        if (context.getAbilityManager().isOnCooldown(context.getOwner(), getName())) {
            return;
        }

        activate();
        context.getAbilityManager().setCooldown(context.getOwner(), getName(), cooldown);
    }

    @Override
    public String getName() {
        return "TNT Tactical";
    }

    @Override
    public String getDescription() {
        return "Télécommande qui fait exploser le dernier bloc posé";
    }
}
