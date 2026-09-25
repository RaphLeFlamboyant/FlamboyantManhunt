package me.flamboyant.configurable.parameters;

import org.bukkit.Material;

public class BooleanParameter extends IntParameter {
    public BooleanParameter(Material representation, String parameterName, String description) {
        super(representation, parameterName, description, 0, 0, 1);
    }
}
