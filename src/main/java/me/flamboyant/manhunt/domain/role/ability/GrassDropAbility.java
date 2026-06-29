package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GrassDropAbility extends PassiveAbility {
    private static final List<Material> GRASSES = Arrays.asList(
        Material.GRASS,
        Material.TALL_GRASS,
        Material.SEAGRASS,
        Material.TALL_SEAGRASS,
        Material.WARPED_ROOTS,
        Material.NETHER_SPROUTS,
        Material.CRIMSON_ROOTS
    );
    private final Random rng = new Random();

    public GrassDropAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(BlockBreakEvent.class, this::onBlockBreak);
    }

    private void onBlockBreak(BlockBreakEvent event) {
        if (!GRASSES.contains(event.getBlock().getType())) {
            return;
        }

        event.setDropItems(false);
        int roll = rng.nextInt(100);
        Location dropLocation = event.getBlock().getLocation();

        if (roll > 93) {
            dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, 10));
        } else if (roll > 24) {
            dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, 2));
        }
    }

    @Override
    public String getName() {
        return "Grass Emerald Drop";
    }

    @Override
    public String getDescription() {
        return "Casser des herbes drop des émeraudes";
    }
}
