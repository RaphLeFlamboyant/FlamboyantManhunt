package me.flamboyant.configurable.gui.items;

import me.flamboyant.configurable.parameters.BooleanParameter;

public class BooleanParameterItem extends IntParameterItem {
    public BooleanParameterItem(BooleanParameter parameter) {
        super(parameter);
    }

    @Override
    protected String getValueText() {
        return parameter.getValue() == 0 ? "NO" : "YES";
    }
}
