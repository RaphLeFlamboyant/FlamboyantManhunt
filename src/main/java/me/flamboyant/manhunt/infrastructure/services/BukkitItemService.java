package me.flamboyant.manhunt.infrastructure.services;

import me.flamboyant.manhunt.application.services.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Bukkit implementation of ItemService.
 * Uses only Bukkit APIs for item creation and manipulation.
 */
@Singleton
public class BukkitItemService implements ItemService {

    @Override
    public void giveItem(Player player, ItemStack item) {
        if (player == null || item == null) {
            throw new IllegalArgumentException("Player and item cannot be null");
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning("Attempted to give item to offline player: " + player.getName());
            return;
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack extra : overflow.values()) {
                player.getWorld().dropItem(player.getLocation(), extra);
            }
        }
    }

    @Override
    public ItemStack createItem(Material material, String displayName, String... lore) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            if (lore.length > 0) {
                meta.setLore(Arrays.stream(lore)
                    .filter(line -> line != null)
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}
