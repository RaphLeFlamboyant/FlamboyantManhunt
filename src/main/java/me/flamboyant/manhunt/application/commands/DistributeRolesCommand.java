package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributeRolesCommand {
    private final GameSessionId sessionId;
    private final List<Player> players;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final Map<Player, ManhuntRoleIdentifier> fixedAssignments;

    public DistributeRolesCommand(GameSessionId sessionId, List<Player> players, int speedrunnerCount, int allyCount,
                                  boolean specialRolesOnly,
                                  Map<Player, ManhuntRoleIdentifier> fixedAssignments) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Players list cannot be null or empty");
        }
        if (speedrunnerCount < 0 || allyCount < 0) {
            throw new IllegalArgumentException("Counts must be non-negative");
        }

        this.sessionId = sessionId;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.speedrunnerCount = speedrunnerCount;
        this.allyCount = allyCount;
        this.specialRolesOnly = specialRolesOnly;
        this.fixedAssignments = fixedAssignments != null
            ? Collections.unmodifiableMap(new HashMap<>(fixedAssignments))
            : Collections.emptyMap();
    }

    public GameSessionId getSessionId() {
        return sessionId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getSpeedrunnerCount() {
        return speedrunnerCount;
    }

    public int getAllyCount() {
        return allyCount;
    }

    public boolean isSpecialRolesOnly() {
        return specialRolesOnly;
    }

    public Map<Player, ManhuntRoleIdentifier> getFixedAssignments() {
        return fixedAssignments;
    }
}
