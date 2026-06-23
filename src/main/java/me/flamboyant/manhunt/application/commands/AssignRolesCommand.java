package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AssignRolesCommand {
    private final GameSessionId sessionId;
    private final Map<Player, ManhuntRoleIdentifier> roleAssignments;

    public AssignRolesCommand(GameSessionId sessionId,
                             Map<Player, ManhuntRoleIdentifier> roleAssignments) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (roleAssignments == null || roleAssignments.isEmpty()) {
            throw new IllegalArgumentException("Role assignments cannot be null or empty");
        }

        this.sessionId = sessionId;
        this.roleAssignments = Collections.unmodifiableMap(new HashMap<>(roleAssignments));
    }

    public GameSessionId getSessionId() {
        return sessionId;
    }

    public Map<Player, ManhuntRoleIdentifier> getRoleAssignments() {
        return roleAssignments;
    }
}
