package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProMinerAbility extends PassiveAbility {
    private static final List<Material> CONCERNED_BLOCKS = Arrays.asList(
        Material.STONE,
        Material.DEEPSLATE,
        Material.NETHERRACK
    );

    private static final List<Material> ITEMS_TO_DROP = Arrays.asList(
        Material.COAL,
        Material.RAW_COPPER,
        Material.RAW_IRON,
        Material.RAW_GOLD,
        Material.GOLD_NUGGET,
        Material.EMERALD,
        Material.QUARTZ,
        Material.LAPIS_LAZULI,
        Material.REDSTONE
    );

    private final Random rng = new Random();

    public ProMinerAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }

    private void onBlockBreak(BlockBreakEvent event) {
        if (!CONCERNED_BLOCKS.contains(event.getBlock().getType())) {
            return;
        }

        if (rng.nextInt(100) > 6) {
            return;
        }

        event.setDropItems(false);
        Material randomOre = ITEMS_TO_DROP.get(rng.nextInt(ITEMS_TO_DROP.size()));
        context.getOwner().getWorld().dropItem(
            event.getBlock().getLocation(),
            new ItemStack(randomOre, 1)
        );
    }

    @Override
    public String getName() {
        return "Pro Miner";
    }

    @Override
    public String getDescription() {
        return "Parfois en minant de la roche, obtient du minerai";
    }
}
