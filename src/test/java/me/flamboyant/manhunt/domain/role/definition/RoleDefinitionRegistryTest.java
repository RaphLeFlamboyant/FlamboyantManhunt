package me.flamboyant.manhunt.domain.role.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleDefinitionRegistryTest {
    private RoleDefinitionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RoleDefinitionRegistry();
    }

    @Test
    void registerStoresDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();

        registry.register(SPEEDRUNNER_SIMPLE, definition);

        assertTrue(registry.hasDefinition(SPEEDRUNNER_SIMPLE));
    }

    @Test
    void getDefinitionReturnsRegisteredDefinition() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();

        registry.register(SPEEDRUNNER_SIMPLE, definition);

        assertEquals(definition, registry.getDefinition(SPEEDRUNNER_SIMPLE));
    }

    @Test
    void getDefinitionThrowsForUnregistered() {
        assertThrows(IllegalArgumentException.class, () ->
            registry.getDefinition(SPEEDRUNNER_SIMPLE)
        );
    }

    @Test
    void registerDuplicateThrows() {
        RoleDefinition definition = RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Test")
            .build();

        registry.register(SPEEDRUNNER_SIMPLE, definition);

        assertThrows(IllegalArgumentException.class, () ->
            registry.register(SPEEDRUNNER_SIMPLE, definition)
        );
    }
}
