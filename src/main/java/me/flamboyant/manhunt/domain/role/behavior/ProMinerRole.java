package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProMinerRole extends HunterRole {
    private List<Material> concernedBlocks = Arrays.asList(Material.STONE, Material.DEEPSLATE, Material.NETHERRACK);
    private List<Material> itemToDrop = Arrays.asList(Material.COAL, Material.RAW_COPPER, Material.RAW_IRON, Material.RAW_GOLD, Material.GOLD_NUGGET, Material.EMERALD, Material.QUARTZ, Material.LAPIS_LAZULI, Material.REDSTONE);
    private final Random rng = new Random();

    @Inject
    public ProMinerRole(
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
        return "Pro Miner Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                "te donne sa position. Parfois en minant de la roche, de la " +
                "deepslate ou de la netherack, tu obtiens du minerai.";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_PRO_MINER;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!concernedBlocks.contains(event.getBlock().getType())) return;
        if (rng.nextInt(100) > 6) return;

        event.setDropItems(false);
        owner.getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(itemToDrop.get(rng.nextInt(itemToDrop.size())), 1));
    }
}
