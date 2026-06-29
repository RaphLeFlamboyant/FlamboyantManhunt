package me.flamboyant.manhunt.infrastructure.adapters;

import com.google.inject.Inject;
import me.flamboyant.configurable.parameters.*;
import me.flamboyant.manhunt.application.commands.GameLaunchConfiguration;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.services.GameLaunchService;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Singleton;
import java.util.*;

/**
 * Infrastructure adapter implementing FlamboyantPluginTools ILaunchablePlugin.
 * Translates framework UI parameters to domain GameLaunchConfiguration.
 * Delegates game orchestration to GameLaunchService (application layer).
 *
 * This is the ONLY class allowed to couple infrastructure to application.
 */
@Singleton
public class ManhuntPluginAdapter implements ILaunchablePlugin {
    private final GameLaunchService gameLaunchService;
    private final Server server;

    // Framework UI parameters (infrastructure concern)
    private BooleanParameter resetPlayersStuffParameter;
    private BooleanParameter specialRolesOnlyParameter;
    private BooleanParameter surpriseSpeedrunnerParameter;
    private IntParameter allyCountParameter;
    private IntParameter speedrunnerCountParameter;
    private IntParameter minutesBeforeRolesParameter;
    private Map<Player, EnumParameter<ManhuntRoleIdentifier>> playerRoles;

    @Inject
    public ManhuntPluginAdapter(
        GameLaunchService gameLaunchService,
        Server server
    ) {
        this.gameLaunchService = gameLaunchService;
        this.server = server;
        this.playerRoles = new HashMap<>();
        initializeParameters();
    }

    @Override
    public boolean start() {
        try {
            GameLaunchConfiguration config = buildConfigurationFromParameters();
            gameLaunchService.startGame(config);
            return true;
        } catch (GameStartException e) {
            Bukkit.getLogger().severe("Failed to start game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean stop() {
        gameLaunchService.stopGame("Game stopped by player");
        return true;
    }

    @Override
    public boolean isRunning() {
        return gameLaunchService.isRunning();
    }

    @Override
    public void resetParameters() {
        initializeParameters();
    }

    @Override
    public List<AParameter> getParameters() {
        List<AParameter> params = new ArrayList<>();
        params.add(resetPlayersStuffParameter);
        params.add(minutesBeforeRolesParameter);
        params.add(speedrunnerCountParameter);
        params.add(allyCountParameter);
        params.add(specialRolesOnlyParameter);
        params.add(surpriseSpeedrunnerParameter);
        params.addAll(playerRoles.values());
        return params;
    }

    private void initializeParameters() {
        resetPlayersStuffParameter = new BooleanParameter(
            Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
        resetPlayersStuffParameter.setCategory("Manhunt Parameters");

        specialRolesOnlyParameter = new BooleanParameter(
            Material.NETHER_STAR, "Special only", "Random role = special");
        specialRolesOnlyParameter.setCategory("Manhunt Parameters");

        surpriseSpeedrunnerParameter = new BooleanParameter(
            Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
        surpriseSpeedrunnerParameter.setCategory("Manhunt Parameters");

        speedrunnerCountParameter = new IntParameter(
            Material.DIAMOND_BOOTS, "Speedrunners count", "0 = random",
            1, 0, server.getOnlinePlayers().size());
        speedrunnerCountParameter.setCategory("Manhunt Parameters");

        allyCountParameter = new IntParameter(
            Material.GOLDEN_APPLE, "Allies count", "0 = random",
            0, 0, server.getOnlinePlayers().size());
        allyCountParameter.setCategory("Manhunt Parameters");

        minutesBeforeRolesParameter = new IntParameter(
            Material.CLOCK, "Roles time", "Minutes avant annonce rôles",
            10, 0, 20);
        minutesBeforeRolesParameter.setCategory("Manhunt Parameters");

        initializePlayerRoleParameters();
    }

    private void initializePlayerRoleParameters() {
        playerRoles.clear();
        for (Player player : server.getOnlinePlayers()) {
            EnumParameter<ManhuntRoleIdentifier> param = new EnumParameter<>(
                Material.PLAYER_HEAD,
                player.getDisplayName(),
                "Select role",
                ManhuntRoleIdentifier.class
            );
            param.setIsNullable(true);
            param.setCategory("Players Role");
            playerRoles.put(player, param);
        }
    }

    private GameLaunchConfiguration buildConfigurationFromParameters() {
        List<Player> players = new ArrayList<>(server.getOnlinePlayers());

        Map<Player, ManhuntRoleIdentifier> roleAssignments = new HashMap<>();
        for (Map.Entry<Player, EnumParameter<ManhuntRoleIdentifier>> entry : playerRoles.entrySet()) {
            if (entry.getValue().getValue() != null) {
                roleAssignments.put(entry.getKey(), entry.getValue().getValue());
            }
        }

        return GameLaunchConfiguration.builder()
            .players(players)
            .playerRoleAssignments(roleAssignments)
            .speedrunnerCount(speedrunnerCountParameter.getValue())
            .allyCount(allyCountParameter.getValue())
            .specialRolesOnly(specialRolesOnlyParameter.getValue())
            .hiddenSpeedrunner(surpriseSpeedrunnerParameter.getValue())
            .resetPlayerStuff(resetPlayersStuffParameter.getValue())
            .roleRevealDelayMinutes(minutesBeforeRolesParameter.getValue())
            .build();
    }
}
