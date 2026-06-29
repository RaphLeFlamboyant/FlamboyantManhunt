package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LinkHunterRole extends HunterRole {
    private static final List<Material> grasses = Arrays.asList(Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.CRIMSON_ROOTS);
    private final Random rng = new Random();

    @Inject
    public LinkHunterRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner, server, plugin, messageService, itemService, eventRegistration);
    }

    @Override
    protected boolean doStop() {
        // Manual unregistration removed - handled by GameLifecycleService
        return super.doStop();
    }

    @Override
    public String getName() {
        return "Link Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. " +
                "Casser des herbes te drop parfois des émeraudes." +
                "Tu fais un bruit courageaux quand tu attaques avec une épée";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_LINK;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() != owner) return;
        if (owner.getInventory().getItemInMainHand() == null
                || owner.getInventory().getItemInMainHand().getType() == Material.AIR) return;
        if (!owner.getInventory().getItemInMainHand().getType().toString().contains("SWORD")) return;

        owner.getWorld().playSound(owner, Sound.ENTITY_VILLAGER_AMBIENT, SoundCategory.VOICE, 1, 1.3f);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        super.onPlayerInteract(event);
        if (event.getPlayer() != owner) return;
        if (!event.hasItem()) return;
        if (!event.getItem().getType().toString().contains("SWORD")) return;

        owner.getWorld().playSound(owner, Sound.ENTITY_VILLAGER_AMBIENT, SoundCategory.VOICE, 1, 1.3f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!grasses.contains(event.getBlock().getType())) return;

        event.setDropItems(false);
        int roll = rng.nextInt(100);
        if (roll > 97)
            event.getPlayer().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.EMERALD, 5));
        if (roll > 24) {
            event.getPlayer().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.EMERALD));
        }
    }
}
