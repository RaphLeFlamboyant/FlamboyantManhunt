package me.flamboyant.manhunt.domain.role.definition;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

@Singleton
public class RoleDefinitionRegistry {
    private final Map<ManhuntRoleIdentifier, RoleDefinition> definitions;

    @Inject
    public RoleDefinitionRegistry() {
        this.definitions = new EnumMap<>(ManhuntRoleIdentifier.class);
    }

    public void register(ManhuntRoleIdentifier identifier, RoleDefinition definition) {
        if (definitions.containsKey(identifier)) {
            throw new IllegalArgumentException("Role definition already registered: " + identifier);
        }
        definitions.put(identifier, definition);
    }

    public RoleDefinition getDefinition(ManhuntRoleIdentifier identifier) {
        RoleDefinition definition = definitions.get(identifier);
        if (definition == null) {
            throw new IllegalArgumentException("No role definition for: " + identifier);
        }
        return definition;
    }

    public boolean hasDefinition(ManhuntRoleIdentifier identifier) {
        return definitions.containsKey(identifier);
    }
}
