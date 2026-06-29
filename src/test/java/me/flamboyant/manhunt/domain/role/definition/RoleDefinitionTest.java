package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleDefinitionTest {

    @Test
    void builderCreatesRoleDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test description")
            .withAbility(ctx -> mock(Ability.class))
            .build();

        assertEquals(SPEEDRUNNER_SIMPLE, definition.getIdentifier());
        assertEquals(SPEEDRUNNER, definition.getRoleType());
        assertEquals("Speedrunner", definition.getName());
        assertEquals("Test description", definition.getDescription());
    }

    @Test
    void createAbilitiesCallsFactories() {
        Ability mockAbility1 = mock(Ability.class);
        Ability mockAbility2 = mock(Ability.class);

        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Test")
            .description("Test")
            .withAbility(ctx -> mockAbility1)
            .withAbility(ctx -> mockAbility2)
            .build();

        AbilityContext mockContext = mock(AbilityContext.class);
        List<Ability> abilities = definition.createAbilities(mockContext);

        assertEquals(2, abilities.size());
        assertEquals(mockAbility1, abilities.get(0));
        assertEquals(mockAbility2, abilities.get(1));
    }

    @Test
    void builderRequiresAllFields() {
        assertThrows(NullPointerException.class, () ->
            RoleDefinition.builder().build()
        );
    }
}
