package me.flamboyant.configurable.gui.items;

import me.flamboyant.gui.view.icons.IIconItem;
import me.flamboyant.configurable.parameters.AParameter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class AParameterItem implements IIconItem {
    protected ItemStack iconItem;
    protected String category;
    protected String description;
    protected String parameterName;
    protected Material representation;

    public AParameterItem(AParameter parameter) {
        this.category = parameter.getCategory();
        this.parameterName = parameter.getParameterName();
        this.description = parameter.getDescription();
        this.representation = parameter.getRepresentation();
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public ItemStack getItem() {
        if (iconItem == null) {
            iconItem = new ItemStack(representation);
            updateItem();
        }

        return iconItem;
    }

    protected void updateItem() {
        ItemMeta meta = iconItem.getItemMeta();
        meta.setDisplayName(parameterName);
        iconItem.setItemMeta(meta);
    }
}
