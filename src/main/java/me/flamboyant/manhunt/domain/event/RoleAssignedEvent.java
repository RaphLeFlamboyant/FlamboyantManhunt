package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

/**
 * Published when a role is assigned to a player during game setup.
 */
public class RoleAssignedEvent extends DomainEvent {
    private final Player player;
    private final ManhuntRoleIdentifier roleIdentifier;

    public RoleAssignedEvent(GameSessionId sessionId, Player player, ManhuntRoleIdentifier roleIdentifier) {
        super(sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (roleIdentifier == null) {
            throw new IllegalArgumentException("Role identifier cannot be null");
        }
        this.player = player;
        this.roleIdentifier = roleIdentifier;
    }

    public Player getPlayer() {
        return player;
    }

    public ManhuntRoleIdentifier getRoleIdentifier() {
        return roleIdentifier;
    }
}
