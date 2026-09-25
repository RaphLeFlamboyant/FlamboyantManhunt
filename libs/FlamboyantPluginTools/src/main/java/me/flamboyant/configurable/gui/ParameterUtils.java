package me.flamboyant.configurable.gui;

import me.flamboyant.configurable.parameters.*;
import me.flamboyant.configurable.gui.items.*;
import me.flamboyant.gui.view.builder.IInventoryGuiBuilder;
import me.flamboyant.gui.view.builder.InventoryGuiBuilder;
import me.flamboyant.gui.view.builder.ItemGroupingMode;
import me.flamboyant.gui.view.common.InventoryGui;
import me.flamboyant.gui.view.icons.IIconItem;
import me.flamboyant.utils.Common;
import me.flamboyant.utils.IParametrable;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ParameterUtils {
    public static InventoryGui createParametersGui(IParametrable plugin, ItemGroupingMode groupingMode, boolean forceAction) {
        List<AParameter> parameters = plugin.getParameters();
        List<IIconItem> wrappedItems = parameters.stream().map(i -> wrapParameter(i)).collect(Collectors.toList());

        IInventoryGuiBuilder builder = new InventoryGuiBuilder();

        return builder.buildView(wrappedItems,
                "Paramètres de la partie",
                (9 * (Math.min(3, 1 + wrappedItems.size() / 4) + 3)),
                groupingMode,
                forceAction);
    }

    private static AParameterItem wrapParameter(AParameter parameter) {
        if (parameter instanceof ValueOfPlayerParameter) {
            ValueOfPlayerParameter vParam = (ValueOfPlayerParameter) parameter;
            AParameterItem subParamItem = wrapParameter(vParam.getSubParameter());

            SkullMeta skull = (SkullMeta) subParamItem.getItem().getItemMeta();
            skull.setOwningPlayer(Common.server.getPlayer(vParam.getPlayerName()));
            subParamItem.getItem().setItemMeta(skull);

            return subParamItem;
        }
        if (parameter instanceof BooleanParameter) return new BooleanParameterItem((BooleanParameter) parameter);
        if (parameter instanceof IntParameter) return new IntParameterItem((IntParameter) parameter);
        if (parameter instanceof PlayerSelectionParameter) return new PlayerSelectionParameterItem((PlayerSelectionParameter) parameter);
        if (parameter instanceof SinglePlayerParameter) return new SinglePlayerParameterItem((SinglePlayerParameter) parameter);
        return new EnumParameterItem((EnumParameter) parameter);
    }
}
