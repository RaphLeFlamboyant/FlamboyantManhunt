package me.flamboyant.manhunt.domain.services;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Domain port for item management.
 * Implemented by application layer ItemService.
 */
public interface ItemPort {
    void giveItem(Player player, ItemStack item);
    boolean hasItem(Player player, Material material);
}
