package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.manhunt.GameData;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.utils.ItemHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;

public class CheckpointHunterRole extends AManhuntRole implements Listener {
    private static final int cooldown = 15;

    private BukkitTask nextCheckpointTask;
    private Location savedLocation;
    private double savedHealth = 20;
    private int savedFoodLevel = 20;
    private float savedSaturation = 10;
    private int savedFireTicks = 0;
    private ItemStack[] savedInventory;
    private HashSet<PotionEffect> savedEffects = new HashSet<>();

    public CheckpointHunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected String getName() {
        return "Checkpoint Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu obtiens un objet te permettant " +
                "de revenir à ton dernier checkpoint. Tu auras alors la vie, saturation, effets et équipement " +
                "que tu avais au moment de ce checkpoint. Les checkpoints sont faits " +
                "par le jeu de façon aléatoire et tu n'en auras pas connaissance.";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.HUNTER;
    }

    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getRollbackItem());
        Common.server.getPluginManager().registerEvents(this, Common.plugin);

        nextCheckpointTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> doCheckpoint(), 1);
        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (GameData.remainingSpeedrunner == 0 ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(nextCheckpointTask.getTaskId());

        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerRespawnEvent.getHandlerList().unregister(this);

        owner.setCooldown(Material.RECOVERY_COMPASS, 0);
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != owner) return;
        ItemStack modelItem = getRollbackItem();
        if (!event.hasItem() || !ItemHelper.isSameItemKind(event.getItem(), modelItem)) return;
        event.setCancelled(true);
        if (owner.hasCooldown(modelItem.getType())) return;

        owner.setCooldown(modelItem.getType(), cooldown * 60 * 20);

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

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (event.getPlayer() != owner) return;
        owner.getInventory().addItem(getRollbackItem());
    }

    private void scheduleNextCheckpoint() {
        nextCheckpointTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> doCheckpoint(), (1 + (Common.rng.nextInt(cooldown) * 60 * 20)));
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
        scheduleNextCheckpoint();
    }

    private ItemStack getRollbackItem() {
        return ItemHelper.generateItem(Material.RECOVERY_COMPASS, 1, "Rollback", Arrays.asList("Te fait revenir au checkpoint"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
