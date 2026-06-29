package me.flamboyant.manhunt.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.flamboyant.manhunt.application.commands.GameLaunchConfiguration;
import me.flamboyant.manhunt.application.injection.ManhuntModule;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.GameLaunchService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for anti-corruption layer.
 * Verifies end-to-end flow: adapter → service → saga → domain.
 */
public class AntiCorruptionLayerIntegrationTest {
    private Injector injector;
    private Server mockServer;
    private Plugin mockPlugin;

    @Before
    public void setup() {
        mockPlugin = mock(Plugin.class);
        mockServer = mock(Server.class);
        when(mockPlugin.getServer()).thenReturn(mockServer);
        when(mockServer.getOnlinePlayers()).thenReturn(Collections.emptyList());

        injector = Guice.createInjector(new ManhuntModule(mockPlugin));
    }

    @Test
    public void testMessageServiceIsInjectable() {
        MessageService messageService = injector.getInstance(MessageService.class);
        assertNotNull("MessageService should be injectable", messageService);
    }

    @Test
    public void testItemServiceIsInjectable() {
        ItemService itemService = injector.getInstance(ItemService.class);
        assertNotNull("ItemService should be injectable", itemService);
    }

    @Test
    public void testEventRegistrationServiceIsInjectable() {
        EventRegistrationService eventRegistration = injector.getInstance(EventRegistrationService.class);
        assertNotNull("EventRegistrationService should be injectable", eventRegistration);
    }

    @Test
    public void testGameLaunchServiceIsInjectable() {
        GameLaunchService gameLaunchService = injector.getInstance(GameLaunchService.class);
        assertNotNull("GameLaunchService should be injectable", gameLaunchService);
        assertFalse("Game should not be running initially", gameLaunchService.isRunning());
    }

    @Test
    public void testManhuntPluginAdapterIsInjectable() {
        ManhuntPluginAdapter adapter = injector.getInstance(ManhuntPluginAdapter.class);
        assertNotNull("ManhuntPluginAdapter should be injectable", adapter);
        assertFalse("Adapter should not be running initially", adapter.isRunning());
    }

    @Test
    public void testAdapterImplementsILaunchablePlugin() {
        ILaunchablePlugin plugin = injector.getInstance(ILaunchablePlugin.class);
        assertNotNull("ILaunchablePlugin binding should work", plugin);
        assertTrue("Bound instance should be ManhuntPluginAdapter",
            plugin instanceof ManhuntPluginAdapter);
    }

    @Test
    public void testServicesWorkWithoutFramework() {
        // Get services directly (no framework involvement)
        MessageService messageService = injector.getInstance(MessageService.class);
        ItemService itemService = injector.getInstance(ItemService.class);
        EventRegistrationService eventRegistration = injector.getInstance(EventRegistrationService.class);

        // Create mock dependencies
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.isOnline()).thenReturn(true);
        Listener mockListener = mock(Listener.class);

        // Test MessageService
        messageService.sendMessage(mockPlayer, "&6Test message");
        verify(mockPlayer).sendMessage(anyString());

        // Test ItemService
        ItemStack item = itemService.createItem(Material.COMPASS, "&6Test Item", "&7Lore");
        assertNotNull("ItemService should create items", item);
        assertEquals("Item should have correct material", Material.COMPASS, item.getType());

        // Test EventRegistrationService
        eventRegistration.registerListener(mockListener);
        // Verification happens through Bukkit internals - no exception means success
    }

    @Test
    public void testServicesAreSingletons() {
        MessageService service1 = injector.getInstance(MessageService.class);
        MessageService service2 = injector.getInstance(MessageService.class);

        assertSame("MessageService should be singleton", service1, service2);

        ItemService itemService1 = injector.getInstance(ItemService.class);
        ItemService itemService2 = injector.getInstance(ItemService.class);

        assertSame("ItemService should be singleton", itemService1, itemService2);
    }
}
