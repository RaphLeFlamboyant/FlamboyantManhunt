package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbilityContextTest {
    private Player mockOwner;
    private GameSessionManager mockSessionManager;
    private MessageService mockMessageService;
    private ItemService mockItemService;
    private EventRegistrationService mockEventRegistration;
    private Server mockServer;
    private Plugin mockPlugin;
    private AbilityManager mockAbilityManager;
    private AbilityContext context;

    @BeforeEach
    void setUp() {
        mockOwner = mock(Player.class);
        mockSessionManager = mock(GameSessionManager.class);
        mockMessageService = mock(MessageService.class);
        mockItemService = mock(ItemService.class);
        mockEventRegistration = mock(EventRegistrationService.class);
        mockServer = mock(Server.class);
        mockPlugin = mock(Plugin.class);
        mockAbilityManager = mock(AbilityManager.class);

        context = new AbilityContext(
            mockOwner,
            mockSessionManager,
            mockMessageService,
            mockItemService,
            mockEventRegistration,
            mockServer,
            mockPlugin,
            mockAbilityManager
        );
    }

    @Test
    void getOwnerReturnsPlayer() {
        assertEquals(mockOwner, context.getOwner());
    }

    @Test
    void getMessageServiceReturnsService() {
        assertEquals(mockMessageService, context.getMessageService());
    }

    @Test
    void sendMessageDelegatesToMessageService() {
        context.sendMessage("test message");

        verify(mockMessageService).sendMessage(mockOwner, "test message");
    }

    @Test
    void setSessionUpdatesSession() {
        GameSession mockSession = mock(GameSession.class);

        context.setSession(mockSession);

        assertEquals(mockSession, context.getSession());
    }
}
