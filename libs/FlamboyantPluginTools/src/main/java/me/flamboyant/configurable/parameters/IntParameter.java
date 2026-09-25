package me.flamboyant.configurable.parameters;

import org.bukkit.Material;

public class IntParameter extends AParameter {
    protected int value;
    private int minValue;
    private int maxValue;

    public IntParameter(Material representation, String parameterName, String description, int defaultValue, int minValue, int maxValue) {
        super(representation, parameterName, description);

        this.minValue = minValue;
        this.maxValue = maxValue;
        value = defaultValue;
    }

    public void setValue(int newValue) { value = newValue; }

    public int getValue() { return value; }
    public int getMinValue() { return minValue; }
    public int getMaxValue() { return maxValue; }
}
