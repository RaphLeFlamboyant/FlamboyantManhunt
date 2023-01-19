package me.flamboyant.gamemodes.newmanhunt.roles.impl;

import me.flamboyant.common.utils.Common;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class LinkHunterRole extends HunterRole {
    private static final List<Material> grasses = Arrays.asList(Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.CRIMSON_ROOTS);

    public LinkHunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        BlockBreakEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected String getName() {
        return "Link Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. " +
                "Casser des herbes te drop parfois des émeraudes." +
                "Tu fais un bruit courageaux quand tu attaques avec une épée";
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != owner) return;
        if (!event.hasItem()) return;
        if (!event.getItem().getType().toString().contains("SWORD")) return;

        owner.getWorld().playSound(owner, Sound.ENTITY_VILLAGER_AMBIENT, SoundCategory.VOICE, 1, 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!grasses.contains(event.getBlock().getType())) return;

        event.setDropItems(false);
        int roll = Common.rng.nextInt(100);
        if (roll > 97)
            event.getPlayer().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.EMERALD, 5));
        if (roll > 24) {
            event.getPlayer().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.EMERALD));
        }
    }
}
