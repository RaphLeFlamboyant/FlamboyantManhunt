package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityManager;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinition;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleTest {
    private Player mockOwner;
    private RoleDefinition mockDefinition;
    private AbilityManager mockAbilityManager;
    private GameSessionManager mockSessionManager;
    private MessageService mockMessageService;
    private ItemService mockItemService;
    private EventRegistrationService mockEventRegistration;
    private Server mockServer;
    private Plugin mockPlugin;
    private GameSession mockSession;

    @BeforeEach
    void setUp() {
        mockOwner = mock(Player.class);
        mockDefinition = mock(RoleDefinition.class);
        mockAbilityManager = mock(AbilityManager.class);
        mockSessionManager = mock(GameSessionManager.class);
        mockMessageService = mock(MessageService.class);
        mockItemService = mock(ItemService.class);
        mockEventRegistration = mock(EventRegistrationService.class);
        mockServer = mock(Server.class);
        mockPlugin = mock(Plugin.class);
        mockSession = mock(GameSession.class);

        when(mockDefinition.getIdentifier()).thenReturn(SPEEDRUNNER_SIMPLE);
        when(mockDefinition.getRoleType()).thenReturn(SPEEDRUNNER);
        when(mockDefinition.getName()).thenReturn("Speedrunner");
        when(mockDefinition.getDescription()).thenReturn("Test description");
        when(mockDefinition.createAbilities(any())).thenReturn(java.util.Collections.emptyList());

        when(mockSessionManager.getActiveSessionForPlayer(mockOwner)).thenReturn(mockSession);
    }

    @Test
    void roleReturnsDefinitionProperties() {
        Role role = new Role(
            mockOwner,
            mockDefinition,
            mockAbilityManager,
            mockSessionManager,
            mockMessageService,
            mockItemService,
            mockEventRegistration,
            mockServer,
            mockPlugin
        );

        assertEquals(SPEEDRUNNER_SIMPLE, role.getRoleIdentifier());
        assertEquals(SPEEDRUNNER, role.getRoleType());
        assertEquals("Speedrunner", role.getName());
        assertEquals("Test description", role.getDescription());
    }

    @Test
    void doStartRegistersAbilities() {
        Ability mockAbility = mock(Ability.class);
        when(mockDefinition.createAbilities(any())).thenReturn(java.util.Collections.singletonList(mockAbility));

        Role role = new Role(
            mockOwner,
            mockDefinition,
            mockAbilityManager,
            mockSessionManager,
            mockMessageService,
            mockItemService,
            mockEventRegistration,
            mockServer,
            mockPlugin
        );

        role.start();

        verify(mockAbilityManager).registerAbility(eq(mockAbility), any());
    }

    @Test
    void doStopUnregistersAbilities() {
        Ability mockAbility = mock(Ability.class);
        when(mockDefinition.createAbilities(any())).thenReturn(java.util.Collections.singletonList(mockAbility));

        Role role = new Role(
            mockOwner,
            mockDefinition,
            mockAbilityManager,
            mockSessionManager,
            mockMessageService,
            mockItemService,
            mockEventRegistration,
            mockServer,
            mockPlugin
        );

        role.start();
        role.stop();

        verify(mockAbilityManager).unregisterAbility(eq(mockAbility), any());
    }
}
