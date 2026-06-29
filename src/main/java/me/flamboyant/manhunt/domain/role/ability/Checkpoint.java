package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;

public class Checkpoint {
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int fireTicks;
    private final List<ItemStack> inventory;
    private final Set<PotionEffect> effects;

    public Checkpoint(
        Location location,
        double health,
        int foodLevel,
        float saturation,
        int fireTicks,
        List<ItemStack> inventory,
        Set<PotionEffect> effects
    ) {
        this.location = location;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.fireTicks = fireTicks;
        this.inventory = inventory;
        this.effects = effects;
    }

    public Location getLocation() {
        return location;
    }

    public double getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }

    public Set<PotionEffect> getEffects() {
        return effects;
    }
}
