package me.flamboyant.gamemodes.newmanhunt.roles.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Hashtable;

public class CutCleanHunterRole extends HunterRole {
    private Hashtable<Material, Material> oreToCutClean = new Hashtable<Material, Material>() {{
       put(Material.COPPER_ORE, Material.COPPER_INGOT);
        put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        put(Material.IRON_ORE, Material.IRON_INGOT);
        put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        put(Material.GOLD_ORE, Material.GOLD_INGOT);
        put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        put(Material.RAW_IRON_BLOCK, Material.IRON_BLOCK);
        put(Material.RAW_GOLD_BLOCK, Material.GOLD_BLOCK);
        put(Material.RAW_COPPER_BLOCK, Material.COPPER_BLOCK);
    }};

    public CutCleanHunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected String getName() {
        return "Cut Clean Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. Tous les minerais sont récoltés cuits.";
    }

    @Override
    protected boolean doStop() {
        BlockBreakEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!oreToCutClean.containsKey(event.getBlock().getType())) return;
        Bukkit.getLogger().info("DROP ITEM : " + event.isDropItems());
        if (!event.isDropItems()) return;

        event.setDropItems(false);
        owner.getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(oreToCutClean.get(event.getBlock().getType()), 1));
    }
}
