package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.parameters.AParameter;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class ATraversableParameterItem extends AParameterItem {
    public ATraversableParameterItem(AParameter parameter) {
        super(parameter);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.isLeftClick())
            doLeftClickAction();
        else if (event.isRightClick())
            doRightClickAction();
        updateItem();
    }

    protected void updateItem() {
        ItemMeta meta = iconItem.getItemMeta();
        meta.setDisplayName(parameterName);
        meta.setLore(Arrays.asList(description, getValueFinalText()));
        iconItem.setItemMeta(meta);
    }

    protected String getValueFinalText() {
        return "" + ChatColor.BOLD + ChatColor.GREEN + getValueText();
    }

    protected abstract String getValueText();

    protected abstract void doRightClickAction();
    protected abstract void doLeftClickAction();
}
