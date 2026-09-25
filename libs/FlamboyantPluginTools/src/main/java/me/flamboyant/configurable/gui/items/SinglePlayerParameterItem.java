package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.parameters.SinglePlayerParameter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

public class SinglePlayerParameterItem extends ATraversableParameterItem {
    private SinglePlayerParameter parameter;
    private int index = -1;

    public SinglePlayerParameterItem(SinglePlayerParameter parameter) {
        super(parameter);
        this.parameter = parameter;

        for (int i = 0; i < parameter.getPossibleValues().length && index == -1; i++) {
            if (parameter.getPossibleValues()[i] == parameter.getConcernedPlayer())
                index = i;
        }
    }

    @Override
    protected void updateItem() {
       super.updateItem();

       if (index == -1)
           iconItem.setType(Material.BARRIER);
       else {
           iconItem.setType(Material.PLAYER_HEAD);

           SkullMeta skull = (SkullMeta) iconItem.getItemMeta();
           skull.setOwningPlayer(parameter.getPossibleValues()[index]);
           iconItem.setItemMeta(skull);
       }
    }

    @Override
    protected String getValueText() {
        return index == -1 ? "Aucun" :  parameter.getConcernedPlayer().getName();
    }

    @Override
    protected void doLeftClickAction() {
        if (++index == parameter.getPossibleValues().length) {
            if (!parameter.isNullable()) index = 0;
            else {
                index = -1;
                parameter.setConcernedPlayer(null);
                return;
            }
        }
        parameter.setConcernedPlayer(parameter.getPossibleValues()[index]);
    }

    @Override
    protected void doRightClickAction() {
        if (index == -1) index = parameter.getPossibleValues().length;
        if (--index < 0) {
            if (!parameter.isNullable()) index = parameter.getPossibleValues().length - 1;
            else {
                index = -1;
                parameter.setConcernedPlayer(null);
                return;
            }
        }
        parameter.setConcernedPlayer(parameter.getPossibleValues()[index]);
    }

    public Player getChosenPlayer() {
        return parameter.getConcernedPlayer();
    }
}
