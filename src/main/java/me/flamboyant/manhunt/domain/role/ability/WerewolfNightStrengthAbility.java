package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class WerewolfNightStrengthAbility extends PassiveAbility {
    private BukkitTask task;
    private boolean powerActivated = false;

    public WerewolfNightStrengthAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // No event handlers, uses scheduled task
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        task = Bukkit.getScheduler().runTaskTimer(
            context.getPlugin(),
            () -> checkNightTime(),
            5 * 20,
            5 * 20
        );
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        super.onRoleStop(context);
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task.getTaskId());
        }
    }

    private void checkNightTime() {
        World world = context.getOwner().getLocation().getWorld();
        long time = world.getTime();

        boolean isNight = world.getName().toLowerCase().contains("end")
            || (!world.getName().toLowerCase().contains("nether") && (time < 1000 || time > 13000));

        if (isNight) {
            setActivationState(true);
            PotionEffect strength = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 7 * 20, 1, false, false);
            context.getOwner().addPotionEffect(strength);

            PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, 25 * 20, 1, false, false);
            context.getOwner().addPotionEffect(nightVision);
        } else {
            setActivationState(false);
        }
    }

    private void setActivationState(boolean isActive) {
        if (powerActivated == isActive) {
            return;
        }

        powerActivated = isActive;
        if (!isActive) {
            context.sendMessage("Vous n'avez plus vos pouvoirs pour le moment. Votre boussole redevient normale.");
            context.getOwner().setCompassTarget(context.getOwner().getBedSpawnLocation());
        } else {
            context.getOwner().setCooldown(Material.COMPASS, 0);
            context.sendMessage("Vous obtenez enfin vos pouvoirs");
        }
    }

    public boolean isPowerActivated() {
        return powerActivated;
    }

    @Override
    public String getName() {
        return "Werewolf Night Strength";
    }

    @Override
    public String getDescription() {
        return "La nuit, obtient Force 1 et Night Vision";
    }
}
