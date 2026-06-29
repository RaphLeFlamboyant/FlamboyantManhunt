package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class CutCleanSpeedrunnerRole extends SpeedrunnerRole {
    private HashSet<Entity> monstersKilledByPlayer = new HashSet<>();
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
        put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
    }};

    private Hashtable<Material, Material> meatToCookedMeat = new Hashtable<Material, Material>() {{
        put(Material.CHICKEN, Material.COOKED_CHICKEN);
        put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        put(Material.BEEF, Material.COOKED_BEEF);
        put(Material.MUTTON, Material.COOKED_MUTTON);
        put(Material.RABBIT, Material.COOKED_RABBIT);
        put(Material.SALMON, Material.COOKED_SALMON);
        put(Material.COD, Material.COOKED_COD);
    }};

    @Inject
    public CutCleanSpeedrunnerRole(
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
        return "Cutclean Speedrunner";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() + " Toutes les viandes et tous les minerais sont récoltés cuits";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.SPEEDRUNNER_CUTCLEAN;
    }

    @Override
    protected boolean doStop() {
        BlockBreakEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        monstersKilledByPlayer.clear();
        return super.doStop();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        if (!oreToCutClean.containsKey(event.getBlock().getType())) return;
        if (!event.isDropItems()) return;

        event.setDropItems(false);
        owner.getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(oreToCutClean.get(event.getBlock().getType()), 1));
    }

    @EventHandler
    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        super.onEntityDamage(event);
        if (event.getEntity().getType() == EntityType.PLAYER || !(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity ety = (LivingEntity)event.getEntity();
        if (ety.getHealth() - event.getFinalDamage() <= 0) {
            Bukkit.getLogger().warning("Le speedrunner a tué un " + ety.getType());
            monstersKilledByPlayer.add(ety);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Bukkit.getLogger().info("onEntityDeath : " + event.getEntity().getType());
        if (!monstersKilledByPlayer.contains(event.getEntity()))
            return;

        monstersKilledByPlayer.remove(event.getEntity());
        List<ItemStack> dropList = event.getDrops();
        for (ItemStack item : dropList) {
            if (meatToCookedMeat.containsKey(item.getType())) {
                item.setType(meatToCookedMeat.get(item.getType()));
            }
        }
    }
}
