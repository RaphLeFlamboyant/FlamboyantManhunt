package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class CheckpointHunterRole extends AManhuntRole implements Listener {
    private static final int cooldown = 15;

    private final Server server;
    private final Plugin plugin;
    private final MessageService messageService;
    private final ItemService itemService;
    private final EventRegistrationService eventRegistration;
    private final Random rng = new Random();

    private BukkitTask nextCheckpointTask;
    private Location savedLocation;
    private double savedHealth = 20;
    private int savedFoodLevel = 20;
    private float savedSaturation = 10;
    private int savedFireTicks = 0;
    private HashSet<PotionEffect> savedEffects = new HashSet<>();

    @Inject
    public CheckpointHunterRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner);
        this.server = server;
        this.plugin = plugin;
        this.messageService = messageService;
        this.itemService = itemService;
        this.eventRegistration = eventRegistration;
    }

    @Override
    public String getName() {
        return "Checkpoint Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand le speedrunner meurt. Tu obtiens un objet te permettant " +
                "de revenir à ton dernier checkpoint. Tu auras alors la vie et la saturation " +
                "que tu avais au moment de ce checkpoint. Les checkpoints sont faits " +
                "par le jeu de façon aléatoire et tu n'en auras pas connaissance.";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.HUNTER;
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_CHECKPOINT;
    }

    @Override
    protected boolean doStart() {
        owner.getInventory().addItem(getRollbackItem());
        // Manual registration removed - handled by StartGameSaga

        nextCheckpointTask = Bukkit.getScheduler().runTaskLater(plugin, () -> doCheckpoint(), 1);
        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        boolean won = session != null && session.getRemainingSpeedrunners() == 0;
        Bukkit.broadcastMessage(messageService.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (won ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected boolean doStop() {
        Bukkit.getScheduler().cancelTask(nextCheckpointTask.getTaskId());

        // Manual unregistration removed - handled by GameLifecycleService

        owner.setCooldown(Material.RECOVERY_COMPASS, 0);
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (savedLocation == null) return;
        if (event.getPlayer() != owner) return;
        ItemStack modelItem = getRollbackItem();
        if (!event.hasItem() || !itemService.isSameItemKind(event.getItem(), modelItem)) return;
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
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (event.getPlayer() != owner) return;
        owner.getInventory().addItem(getRollbackItem());
        savedLocation = null;
    }

    private void scheduleNextCheckpoint() {
        nextCheckpointTask = Bukkit.getScheduler().runTaskLater(plugin, () -> doCheckpoint(), (1 + (rng.nextInt(cooldown) * 60 * 20)));
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

        scheduleNextCheckpoint();
    }

    private ItemStack getRollbackItem() {
        return itemService.generateItem(Material.RECOVERY_COMPASS, 1, "Rollback", Arrays.asList("Te fait revenir au checkpoint"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
