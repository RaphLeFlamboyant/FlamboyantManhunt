package me.flamboyant.gui.view.icons;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface IIconItem {
    String getCategory();
    ItemStack getItem();
    void onClick(InventoryClickEvent event);
}
