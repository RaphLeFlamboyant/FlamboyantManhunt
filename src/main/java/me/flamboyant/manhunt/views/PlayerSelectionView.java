package me.flamboyant.manhunt.views;

import me.flamboyant.utils.Common;
import me.flamboyant.utils.ItemHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class PlayerSelectionView implements Listener {
    private static final int inventorySize = 9 * 3;
    private Inventory view;
    List<Player> possibleValues;
    String viewName;
    private Player selectedPlayer;

    public PlayerSelectionView(List<Player> possibleValues, String viewName) {
        this.possibleValues = possibleValues;
        this.viewName = viewName;
    }

    public String getViewID() {
        return viewName;
    }

    public Player getSelectedPlayer() { return selectedPlayer; }

    public Inventory getView() {
        if (view == null) {
            Inventory myInventory = Bukkit.createInventory(null, inventorySize, getViewID());

            int index = 0;
            myInventory.setItem((inventorySize / 2) - (possibleValues.size() / 2) + index - 1,
                    ItemHelper.generateItem(Material.BARRIER, 1, "Aucun", Arrays.asList(), false, null, false, false));

            for (Player player : possibleValues) {
                ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skull = (SkullMeta) playerHead.getItemMeta();
                skull.setOwningPlayer(player);
                skull.setDisplayName(player.getDisplayName());
                playerHead.setItemMeta(skull);

                myInventory.setItem((inventorySize / 2) - (possibleValues.size() / 2) + index, playerHead);
                index++;
            }

            view = myInventory;
        }

        return view;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory != view) return;
        event.setCancelled(true);
        if (event.getSlotType() == InventoryType.SlotType.QUICKBAR) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || (clicked.getType() != Material.PLAYER_HEAD && clicked.getType() != Material.BARRIER)) return;

        if (clicked.getType() == Material.PLAYER_HEAD)
            selectedPlayer = Common.server.getPlayer(clicked.getItemMeta().getDisplayName());
        else
            selectedPlayer = null;
    }

    public void close() {
        InventoryClickEvent.getHandlerList().unregister(this);
        selectedPlayer = null;
        view.clear();
        view = null;
    }
}
