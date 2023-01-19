package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.common.utils.Common;
import me.flamboyant.common.utils.ItemHelper;
import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.views.PlayerSelectionView;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SpeedrunnerSwapperRole extends SpeedrunnerRole {
    private PlayerSelectionView roleView;

    public SpeedrunnerSwapperRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        PlayerInteractEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getTargetSelectionItem());

        roleView = new PlayerSelectionView(GameData.playerClassList.keySet().stream().filter(p -> p != owner).collect(Collectors.toList()), "Death Swapper Selection");
        return super.doStart();
    }

    @Override
    protected String getName() {
        return "Death Swapper Speedrunner";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() + " Pour t'aider tu as une boussole spéciale qui te permet de sélectionner un joueur toutes les 5 minutes." +
                " Cela intervertit vos position au moment du clic !";
    }

    @EventHandler
    public void onInteractWithSelectionItem(PlayerInteractEvent event) {
        if (event.getPlayer() != owner) return;
        if (!ItemHelper.isExactlySameItemKind(event.getItem(), getTargetSelectionItem())) return;
        event.setCancelled(true);

        Common.server.getPluginManager().registerEvents(roleView, Common.plugin);
        owner.openInventory(roleView.getView());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != owner) return;
        if (event.getInventory() != roleView.getView()) return;

        Player target = roleView.getSelectedPlayer();
        roleView.close();
        Location ownerLocation = owner.getLocation();
        owner.teleport(target.getLocation());
        target.teleport(ownerLocation);

        owner.setCooldown(Material.RECOVERY_COMPASS, 5 * 20);
    }

    private ItemStack getTargetSelectionItem() {
        return ItemHelper.generateItem(Material.RECOVERY_COMPASS, 1, "Choisir le joueur", Arrays.asList("Change ta place avec un joueur"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
