package me.flamboyant.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.List;

public class ItemHelper {
    public static ItemStack generateItem(Material material,
                                         int quantity,
                                         String displayName,
                                         List<String> lore,
                                         Boolean addEnchant,
                                         Enchantment enchant,
                                         int enchantLevel,
                                         Boolean hideEnchant,
                                         Boolean hideAttributes) {
        ItemStack custom = new ItemStack(material, quantity);
        ItemMeta customM = custom.getItemMeta();
        customM.setDisplayName(displayName);
        customM.setLore(lore);
        if (addEnchant) customM.addEnchant(enchant, enchantLevel, true);
        if (hideEnchant) customM.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (hideAttributes) customM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        custom.setItemMeta(customM);

        return custom;
    }

    public static ItemStack generateItem(Material material,
                                         int quantity,
                                         String displayName,
                                         List<String> lore,
                                         Boolean addEnchant,
                                         Enchantment enchant,
                                         Boolean hideEnchant,
                                         Boolean hideAttributes) {
        return generateItem(material, quantity, displayName, lore, addEnchant, enchant, 0, hideEnchant, hideAttributes);
    }

    public static ItemStack generatePotion(PotionType potionType,
                                           boolean isSplash,
                                           boolean isExtended,
                                           boolean isUpgraded,
                                           String displayName,
                                           List<String> lore,
                                           Boolean hideAttributes) {
        ItemStack custom = new ItemStack(isSplash ? Material.SPLASH_POTION : Material.POTION, 1);
        PotionMeta customM = (PotionMeta) custom.getItemMeta();
        customM.setDisplayName(displayName);
        customM.setLore(lore);
        customM.setBasePotionData(new PotionData(potionType, isExtended, isUpgraded));
        custom.setItemMeta(customM);

        return custom;
    }

    public static boolean isExactlySameItemKind(ItemStack item, ItemStack reference) {
        if (item == null && reference == null) return true;
        if (item == null || reference == null) return false;

        boolean result = true;
        result &= item.getAmount() == reference.getAmount();
        result &= isSameItemKind(item, reference);

        return result;
    }

    public static boolean isSameItemKind(ItemStack item, ItemStack reference) {
        if (item == null && reference == null) return true;
        if (item == null || reference == null) return false;

        boolean result = true;
        result &= item.getType() == reference.getType();
        result &= item.getEnchantments().size() == reference.getEnchantments().size();

        for (Enchantment ench : item.getEnchantments().keySet()) {
            result &= reference.getEnchantments().containsKey(ench);
        }

        return result;
    }

    public static List<Material> forbiddenMaterials = Arrays.asList(
            Material.AIR,
            Material.DEBUG_STICK,
            Material.END_PORTAL_FRAME,
            Material.LIGHT,
            Material.VOID_AIR,
            Material.COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID);

    public static Material getRandomLegitMaterial() {
        Material res;

        do {
            res = Material.values()[Common.rng.nextInt(Material.values().length)];
        } while (forbiddenMaterials.contains(res));

        return res;
    }
}
