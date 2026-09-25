package me.flamboyant.configurable.parameters;

import me.flamboyant.utils.Common;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class PlayerSelectionParameter extends AParameter {
    private boolean allPlayers;
    private Player[] possibleValues;
    private List<Player> selectedValues;

    public PlayerSelectionParameter(Material representation, String parameterName, String description) {
        super(representation, parameterName, description);

        possibleValues = Arrays.stream(Common.server.getOfflinePlayers()).filter(op -> op.getPlayer() != null).map(op -> op.getPlayer()).toArray(Player[]::new);
        selectedValues = Arrays.asList(possibleValues);
    }

    public Player[] getPossibleValues() { return possibleValues; }
    public List<Player> getConcernedPlayers() { return selectedValues; }

    public boolean isAllPlayers() { return allPlayers; }
    public void setAllPlayers(boolean allPlayers) { this.allPlayers = allPlayers; }
    public void addConcernedPlayer(Player concernedPlayer) {
        if (!selectedValues.contains(concernedPlayer))
            selectedValues.add(concernedPlayer);
    }

    public void removeConcernedPlayer(Player notConcernedAnymorePlayer) {
        if (selectedValues.contains(notConcernedAnymorePlayer))
            selectedValues.remove(notConcernedAnymorePlayer);
    }
}
