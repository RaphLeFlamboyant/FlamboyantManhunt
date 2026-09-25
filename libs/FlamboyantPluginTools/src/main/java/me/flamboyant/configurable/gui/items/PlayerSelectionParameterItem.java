package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.gui.PlayerSelectionView;
import me.flamboyant.configurable.parameters.PlayerSelectionParameter;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PlayerSelectionParameterItem extends AParameterItem implements Listener {
    private PlayerSelectionParameter parameter;
    private Inventory previousInventory;
    private PlayerSelectionView playerSelectionView;

    public PlayerSelectionParameterItem(PlayerSelectionParameter parameter) {
        super(parameter);

        ItemMeta meta = iconItem.getItemMeta();
        meta.setDisplayName(parameter.getParameterName());
        meta.setLore(Arrays.asList(parameter.getDescription()));
        iconItem.setItemMeta(meta);

        this.parameter = parameter;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        openPlayerSelectionView(event.getClickedInventory());
    }

    private void openPlayerSelectionView(Inventory viewClickFrom) {
        previousInventory = viewClickFrom;
        playerSelectionView = new PlayerSelectionView(parameter.getPossibleValues(), this);

        for (HumanEntity ety : viewClickFrom.getViewers()) {
            Bukkit.getScheduler().runTaskLater(Common.plugin, () -> ety.openInventory(playerSelectionView.getView()), 1L);
        }

        Common.server.getPluginManager().registerEvents(this, Common.plugin);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != playerSelectionView.getView()) return;
        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> event.getPlayer().openInventory(previousInventory), 1L);
        playerSelectionView.close();
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    // Disable player
    public void doRightClickOnPlayer(String playerName) {
        Player player = Common.server.getPlayer(playerName);
        parameter.removeConcernedPlayer(player);
    }

    // Enable player
    public void doLeftClickOnPlayer(String playerName) {
        Player player = Common.server.getPlayer(playerName);
        parameter.addConcernedPlayer(player);
    }

    public void doLeftClickAllPlayers() {
        parameter.setAllPlayers(true);
    }

    public void doRightClickAllPlayers() {
        parameter.setAllPlayers(false);
    }

    public boolean isAllPlayers() {
        return parameter.isAllPlayers();
    }
}
