package me.flamboyant.configurable.parameters;

import org.bukkit.Material;

public class ValueOfPlayerParameter extends AParameter {
    private String playerName;
    private AParameter subParameter;

    public ValueOfPlayerParameter(AParameter subParameter, String prefix, String playerName, String description) {
        super(Material.PLAYER_HEAD, prefix + " " + playerName, description);

        this.playerName = playerName;
        this.subParameter = subParameter;
    }

    public String getPlayerName() {
        return playerName;
    }
    public AParameter getSubParameter() {
        return subParameter;
    }
}
