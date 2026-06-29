package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CutCleanAbility extends PassiveAbility {
    private static final Map<Material, Material> SMELTING_MAP = new HashMap<>();

    static {
        SMELTING_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELTING_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELTING_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELTING_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELTING_MAP.put(Material.SAND, Material.GLASS);
        SMELTING_MAP.put(Material.NETHERRACK, Material.NETHER_BRICK);
        // Meat
        SMELTING_MAP.put(Material.BEEF, Material.COOKED_BEEF);
        SMELTING_MAP.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        SMELTING_MAP.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        SMELTING_MAP.put(Material.MUTTON, Material.COOKED_MUTTON);
        SMELTING_MAP.put(Material.RABBIT, Material.COOKED_RABBIT);
        SMELTING_MAP.put(Material.COD, Material.COOKED_COD);
        SMELTING_MAP.put(Material.SALMON, Material.COOKED_SALMON);
    }

    public CutCleanAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
        context.registerEventHandler(EntityDeathEvent.class, this::onEntityDeath);
    }

    private void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();
        Material smelted = SMELTING_MAP.get(blockType);

        if (smelted != null) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation(),
                new ItemStack(smelted, 1)
            );
        }
    }

    private void onEntityDeath(EntityDeathEvent event) {
        List<ItemStack> newDrops = new ArrayList<>();

        for (ItemStack drop : event.getDrops()) {
            Material smelted = SMELTING_MAP.get(drop.getType());
            if (smelted != null) {
                newDrops.add(new ItemStack(smelted, drop.getAmount()));
            } else {
                newDrops.add(drop);
            }
        }

        event.getDrops().clear();
        event.getDrops().addAll(newDrops);
    }

    @Override
    public String getName() {
        return "CutClean";
    }

    @Override
    public String getDescription() {
        return "Les minerais et la nourriture sont automatiquement cuits";
    }
}
