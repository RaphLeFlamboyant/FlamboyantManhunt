package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.parameters.EnumParameter;

public class EnumParameterItem extends ATraversableParameterItem {
    private int index = 0;
    private EnumParameter parameter;

    public EnumParameterItem(EnumParameter parameter) {
        super(parameter);

        this.parameter = parameter;
        while (parameter.getPossibleValues()[index] != parameter.getSelectedValue()) index++;
    }

    @Override
    protected String getValueText() {
        return index >= 0
                ? parameter.getPossibleValues()[index].toString()
                : "NONE";
    }

    @Override
    protected void doLeftClickAction() {
        if (++index == parameter.getPossibleValues().length) {
            if (parameter.isNullable()) {
                index = -1;
                parameter.setSelectedValue(null);
                return;
            }

            index = 0;
        }
        parameter.setSelectedValue(parameter.getPossibleValues()[index]);
    }

    @Override
    protected void doRightClickAction() {
        if (--index < 0) {
            if (index == -1 && parameter.isNullable()) {
                parameter.setSelectedValue(null);
                return;
            }

            index = parameter.getPossibleValues().length - 1;
        }
        parameter.setSelectedValue(parameter.getPossibleValues()[index]);
    }
}
