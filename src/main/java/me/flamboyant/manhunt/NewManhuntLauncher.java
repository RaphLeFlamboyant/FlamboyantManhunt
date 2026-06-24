package me.flamboyant.manhunt;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.configurable.parameters.*;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NewManhuntLauncher implements ILaunchablePlugin {
    private boolean running;
    private GameSessionId currentSessionId;
    private BooleanParameter resetPlayersStuffParameter = new BooleanParameter(Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
    private BooleanParameter specialRolesOnlyParameter = new BooleanParameter(Material.NETHER_STAR, "Special only", "Random role = special");
    private BooleanParameter surpriseSpeedrunnerParameter = new BooleanParameter(Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
    private IntParameter allyCountParameter;
    private IntParameter speedrunnerCountParameter;
    private int initialMinutesBeforeRoles = 10;
    private IntParameter minutesBeforeRolesParameter = new IntParameter(Material.CLOCK, "Roles time", "Minutes avant annonce rôles", initialMinutesBeforeRoles, 0, 20);

    private HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playerRoles = new HashMap<>();
    private List<ILaunchablePlugin> optionalPlugin = new ArrayList<>();

    private final StartGameSaga startGameSaga;
    private final EndGameSaga endGameSaga;
    private final GameSessionManager sessionManager;

    private static NewManhuntLauncher instance;
    public static NewManhuntLauncher getInstance()
    {
        if (instance == null)
        {
            instance = new NewManhuntLauncher();
        }

        return instance;
    }

    /**
     * Sets the singleton instance (used by DI initialization).
     * Package-private to prevent external misuse.
     *
     * @param launcher DI-managed instance
     */
    static void setInstance(NewManhuntLauncher launcher) {
        instance = launcher;
    }

    @Inject
    public NewManhuntLauncher(StartGameSaga startGameSaga, EndGameSaga endGameSaga, GameSessionManager sessionManager)
    {
        this.startGameSaga = startGameSaga;
        this.endGameSaga = endGameSaga;
        this.sessionManager = sessionManager;
        setDefaultParameters();
    }

    // Deprecated: For backwards compatibility with getInstance() pattern
    // TODO: Remove once all callers use DI
    protected NewManhuntLauncher()
    {
        this.startGameSaga = null;
        this.endGameSaga = null;
        this.sessionManager = GameSessionManager.getInstance();
        setDefaultParameters();
    }

    private void setDefaultParameters() {
        initializePlayerRolesParameter();
    }

    private void initializePlayerRolesParameter() {
        playerRoles.clear();
        for (Player player : Common.server.getOnlinePlayers()) {
            EnumParameter<ManhuntRoleIdentifier> param = new EnumParameter<>(Material.PLAYER_HEAD, player.getDisplayName(), "Select role", ManhuntRoleIdentifier.class);
            param.setIsNullable(true);
            param.setCategory("Players Role");
            playerRoles.put(player, param);
        }
    }

    @Override
    public boolean start() {
        if (running) {
            return false;
        }

        // Check if DI is properly configured
        if (startGameSaga == null) {
            Bukkit.getLogger().severe("NewManhuntLauncher not properly initialized via dependency injection!");
            return false;
        }

        try {
            // Launch game via saga
            currentSessionId = Launch();
            if (currentSessionId == null) {
                return false;
            }

            for (ILaunchablePlugin plugin : optionalPlugin) {
                plugin.start();
            }

            running = true;
            return true;
        } catch (GameStartException e) {
            Bukkit.getLogger().severe("Failed to start game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean stop() {
        if (!running) {
            return false;
        }

        // Check if DI is properly configured
        if (endGameSaga == null) {
            Bukkit.getLogger().severe("NewManhuntLauncher not properly initialized via dependency injection!");
            return false;
        }

        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.stop();
        }

        // End game via saga
        if (currentSessionId != null) {
            EndGameCommand command = new EndGameCommand(currentSessionId, "Game stopped by player");
            endGameSaga.end(command);
            currentSessionId = null;
        }

        running = false;
        return true;
    }

    @Override
    public void resetParameters() {
        resetPlayersStuffParameter = new BooleanParameter(Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
        resetPlayersStuffParameter.setCategory("Manhunt Parameters");
        specialRolesOnlyParameter = new BooleanParameter(Material.NETHER_STAR, "Special only", "Random role = special");
        specialRolesOnlyParameter.setCategory("Manhunt Parameters");
        surpriseSpeedrunnerParameter = new BooleanParameter(Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
        surpriseSpeedrunnerParameter.setCategory("Manhunt Parameters");
        speedrunnerCountParameter = new IntParameter(Material.DIAMOND_BOOTS, "Speedrunners count", "0 = random", 1, 0, Common.server.getOnlinePlayers().size());
        speedrunnerCountParameter.setCategory("Manhunt Parameters");
        allyCountParameter = new IntParameter(Material.GOLDEN_APPLE, "Allies count", "0 = random", 0, 0, Common.server.getOnlinePlayers().size());
        allyCountParameter.setCategory("Manhunt Parameters");
        minutesBeforeRolesParameter = new IntParameter(Material.CLOCK, "Roles time", "Minutes avant annonce rôles", initialMinutesBeforeRoles, 0, 20);
        minutesBeforeRolesParameter.setCategory("Manhunt Parameters");

        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.resetParameters();
        }

        setDefaultParameters();
    }

    @Override
    public List<AParameter> getParameters() {
        return getParameters(true);
    }

    private List<AParameter> getParameters(boolean includeNonModifiable) {
        List<AParameter> res = new ArrayList<>();
        if (includeNonModifiable) {
            res.add(resetPlayersStuffParameter);
            res.add(minutesBeforeRolesParameter);
            res.add(speedrunnerCountParameter);
            res.add(allyCountParameter);
            res.add(specialRolesOnlyParameter);
            res.add(surpriseSpeedrunnerParameter);
            for (Player player : playerRoles.keySet()) {
                res.add(new ValueOfPlayerParameter(playerRoles.get(player), "Role de ", player.getDisplayName(), ""));
            }
        }

        for (ILaunchablePlugin plugin : optionalPlugin) {
            if (includeNonModifiable || plugin.canModifyParametersOnTheFly())
            {
                res.addAll(plugin.getParameters());
            }
        }
        return res;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean canModifyParametersOnTheFly() { return false; }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;

        EnderDragon dragon = (EnderDragon) event.getEntity();

        // Check if this damage kills the dragon
        if (dragon.getHealth() - event.getFinalDamage() <= 0) {
            // Determine killer
            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                if (damager instanceof Player) {
                    killer = (Player) damager;
                }
            }

            // Get active session and publish domain event
            GameSession session = GameSessionManager.getInstance().getActiveSession();
            if (session != null) {
                session.notifyDragonKilled(killer);
            }
        }
    }

    private void resetPlayerState(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(3);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private int getSpeedrunnerCount() {
        return (int) playerRoles.values().stream().filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.SPEEDRUNNER).count();
    }

    private int getAllyCount() {
        return (int) playerRoles.values().stream().filter(r -> r.getSelectedValue() != null && r.getSelectedValue().getRoleType() == ManhuntRoleType.ALLY).count();
    }

    private GameSessionId Launch() throws GameStartException {
        int speedrunnerCount = speedrunnerCountParameter.getValue() == 0 ? getSpeedrunnerCount() : speedrunnerCountParameter.getValue();
        int allyCount = allyCountParameter.getValue() == 0 ? getAllyCount() : allyCountParameter.getValue();

        // Reset player states before game start
        for (Player player : Common.server.getOnlinePlayers()) {
            if (player == null) continue;
            resetPlayerState(player);
            if (resetPlayersStuffParameter.getValue() != 0) {
                player.getInventory().clear();
            }
        }

        // Build fixed role assignments from player parameters
        Map<Player, ManhuntRoleIdentifier> fixedRoleAssignments = new HashMap<>();
        for (Map.Entry<Player, EnumParameter<ManhuntRoleIdentifier>> entry : playerRoles.entrySet()) {
            if (entry.getValue().getSelectedValue() != null) {
                fixedRoleAssignments.put(entry.getKey(), entry.getValue().getSelectedValue());
            }
        }

        // Build list of all players
        List<Player> allPlayers = new ArrayList<>(Common.server.getOnlinePlayers());

        // Build start game command
        StartGameCommand command = StartGameCommand.builder()
            .players(allPlayers)
            .fixedRoleAssignments(fixedRoleAssignments)
            .speedrunnerCount(speedrunnerCount)
            .allyCount(allyCount)
            .specialRolesOnly(specialRolesOnlyParameter.getValue() > 0)
            .resetPlayerStuff(resetPlayersStuffParameter.getValue() != 0)
            .surpriseSpeedrunner(surpriseSpeedrunnerParameter.getValue() > 0)
            .minutesBeforeRoleReveal(minutesBeforeRolesParameter.getValue())
            .build();

        // Start game via saga
        return startGameSaga.start(command);
    }
}
