package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

/**
 * Factory interface for creating role instances with AssistedInject.
 * Guice implements this interface to provide dependency injection.
 *
 * @param <T> The concrete role type
 */
public interface AssistedRoleFactory<T extends AManhuntRole> {
    /**
     * Create a role instance with injected dependencies.
     *
     * @param owner The player who will own the role (runtime parameter)
     * @return A new role instance with all dependencies injected
     */
    T create(Player owner);
}
