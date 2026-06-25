package me.flamboyant.manhunt.infrastructure.services;

import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BukkitMessageServiceTest {
    private BukkitMessageService messageService;
    private Server mockServer;
    private Player mockPlayer;

    @Before
    public void setup() {
        mockServer = mock(Server.class);
        mockPlayer = mock(Player.class);
        when(mockPlayer.isOnline()).thenReturn(true);
        messageService = new BukkitMessageService(mockServer);
    }

    @Test
    public void testSendMessage_translatesColorCodes() {
        messageService.sendMessage(mockPlayer, "&6Hello &aWorld");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer).sendMessage(captor.capture());

        String sent = captor.getValue();
        assertTrue("Message should contain color codes", sent.contains(ChatColor.GOLD.toString()));
        assertTrue("Message should contain 'Hello'", sent.contains("Hello"));
        assertTrue("Message should contain 'World'", sent.contains("World"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendMessage_nullPlayer_throwsException() {
        messageService.sendMessage(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendMessage_nullMessage_throwsException() {
        messageService.sendMessage(mockPlayer, null);
    }

    @Test
    public void testSendMessage_offlinePlayer_logsWarning() {
        when(mockPlayer.isOnline()).thenReturn(false);
        when(mockPlayer.getName()).thenReturn("TestPlayer");

        // Should not throw exception
        messageService.sendMessage(mockPlayer, "test");

        // Verify message was not sent
        verify(mockPlayer, never()).sendMessage(anyString());
    }

    @Test
    public void testBroadcastMessage_sendsToServer() {
        messageService.broadcastMessage("&cTest broadcast");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockServer).broadcastMessage(captor.capture());

        String sent = captor.getValue();
        assertTrue("Broadcast should contain color codes", sent.contains(ChatColor.RED.toString()));
        assertTrue("Broadcast should contain message", sent.contains("Test broadcast"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBroadcastMessage_nullMessage_throwsException() {
        messageService.broadcastMessage(null);
    }

    @Test
    public void testSendFormattedMessage_formatsAndSends() {
        messageService.sendFormattedMessage(mockPlayer, "&6Score: %d, Rank: %s", 100, "Gold");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer).sendMessage(captor.capture());

        String sent = captor.getValue();
        assertTrue("Message should contain formatted score", sent.contains("100"));
        assertTrue("Message should contain formatted rank", sent.contains("Gold"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendFormattedMessage_nullFormat_throwsException() {
        messageService.sendFormattedMessage(mockPlayer, null, "arg");
    }
}
