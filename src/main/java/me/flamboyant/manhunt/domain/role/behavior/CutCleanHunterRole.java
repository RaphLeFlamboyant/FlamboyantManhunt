package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

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

    @Inject
    public CutCleanHunterRole(
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
    public String getName() {
        return "Cut Clean Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. Tous les minerais sont récoltés cuits.";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_CUTCLEAN;
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
