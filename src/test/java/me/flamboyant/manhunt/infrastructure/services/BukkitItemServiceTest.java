package me.flamboyant.manhunt.infrastructure.services;

import me.flamboyant.manhunt.application.services.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BukkitItemServiceTest {
    private BukkitItemService itemService;
    private Player mockPlayer;
    private PlayerInventory mockInventory;
    private World mockWorld;

    @Before
    public void setup() {
        itemService = new BukkitItemService();
        mockPlayer = mock(Player.class);
        mockInventory = mock(PlayerInventory.class);
        mockWorld = mock(World.class);

        when(mockPlayer.getInventory()).thenReturn(mockInventory);
        when(mockPlayer.isOnline()).thenReturn(true);
        when(mockPlayer.getWorld()).thenReturn(mockWorld);
    }

    @Test
    public void testGiveItem_addsToInventory() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        when(mockInventory.addItem(item)).thenReturn(new HashMap<>());

        itemService.giveItem(mockPlayer, item);

        verify(mockInventory).addItem(item);
        verify(mockWorld, never()).dropItem(any(), any());
    }

    @Test
    public void testGiveItem_fullInventory_dropsAtLocation() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        HashMap<Integer, ItemStack> overflow = new HashMap<>();
        overflow.put(0, item);
        when(mockInventory.addItem(item)).thenReturn(overflow);
        when(mockPlayer.getLocation()).thenReturn(null); // Location doesn't matter for this test

        itemService.giveItem(mockPlayer, item);

        verify(mockInventory).addItem(item);
        verify(mockWorld).dropItem(any(), eq(item));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGiveItem_nullPlayer_throwsException() {
        itemService.giveItem(null, new ItemStack(Material.DIAMOND));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGiveItem_nullItem_throwsException() {
        itemService.giveItem(mockPlayer, null);
    }

    @Test
    public void testGiveItem_offlinePlayer_logsWarning() {
        when(mockPlayer.isOnline()).thenReturn(false);
        when(mockPlayer.getName()).thenReturn("TestPlayer");

        ItemStack item = new ItemStack(Material.DIAMOND);
        itemService.giveItem(mockPlayer, item);

        verify(mockInventory, never()).addItem(any());
    }

    @Test
    public void testCreateItem_setsDisplayNameAndLore() {
        ItemStack item = itemService.createItem(Material.COMPASS, "&6Tracker", "&7Line 1", "&aLine 2");

        assertNotNull("Item should not be null", item);
        assertEquals("Material should be COMPASS", Material.COMPASS, item.getType());

        ItemMeta meta = item.getItemMeta();
        assertNotNull("ItemMeta should not be null", meta);

        String displayName = meta.getDisplayName();
        assertTrue("Display name should contain 'Tracker'", displayName.contains("Tracker"));

        List<String> lore = meta.getLore();
        assertNotNull("Lore should not be null", lore);
        assertEquals("Should have 2 lore lines", 2, lore.size());
        assertTrue("First lore line should contain 'Line 1'", lore.get(0).contains("Line 1"));
        assertTrue("Second lore line should contain 'Line 2'", lore.get(1).contains("Line 2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateItem_nullMaterial_throwsException() {
        itemService.createItem(null, "Name");
    }

    @Test
    public void testCreateItem_nullDisplayName_works() {
        ItemStack item = itemService.createItem(Material.DIAMOND, null);

        assertNotNull("Item should not be null", item);
        assertEquals("Material should be DIAMOND", Material.DIAMOND, item.getType());
    }
}
