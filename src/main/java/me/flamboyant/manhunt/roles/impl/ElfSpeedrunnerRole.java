package me.flamboyant.gamemodes.newmanhunt.roles.impl;

import me.flamboyant.common.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class ElfSpeedrunnerRole extends SpeedrunnerRole {
    private static final int degres = 3;
    private static final List<Biome> regenBiomes = Arrays.asList(Biome.FOREST, Biome.BIRCH_FOREST, Biome.WARPED_FOREST, Biome.CRIMSON_FOREST, Biome.OLD_GROWTH_BIRCH_FOREST, Biome.DARK_FOREST, Biome.WINDSWEPT_FOREST, Biome.FLOWER_FOREST, Biome.LUSH_CAVES, Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.SNOWY_TAIGA, Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE, Biome.MANGROVE_SWAMP);
    private BukkitTask regenTask;

    public ElfSpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStart() {
        regenTask = Bukkit.getScheduler().runTaskTimer(Common.plugin, () -> {
            if (regenBiomes.contains(owner.getLocation().getWorld().getBiome(owner.getLocation())))
                owner.setHealth(owner.getHealth() + 1);
        }, 5 * 20, 5 * 20);

        return super.doStart();
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(regenTask.getTaskId());
        EntityDamageEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected String getName() {
        return "Speedrunner Elfe";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "Tirer à l'arc envoie une salve de 5 flèches. " +
                "Tirer à l'abalète t'inflige des dégâts. " +
                "Régénration auto dans les biomes forêts et lush cave.";
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() != owner) return;
        if (event.getBow().getType() == Material.CROSSBOW) {
            owner.damage(2);
            return;
        }

        AbstractArrow projectile = (AbstractArrow) event.getProjectile();
        Vector velocity = projectile.getVelocity();
        Vector fakeVector = new Vector(velocity.getX(), 0, velocity.getZ());
        double length = fakeVector.length();
        fakeVector = fakeVector.normalize();
        double cos = fakeVector.getX();
        double sin = fakeVector.getZ();
        double angle = Math.toDegrees(sin < 0 ? Math.acos(cos) * -1 : Math.acos(cos));

        for (int i = -2; i < 3; i++){
            if (i == 0) continue;

            double newAngle = angle + (degres * i);
            cos = Math.cos(Math.toRadians(newAngle));
            sin = Math.sin(Math.toRadians(newAngle));
            Vector arrowAngled = new Vector(cos, 0, sin).normalize().multiply(length);
            arrowAngled.setY(velocity.getY());
            owner.launchProjectile(Arrow.class, arrowAngled);
        }
    }
}
