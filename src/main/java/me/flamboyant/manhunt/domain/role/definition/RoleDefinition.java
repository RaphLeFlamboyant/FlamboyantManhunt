package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RoleDefinition {
    private final ManhuntRoleIdentifier identifier;
    private final ManhuntRoleType roleType;
    private final String name;
    private final String description;
    private final List<AbilityFactory> abilityFactories;

    private RoleDefinition(Builder builder) {
        this.identifier = builder.identifier;
        this.roleType = builder.roleType;
        this.name = builder.name;
        this.description = builder.description;
        this.abilityFactories = builder.abilityFactories;
    }

    public List<Ability> createAbilities(AbilityContext context) {
        return abilityFactories.stream()
            .map(factory -> factory.create(context))
            .collect(Collectors.toList());
    }

    public ManhuntRoleIdentifier getIdentifier() {
        return identifier;
    }

    public ManhuntRoleType getRoleType() {
        return roleType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ManhuntRoleIdentifier identifier;
        private ManhuntRoleType roleType;
        private String name;
        private String description;
        private List<AbilityFactory> abilityFactories = new ArrayList<>();

        public Builder identifier(ManhuntRoleIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder roleType(ManhuntRoleType roleType) {
            this.roleType = roleType;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder withAbility(AbilityFactory factory) {
            this.abilityFactories.add(factory);
            return this;
        }

        public RoleDefinition build() {
            Objects.requireNonNull(identifier, "identifier required");
            Objects.requireNonNull(roleType, "roleType required");
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(description, "description required");
            return new RoleDefinition(this);
        }
    }
}
