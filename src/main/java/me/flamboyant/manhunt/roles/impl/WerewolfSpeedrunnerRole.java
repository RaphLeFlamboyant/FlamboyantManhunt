package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ChatColorUtils;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class WerewolfSpeedrunnerRole extends SpeedrunnerRole {
    protected List<Player> hunterList;
    protected int targetIndex = 0;
    private BukkitTask task;
    private boolean powerActivated = false;

    public WerewolfSpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected String getName() {
        return "Speedrunner Garou";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "La nuit tu obtiens Force 1, Night Vision et tu peux détecter les hunters avec " +
                "une boussole toutes les 30 secondes";
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

        task = Bukkit.getScheduler().runTaskTimer(Common.plugin, () -> {
            World world = owner.getLocation().getWorld();
            long time = world.getTime();
            if (world.getName().toLowerCase().contains("end")
                    || !world.getName().toLowerCase().contains("nether") && (time < 12300 || time > 23850)) {
                setActivationState(true);
                PotionEffect effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 7 * 20, 1, false, false);
                owner.addPotionEffect(effect);
                effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 7 * 20, 1, false, false);
                owner.addPotionEffect(effect);
            }
            else setActivationState(false);
        }, 5 * 20, 5 * 20);

        return super.doStart();
    }

    @Override
    protected void addCompassCooldown() {
        owner.setCooldown(Material.COMPASS, 30 * 20);
    }

    private void setActivationState(boolean isActive) {
        if (powerActivated == isActive) return;

        powerActivated = isActive;
        if (!isActive) {
            owner.sendMessage(ChatColorUtils.feedback("Vous n'avez plus vos pouvoirs pour le moment. Votre boussole redevient normale."));
            owner.setCompassTarget(owner.getBedSpawnLocation());
        }
        else {
            owner.setCooldown(Material.COMPASS, 0);
            owner.sendMessage(ChatColorUtils.feedback("Vous obtenez enfin vos pouvoirs"));
        }
    }
}
