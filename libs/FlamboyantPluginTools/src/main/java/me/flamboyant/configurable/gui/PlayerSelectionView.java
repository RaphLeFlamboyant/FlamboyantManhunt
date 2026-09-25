package me.flamboyant.configurable.gui;

import me.flamboyant.configurable.gui.items.PlayerSelectionParameterItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerSelectionView implements Listener {
    private static final int inventorySize = 9 * 6;
    private List<ItemStack> playerItems = new ArrayList<>();
    private Inventory view;
    private PlayerSelectionParameterItem playerParameters;

    public PlayerSelectionView(Player[] possibleValues, PlayerSelectionParameterItem playerParameters) {
        this.playerParameters = playerParameters;

        view = Bukkit.createInventory(null, inventorySize, getViewId());
        int index = 0;
        for (Player player : possibleValues) {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) playerHead.getItemMeta();
            skull.setDisplayName(player.getDisplayName());
            skull.setLore(Arrays.asList(getValueString(false)));
            skull.setOwningPlayer(player);
            playerHead.setItemMeta(skull);
            addItemSelectionVisual(playerHead);
            playerItems.add(playerHead);

            view.setItem((inventorySize / 2) - (possibleValues.length / 2) + index, playerHead);
            index++;
        }

        ItemStack allPlayersItem = new ItemStack(Material.LEVER);
        ItemMeta meta = allPlayersItem.getItemMeta();
        meta.setDisplayName("Tous les joueurs");
        meta.setLore(Arrays.asList(getValueString(false)));
        allPlayersItem.setItemMeta(meta);
        view.setItem(inventorySize - 1, allPlayersItem);
        if (playerParameters.isAllPlayers()) onAllPlayersSelected(allPlayersItem);

        ItemStack previous = new ItemStack(Material.REDSTONE);
        previous.addUnsafeEnchantment(Enchantment.ARROW_FIRE, 1);
        meta = previous.getItemMeta();
        meta.setDisplayName("Revenir au menu");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        previous.setItemMeta(meta);
        view.setItem(0, allPlayersItem);
    }

    public String getViewId() {
        return "Sélection des joueurs";
    }

    public Inventory getView() {
        return view;
    }

    private String getValueString(boolean value) {
        return value ? "YES" : "NO";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory != view) return;
        event.setCancelled(true);
        if (event.getSlotType() == InventoryType.SlotType.QUICKBAR) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName.equals("Tous les joueurs")) {
            if (event.isLeftClick()) onAllPlayersSelected(clicked);
            if (event.isRightClick()) onAllPlayersUnselected(clicked);
        }
        else if (displayName.equals("Revenir au menu")) {
            event.getWhoClicked().closeInventory();
        }
        else {
            if (event.isLeftClick()) onPlayerSelected(clicked);
            if (event.isRightClick()) onPlayerUnselected(clicked);
        }
    }

    private void onPlayerSelected(ItemStack playerItem) {
        playerParameters.doLeftClickOnPlayer(playerItem.getItemMeta().getDisplayName());
        addItemSelectionVisual(playerItem);
    }

    private void onPlayerUnselected(ItemStack playerItem) {
        playerParameters.doRightClickOnPlayer(playerItem.getItemMeta().getDisplayName());
        removeItemSelectionVisual(playerItem);
    }

    private void onAllPlayersSelected(ItemStack allPlayersItem) {
        playerParameters.doRightClickAllPlayers();
        addItemSelectionVisual(allPlayersItem);

        for (ItemStack playerItem : playerItems) {
            addItemSelectionVisual(playerItem);
            playerParameters.doRightClickOnPlayer(playerItem.getItemMeta().getDisplayName());
        }
    }

    private void onAllPlayersUnselected(ItemStack allPlayersItem) {
        playerParameters.doLeftClickAllPlayers();
        removeItemSelectionVisual(allPlayersItem);
    }

    private void addItemSelectionVisual(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.ARROW_FIRE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(Arrays.asList("YES"));
        item.setItemMeta(meta);
    }

    private void removeItemSelectionVisual(ItemStack item) {
        item.removeEnchantment(Enchantment.ARROW_FIRE);
    }

    public void close() {
        InventoryClickEvent.getHandlerList().unregister(this);
        playerItems.clear();

        if (view != null) view.clear();
        view = null;
    }
}
