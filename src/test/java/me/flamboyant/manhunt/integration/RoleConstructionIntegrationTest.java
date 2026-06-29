package me.flamboyant.manhunt.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.flamboyant.manhunt.application.injection.ManhuntModule;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinitionRegistry;
import me.flamboyant.manhunt.domain.role.factory.AssistedRoleFactory;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration test verifying AssistedInject role construction.
 * Tests that all 22 role identifiers can be created with injected dependencies.
 */
public class RoleConstructionIntegrationTest {
    private Injector injector;
    private AssistedRoleFactory registry;
    private Player mockPlayer;

    @Before
    public void setup() {
        // Create mock plugin with mock server
        Plugin mockPlugin = mock(Plugin.class);
        Server mockServer = mock(Server.class);
        when(mockPlugin.getServer()).thenReturn(mockServer);

        // Create real Guice injector with ManhuntModule
        injector = Guice.createInjector(new ManhuntModule(mockPlugin));

        // Get AssistedRoleFactory from injector
        registry = injector.getInstance(AssistedRoleFactory.class);

        // Create mock player for role construction
        mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
    }

    @Test
    public void testAllRoleIdentifiersCanBeCreated() {
        // Verify all 22 role identifiers can be instantiated
        int successCount = 0;

        for (ManhuntRoleIdentifier id : ManhuntRoleIdentifier.values()) {
            try {
                AManhuntRole role = registry.createRole(id, mockPlayer);
                assertNotNull("Role should not be null for: " + id, role);
                successCount++;
            } catch (Exception e) {
                fail("Failed to create role: " + id + " - " + e.getMessage());
            }
        }

        assertEquals("All role identifiers should be creatable",
            ManhuntRoleIdentifier.values().length, successCount);
    }

    @Test
    public void testSpeedrunnerRoleHasInjectedDependencies() {
        AManhuntRole role = registry.createRole(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE, mockPlayer);

        assertNotNull("SpeedrunnerRole should be created", role);
        assertEquals("Role owner should match mock player", mockPlayer, role.getOwner());

        // Role should be properly initialized (dependencies injected)
        // We can't directly test private fields, but we can verify the role works
        assertNotNull("Role should have a name", role.getName());
    }

    @Test
    public void testHunterRoleHasInjectedDependencies() {
        AManhuntRole role = registry.createRole(ManhuntRoleIdentifier.HUNTER_SIMPLE, mockPlayer);

        assertNotNull("HunterRole should be created", role);
        assertEquals("Role owner should match mock player", mockPlayer, role.getOwner());
        assertNotNull("Role should have a name", role.getName());
    }

    @Test
    public void testDifferentRoleInstancesAreIndependent() {
        Player player1 = mock(Player.class);
        when(player1.getName()).thenReturn("Player1");

        Player player2 = mock(Player.class);
        when(player2.getName()).thenReturn("Player2");

        AManhuntRole role1 = registry.createRole(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE, player1);
        AManhuntRole role2 = registry.createRole(ManhuntRoleIdentifier.SPEEDRUNNER_SIMPLE, player2);

        assertNotSame("Roles should be different instances", role1, role2);
        assertEquals("Role1 should have player1", player1, role1.getOwner());
        assertEquals("Role2 should have player2", player2, role2.getOwner());
    }

    @Test
    public void testSpecialRolesCanBeCreated() {
        // Test special role variants
        AManhuntRole gluer = registry.createRole(ManhuntRoleIdentifier.ALLY_GLUER, mockPlayer);
        assertNotNull("GluerRole should be created", gluer);

        AManhuntRole imposter = registry.createRole(ManhuntRoleIdentifier.ALLY_IMPOSTER, mockPlayer);
        assertNotNull("ImposterRole should be created", imposter);

        AManhuntRole undecided = registry.createRole(ManhuntRoleIdentifier.UNDECIDED, mockPlayer);
        assertNotNull("UndecidedRole should be created", undecided);
    }
}
