package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for role factories following the Registry pattern.
 * Maps role identifiers to factory functions for creating role instances.
 */
public class RoleRegistry {
    private final Map<ManhuntRoleIdentifier, AssistedRoleFactory<?>> factories;

    public RoleRegistry() {
        this.factories = new HashMap<>();
    }

    /**
     * Registers a role factory for the given identifier.
     *
     * @param identifier The role identifier
     * @param factory The factory to create instances of this role
     * @throws IllegalStateException if role already registered
     */
    public void register(ManhuntRoleIdentifier identifier, AssistedRoleFactory<?> factory) {
        if (factories.containsKey(identifier)) {
            throw new IllegalStateException("Role already registered: " + identifier);
        }
        factories.put(identifier, factory);
    }

    /**
     * Creates a role instance for the given identifier and owner.
     *
     * @param identifier The role identifier
     * @param owner The player who will own the role
     * @return A new role instance
     * @throws IllegalArgumentException if role not registered
     */
    public AManhuntRole createRole(ManhuntRoleIdentifier identifier, Player owner) {
        AssistedRoleFactory<?> factory = factories.get(identifier);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for role: " + identifier);
        }
        return factory.create(owner);
    }

    /**
     * Checks if a role factory is registered.
     *
     * @param identifier The role identifier
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(ManhuntRoleIdentifier identifier) {
        return factories.containsKey(identifier);
    }
}
