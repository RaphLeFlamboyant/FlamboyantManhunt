package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.parameters.IntParameter;

public class IntParameterItem extends ATraversableParameterItem {
    protected IntParameter parameter;

    public IntParameterItem(IntParameter parameter) {
        super(parameter);

        this.parameter = parameter;
    }

    @Override
    protected String getValueText() {
        return Integer.toString(parameter.getValue());
    }

    @Override
    protected void doLeftClickAction() {
        if (parameter.getValue() < parameter.getMaxValue())
            parameter.setValue(parameter.getValue() + 1);
    }

    @Override
    protected void doRightClickAction() {
        if (parameter.getValue() > parameter.getMinValue())
            parameter.setValue(parameter.getValue() - 1);
    }
}
