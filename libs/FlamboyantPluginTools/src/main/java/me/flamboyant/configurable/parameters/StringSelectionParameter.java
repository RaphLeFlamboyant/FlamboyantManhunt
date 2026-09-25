package me.flamboyant.configurable.parameters;

import org.bukkit.Material;

public class StringSelectionParameter extends AParameter {
    private String selectedValue;
    private String[] possibleValues;

    public StringSelectionParameter(Material representation, String parameterName, String description, String[] possibleValues) {
        super(representation, parameterName, description);

        this.possibleValues = possibleValues;
        selectedValue = possibleValues[0];
    }

    public String[] getPossibleValues() { return possibleValues; }
    public String getSelectedValue() { return selectedValue; }
    public void setSelectedValue(String selectedValue) { this.selectedValue = selectedValue; }
}
