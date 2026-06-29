package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

/**
 * Factory interface for creating role instances with AssistedInject.
 * Guice implements this interface to provide dependency injection.
 */
public interface AssistedRoleFactory {
    /**
     * Create a role instance with injected dependencies.
     *
     * @param owner The player who will own the role (runtime parameter)
     * @param definition The role definition containing abilities and metadata
     * @return A new role instance with all dependencies injected
     */
    AManhuntRole create(Player owner, RoleDefinition definition);
}
