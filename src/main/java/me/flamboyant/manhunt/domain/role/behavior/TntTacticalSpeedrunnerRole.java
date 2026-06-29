package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.Arrays;

public class TntTacticalSpeedrunnerRole extends SpeedrunnerRole {
    private Location lastBlockLocation;

    @Inject
    public TntTacticalSpeedrunnerRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner, server, plugin, messageService, itemService, eventRegistration);
    }

    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getTntActivationItem());
       return super.doStart();
    }

    @Override
    protected boolean doStop() {
        // Manual unregistration removed - handled by GameLifecycleService
        return super.doStop();
    }

    @Override
    public String getName() {
        return "Speedrunner Tactique TNT";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() + " Tu as également une télécommande qui fait exploser le" +
                " dernier bloc que tu as posé !";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.SPEEDRUNNER_TNT_TACTICAL;
    }

    @Override
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        super.onPlayerInteract(event);
        if (event.getPlayer() != owner) return;
        if (owner.hasCooldown(Material.RECOVERY_COMPASS)) return;
        if (!itemService.isExactlySameItemKind(event.getItem(), getTntActivationItem())) return;
        event.setCancelled(true);

        if (lastBlockLocation == null) {
            owner.sendMessage(messageService.feedback("Le dernier bloc posé a été cassé."));
            return;
        }
        if (!lastBlockLocation.getChunk().isLoaded()) {
            owner.sendMessage(messageService.feedback("Le dernier bloc posé n'est pas dans une zone chargée !"));
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
        return itemService.generateItem(Material.RECOVERY_COMPASS, 1, "Activation TNT", Arrays.asList("Fait exploser le dernier bloc placé"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
