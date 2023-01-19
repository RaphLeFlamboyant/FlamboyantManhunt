package me.flamboyant.gamemodes.newmanhunt.roles.impl;

import me.flamboyant.common.utils.Common;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ProMinerRole extends HunterRole {
    private List<Material> concernedBlocks = Arrays.asList(Material.STONE, Material.DEEPSLATE, Material.NETHERRACK);
    private List<Material> itemToDrop = Arrays.asList(Material.COAL, Material.RAW_COPPER, Material.RAW_IRON, Material.RAW_GOLD, Material.GOLD_NUGGET, Material.EMERALD, Material.QUARTZ, Material.LAPIS_LAZULI, Material.REDSTONE);

    public ProMinerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        BlockBreakEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected String getName() {
        return "Pro Miner Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. Parfois en minant de la roche, de la " +
                "deepslate ou de la netherack, tu obtiens du minerai.";
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!concernedBlocks.contains(event.getBlock().getType())) return;
        if (Common.rng.nextInt(100) > 6) return;

        event.setDropItems(false);
        owner.getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(itemToDrop.get(Common.rng.nextInt(itemToDrop.size())), 1));
    }
}
