package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.List;

public class WerewolfSpeedrunnerRole extends SpeedrunnerRole {
    protected List<Player> hunterList;
    protected int targetIndex = 0;
    private BukkitTask task;
    private boolean powerActivated = false;

    @Inject
    public WerewolfSpeedrunnerRole(
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
        return "Speedrunner Garou";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "La nuit tu obtiens Force 1, Night Vision et tu peux détecter les hunters avec " +
                "une boussole toutes les 30 secondes";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.SPEEDRUNNER_WEREWOLF;
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(task.getTaskId());

        return super.doStop();
    }

    @Override
    protected boolean doStart() {
        ItemStack item = new ItemStack(Material.COMPASS);
        owner.getInventory().addItem(item);

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = owner.getLocation().getWorld();
            long time = world.getTime();
            if (world.getName().toLowerCase().contains("end")
                    || !world.getName().toLowerCase().contains("nether") && (time < 1000 || time > 13000)) {
                setActivationState(true);
                PotionEffect effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 7 * 20, 1, false, false);
                owner.addPotionEffect(effect);
                effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 25 * 20, 1, false, false);
                owner.addPotionEffect(effect);
            }
            else setActivationState(false);
        }, 5 * 20, 5 * 20);

        return super.doStart();
    }

    @Override
    protected void addCompassCooldown() {
        if (powerActivated)
            owner.setCooldown(Material.COMPASS, 30 * 20);
        else
            owner.setCooldown(Material.COMPASS, 15 * 60 * 20);
    }

    private void setActivationState(boolean isActive) {
        if (powerActivated == isActive) return;

        powerActivated = isActive;
        if (!isActive) {
            owner.sendMessage(messageService.feedback("Vous n'avez plus vos pouvoirs pour le moment. Votre boussole redevient normale."));
            owner.setCompassTarget(owner.getBedSpawnLocation());
        }
        else {
            owner.setCooldown(Material.COMPASS, 0);
            owner.sendMessage(messageService.feedback("Vous obtenez enfin vos pouvoirs"));
        }
    }
}
