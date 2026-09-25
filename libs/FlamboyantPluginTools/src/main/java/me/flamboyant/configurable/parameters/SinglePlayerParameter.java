package me.flamboyant.configurable.parameters;

import me.flamboyant.utils.Common;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class SinglePlayerParameter extends AParameter {
    private Player concernedPlayer;
    private Player[] possibleValues;
    private boolean isNullable;

    public SinglePlayerParameter(String parameterName, String description) {
        this(parameterName, description, false);

    }
    public SinglePlayerParameter(String parameterName, String description, boolean isNullable) {
        super(Material.PLAYER_HEAD, parameterName, description);

        possibleValues = Arrays.stream(Common.server.getOfflinePlayers()).filter(op -> op.getPlayer() != null).map(op -> op.getPlayer()).toArray(Player[]::new);
        concernedPlayer = possibleValues[0];
        this.isNullable = isNullable;
    }

    public Player[] getPossibleValues() { return possibleValues; }
    public Player getConcernedPlayer() { return concernedPlayer; }
    public boolean isNullable() { return isNullable; }
    public void setNullable(boolean isNullable) { this.isNullable = isNullable; }
    public void setConcernedPlayer(Player concernedPlayer) {
        if (!isNullable && concernedPlayer == null) return;
        this.concernedPlayer = concernedPlayer;
    }
}
