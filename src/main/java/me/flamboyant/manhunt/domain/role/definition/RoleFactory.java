package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

/**
 * Functional interface for creating role instances.
 * Enables method reference syntax for role constructors (e.g., SpeedrunnerRole::new).
 */
@FunctionalInterface
public interface RoleFactory {
    /**
     * Creates a role instance for the given player.
     *
     * @param owner The player who will own this role
     * @return A new role instance
     */
    AManhuntRole create(Player owner);
}
