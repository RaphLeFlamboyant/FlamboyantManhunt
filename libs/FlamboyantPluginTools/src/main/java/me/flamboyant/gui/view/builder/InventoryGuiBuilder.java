package me.flamboyant.gui.view.builder;

import me.flamboyant.gui.view.icons.IIconItem;
import me.flamboyant.gui.view.common.InventoryGui;
import me.flamboyant.gui.view.icons.impl.PageSwapIconItem;
import me.flamboyant.utils.ItemHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryGuiBuilder implements IInventoryGuiBuilder {
    private static ItemStack emptyItem = new ItemStack(Material.AIR);

    @Override
    public InventoryGui buildView(List<IIconItem> icons, String name, int inventorySize, boolean forceAction) {
        return buildView(icons, name, inventorySize, ItemGroupingMode.PACKED, forceAction);
    }

    @Override
    public InventoryGui buildView(List<IIconItem> icons, String name, int inventorySize, ItemGroupingMode groupingMode, boolean forceAction) {
        List<ItemStack> items = generateViewItems(icons, inventorySize, groupingMode);
        List<IIconItem> finalIconList = new ArrayList<>();
        List<Inventory> pages = fillGui(items, name, inventorySize, finalIconList);
        finalIconList.addAll(icons);

        return new InventoryGui(name, pages, icons, forceAction);
    }

    private List<ItemStack> generateViewItems(List<IIconItem> icons, int inventorySize, ItemGroupingMode groupingMode) {
        List<ItemStack> res = new ArrayList<>();
        HashMap<String, List<ItemStack>> groupedItems = byCategoryItems(icons);

        switch (groupingMode) {
            case PACKED:
                res = icons.stream().map(i -> i.getItem()).collect(Collectors.toList());
                if (icons.size() <= inventorySize) {
                    int freeSpace = inventorySize - icons.size();
                    for (int i = freeSpace / 2; i > 0; i--) {
                        res.add(0, emptyItem);
                    }
                }
                break;
            case PARTED:
                for (String category : groupedItems.keySet()) {
                    res.add(emptyItem);
                    res.addAll(groupedItems.get(category));
                }
                if (icons.size() <= inventorySize) {
                    int freeSpace = inventorySize - icons.size();
                    for (int i = freeSpace / 2; i > 0; i--) {
                        res.add(0, emptyItem);
                    }
                }
                break;
            case EXCLUSIVE_LINE:
                for (String category : groupedItems.keySet()) {
                    res.addAll(groupedItems.get(category));
                    while (res.size() % 9 != 0) {
                        res.add(emptyItem);
                    }
                }
                break;
            case EXCLUSIVE_PAGE:
                if (groupedItems.size() == 1)
                    return generateViewItems(icons, inventorySize, ItemGroupingMode.PACKED);
                for (String category : groupedItems.keySet()) {
                    res.addAll(groupedItems.get(category));
                    while (res.size() % (inventorySize - 9) != 0) {
                        res.add(emptyItem);
                    }
                }
                break;
        }

        return res;
    }

    private HashMap<String, List<ItemStack>> byCategoryItems(List<IIconItem> icons) {
        HashMap<String, List<ItemStack>> iconsByCategory = new HashMap<>();
        for (IIconItem icon : icons) {
            if (!iconsByCategory.containsKey(icon.getCategory()))
                iconsByCategory.put(icon.getCategory(), new ArrayList<>());

            iconsByCategory.get(icon.getCategory()).add(icon.getItem());
        }

        return iconsByCategory;
    }

    private List<Inventory> fillGui(List<ItemStack> items, String name, int inventorySize, List<IIconItem> outIconsGenerated) {
        ArrayList<Inventory> pages = new ArrayList<>();
        Inventory currentInventory = Bukkit.createInventory(null, inventorySize, name);
        pages.add(currentInventory);

        if (items.size() <= inventorySize) {
            for (int i = 0; i < items.size(); i++) {
                currentInventory.setItem(i, items.get(i));
            }

            return pages;
        }

        int index = 0;
        for (ItemStack item : items) {
            if (index == inventorySize - 9) {
                Inventory nextInventory = Bukkit.createInventory(null, inventorySize, name);
                pages.add(nextInventory);
                PageSwapIconItem nextIcon = generateNextPageItem(nextInventory);
                PageSwapIconItem previousIcon = generatePreviousPageItem(currentInventory);

                outIconsGenerated.add(nextIcon);
                outIconsGenerated.add(previousIcon);

                currentInventory.setItem(inventorySize - 1, nextIcon.getItem());
                nextInventory.setItem(inventorySize - 9, previousIcon.getItem());

                currentInventory = nextInventory;
                index = 0;
            }

            currentInventory.setItem(index++, item);
        }

        return pages;
    }


    private PageSwapIconItem generateNextPageItem(Inventory target) {
        ItemStack item = ItemHelper.generateItem(Material.PAPER, 1, "Page suivante", Arrays.asList(), false, null, true, false);
        return new PageSwapIconItem(item, target);
    }

    private PageSwapIconItem generatePreviousPageItem(Inventory target) {
        ItemStack item = ItemHelper.generateItem(Material.PAPER, 1, "Page suivante", Arrays.asList(), false, null, true, false);
        return new PageSwapIconItem(item, target);
    }
}
