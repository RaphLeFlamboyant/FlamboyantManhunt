package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RolesDistributedEvent extends DomainEvent {
    private final Map<Player, ManhuntRoleIdentifier> assignments;

    public RolesDistributedEvent(GameSessionId sessionId, Map<Player, ManhuntRoleIdentifier> assignments) {
        super(sessionId);
        this.assignments = Collections.unmodifiableMap(new HashMap<>(assignments));
    }

    public Map<Player, ManhuntRoleIdentifier> getAssignments() {
        return assignments;
    }
}
