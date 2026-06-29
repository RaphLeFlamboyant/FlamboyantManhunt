package me.flamboyant.manhunt.application.services;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Service for item creation and manipulation.
 * Replaces ItemHelper with framework-independent abstraction.
 */
public interface ItemService extends me.flamboyant.manhunt.domain.services.ItemPort {
    /**
     * Give an item to a player. If inventory is full, drops at player location.
     * @param player target player (must not be null)
     * @param item item to give (must not be null)
     */
    void giveItem(Player player, ItemStack item);

    /**
     * Create an item with display name and lore.
     * @param material item material (must not be null)
     * @param displayName display name with '&' color codes (can be null)
     * @param lore lore lines with '&' color codes
     * @return configured ItemStack
     */
    ItemStack createItem(Material material, String displayName, String... lore);

    /**
     * Check if two items are of the same kind (same material and display name).
     * @param item1 first item (can be null)
     * @param item2 second item (can be null)
     * @return true if both items have same material and display name
     */
    boolean isSameItemKind(ItemStack item1, ItemStack item2);
}
