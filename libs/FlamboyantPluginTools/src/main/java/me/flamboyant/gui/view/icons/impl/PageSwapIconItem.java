package me.flamboyant.gui.view.icons.impl;

import me.flamboyant.gui.view.icons.IIconItem;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PageSwapIconItem implements IIconItem {
    protected ItemStack iconItem;
    private Inventory targetInventory;

    public PageSwapIconItem(ItemStack iconItem, Inventory targetInventory) {
        this.iconItem = iconItem;
        this.targetInventory = targetInventory;
    }

    @Override
    public String getCategory() {
        return "";
    }

    @Override
    public ItemStack getItem() {
        return iconItem;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.getWhoClicked().openInventory(targetInventory);
    }
}
