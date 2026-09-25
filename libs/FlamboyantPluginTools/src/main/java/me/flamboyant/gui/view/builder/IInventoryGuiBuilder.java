package me.flamboyant.gui.view.builder;

import me.flamboyant.gui.view.icons.IIconItem;
import me.flamboyant.gui.view.common.InventoryGui;

import java.util.List;

public interface IInventoryGuiBuilder {
    InventoryGui buildView(List<IIconItem> icons, String name, int inventorySize, boolean forceAction);
    InventoryGui buildView(List<IIconItem> icons, String name, int inventorySize, ItemGroupingMode groupingMode, boolean forceAction);
}
