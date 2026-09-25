package me.flamboyant.configurable.parameters;

import org.bukkit.Material;

public class EnumParameter<T extends Enum<T>> extends AParameter {
    private T selectedValue;
    private T[] possibleValues;
    private boolean nullable;

    public EnumParameter(Material representation, String parameterName, String description, Class<T> enumClass) {
        super(representation, parameterName, description);

        possibleValues = enumClass.getEnumConstants();
        selectedValue = possibleValues[0];
    }

    public EnumParameter(Material representation, String parameterName, String description, T[] possibleValues, T initialValue) {
        super(representation, parameterName, description);

        this.possibleValues = possibleValues;
        this.selectedValue = initialValue;
    }

    public T[] getPossibleValues() { return possibleValues; }
    public T getSelectedValue() { return selectedValue; }
    public void setSelectedValue(T selectedValue) { this.selectedValue = selectedValue; }

    public void setIsNullable(boolean isNullable) { nullable = isNullable; }
    public boolean isNullable() { return nullable; }
}
