package me.flamboyant.manhunt.application.services;

import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.HandlersRegisteredEvent;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EventHandlerRegistrationServiceTest {

    private Plugin mockPlugin;
    private PluginManager mockPluginManager;
    private DomainEventPublisher mockPublisher;
    private EventHandlerRegistrationService service;

    @Before
    public void setUp() {
        mockPlugin = mock(Plugin.class);
        mockPluginManager = mock(PluginManager.class);
        mockPublisher = mock(DomainEventPublisher.class);

        // Mock static Bukkit.getPluginManager()
        when(mockPlugin.getServer()).thenReturn(mock(org.bukkit.Server.class));
        when(mockPlugin.getServer().getPluginManager()).thenReturn(mockPluginManager);

        service = new EventHandlerRegistrationService(mockPlugin, mockPublisher);
    }

    @Test
    public void registerHandlers_shouldRegisterListeners() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Listener listener1 = mock(Listener.class);
        Listener listener2 = mock(Listener.class);

        // Act
        HandlerRegistration registration = service.registerHandlers(sessionId, listener1, listener2);

        // Assert
        assertNotNull(registration);
        assertEquals(sessionId, registration.getSessionId());
        assertEquals(2, registration.getListeners().size());

        verify(mockPluginManager).registerEvents(listener1, mockPlugin);
        verify(mockPluginManager).registerEvents(listener2, mockPlugin);
    }

    @Test
    public void registerHandlers_shouldPublishEvent() {
        // Arrange
        GameSessionId sessionId = GameSessionId.generate();
        Listener listener = mock(Listener.class);

        // Act
        service.registerHandlers(sessionId, listener);

        // Assert
        ArgumentCaptor<HandlersRegisteredEvent> eventCaptor =
            ArgumentCaptor.forClass(HandlersRegisteredEvent.class);
        verify(mockPublisher).publish(eventCaptor.capture());

        HandlersRegisteredEvent event = eventCaptor.getValue();
        assertEquals(sessionId, event.getSessionId());
    }
}
