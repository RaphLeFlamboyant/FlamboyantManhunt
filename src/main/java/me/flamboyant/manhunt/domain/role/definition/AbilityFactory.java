package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;

@FunctionalInterface
public interface AbilityFactory {
    Ability create(AbilityContext context);
}
