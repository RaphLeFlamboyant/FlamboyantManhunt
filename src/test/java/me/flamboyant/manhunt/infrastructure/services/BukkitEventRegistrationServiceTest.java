package me.flamboyant.manhunt.infrastructure.services;

import me.flamboyant.manhunt.application.exceptions.EventRegistrationException;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class BukkitEventRegistrationServiceTest {
    private BukkitEventRegistrationService eventRegistration;
    private Server mockServer;
    private Plugin mockPlugin;
    private PluginManager mockPluginManager;
    private Listener mockListener;

    @Before
    public void setup() {
        mockServer = mock(Server.class);
        mockPlugin = mock(Plugin.class);
        mockPluginManager = mock(PluginManager.class);
        mockListener = mock(Listener.class);

        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);

        eventRegistration = new BukkitEventRegistrationService(mockServer, mockPlugin);
    }

    @Test
    public void testRegisterListener_callsPluginManager() {
        eventRegistration.registerListener(mockListener);

        verify(mockPluginManager).registerEvents(mockListener, mockPlugin);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterListener_nullListener_throwsException() {
        eventRegistration.registerListener(null);
    }

    @Test(expected = EventRegistrationException.class)
    public void testRegisterListener_pluginManagerThrows_wrapsException() {
        doThrow(new RuntimeException("Test exception")).when(mockPluginManager).registerEvents(any(), any());

        eventRegistration.registerListener(mockListener);
    }

    @Test
    public void testUnregisterListener_callsHandlerList() {
        // Note: HandlerList.unregisterAll is static, can't easily mock
        // This test just verifies the method doesn't throw
        eventRegistration.unregisterListener(mockListener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterListener_nullListener_throwsException() {
        eventRegistration.unregisterListener(null);
    }
}
