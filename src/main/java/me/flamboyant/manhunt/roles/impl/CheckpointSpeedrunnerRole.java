package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.utils.ItemHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.HashSet;

public class CheckpointSpeedrunnerRole extends SpeedrunnerRole {
    private static final int cooldown = 15;

    private Location savedLocation;
    private double savedHealth = 20;
    private int savedFoodLevel = 20;
    private float savedSaturation = 10;
    private int savedFireTicks = 0;
    private ItemStack[] savedInventory;
    private HashSet<PotionEffect> savedEffects = new HashSet<>();

    public CheckpointSpeedrunnerRole(Player owner) {
        super(owner);
    }

    @Override
    protected String getName() {
        return "Checkpoint Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais ut perds si tu meurs avant ! " +
                "Tu obtiens deux objets. Le premier te permet de poser un checkpoint. " +
                "Le second te permet de revenir à ton dernier checkpoint. " +
                "Tu auras alors la vie, saturation, effets et équipement " +
                "que tu avais au moment de ce checkpoint.";
    }
    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getCheckpointItem());
        owner.getInventory().addItem(getRollbackItem());
        return super.doStart();
    }

    @Override
    protected boolean doStop() {
        PlayerInteractEvent.getHandlerList().unregister(this);
        owner.setCooldown(Material.WRITABLE_BOOK, 0);
        owner.setCooldown(Material.RECOVERY_COMPASS, 0);
        return super.doStop();
    }

    @Override
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        super.onPlayerInteract(event);
        if (event.getPlayer() != owner) return;
        if (!event.hasItem()) return;

        Material itemUsed;
        Runnable methodToExecute;
        if (ItemHelper.isSameItemKind(event.getItem(), getCheckpointItem())) {
            itemUsed = getCheckpointItem().getType();
            methodToExecute = this::doCheckpoint;
        }
        else if (ItemHelper.isSameItemKind(event.getItem(), getRollbackItem())) {
            itemUsed = getRollbackItem().getType();
            methodToExecute = this::doRollBack;
        }
        else
            return;

        event.setCancelled(true);
        if (owner.hasCooldown(itemUsed)) return;
        owner.setCooldown(itemUsed, cooldown * 60 * 20);

        methodToExecute.run();
    }

    private void doRollBack() {
        owner.teleport(savedLocation);
        owner.setHealth(savedHealth);
        owner.setFoodLevel(savedFoodLevel);
        owner.setSaturation(savedSaturation);
        owner.setFireTicks(savedFireTicks);
        for (PotionEffect effect : owner.getActivePotionEffects()) {
            owner.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : savedEffects) {
            owner.addPotionEffect(effect);
        }
        owner.getInventory().setContents(savedInventory);
    }

    private void doCheckpoint() {
        savedLocation = owner.getLocation();
        savedHealth = owner.getHealth();
        savedFoodLevel = owner.getFoodLevel();
        savedSaturation = owner.getSaturation();
        savedFireTicks = owner.getFireTicks();
        savedEffects.clear();
        for (PotionEffect effect : owner.getActivePotionEffects()) {
            savedEffects.add(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
        }

        savedInventory = owner.getInventory().getContents();
    }

    private ItemStack getRollbackItem() {
        return ItemHelper.generateItem(Material.RECOVERY_COMPASS, 1, "Rollback", Arrays.asList("Te fait revenir au checkpoint"), true, Enchantment.ARROW_FIRE, true, true);
    }

    private ItemStack getCheckpointItem() {
        return ItemHelper.generateItem(Material.WRITABLE_BOOK, 1, "Save State", Arrays.asList("Créer un checkpoint"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
