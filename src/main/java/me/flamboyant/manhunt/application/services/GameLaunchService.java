package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.commands.GameLaunchConfiguration;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameStartFailedEvent;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.plugin.GamePlugin;
import org.bukkit.Bukkit;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Application service for game launch orchestration.
 * Framework-free: uses only domain concepts and sagas.
 */
@Singleton
public class GameLaunchService {
    private volatile boolean running;
    private GameSessionId currentSessionId;
    private final List<GamePlugin> optionalPlugins;
    private final StartGameSaga startGameSaga;
    private final EndGameSaga endGameSaga;
    private final DomainEventPublisher eventPublisher;

    @Inject
    public GameLaunchService(
        StartGameSaga startGameSaga,
        EndGameSaga endGameSaga,
        DomainEventPublisher eventPublisher
    ) {
        if (startGameSaga == null) {
            throw new IllegalArgumentException("StartGameSaga cannot be null");
        }
        if (endGameSaga == null) {
            throw new IllegalArgumentException("EndGameSaga cannot be null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("DomainEventPublisher cannot be null");
        }
        this.startGameSaga = startGameSaga;
        this.endGameSaga = endGameSaga;
        this.eventPublisher = eventPublisher;
        this.optionalPlugins = new ArrayList<>();
        this.running = false;

        // Subscribe to GameStartFailedEvent to reset state on saga compensation
        this.eventPublisher.subscribe(GameStartFailedEvent.class, this::onGameStartFailed);
    }

    /**
     * Start a new game with the given configuration.
     * @param config game launch configuration (must not be null)
     * @return session ID of started game
     * @throws GameStartException if game cannot be started
     */
    public synchronized GameSessionId startGame(GameLaunchConfiguration config) throws GameStartException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (running) {
            throw new GameStartException("Cannot start game - already running");
        }
        if (currentSessionId != null) {
            throw new IllegalStateException("Session ID exists but game not marked as running");
        }

        // Build StartGameCommand from configuration
        StartGameCommand command = StartGameCommand.builder()
            .players(config.getPlayers())
            .fixedRoleAssignments(config.getPlayerRoleAssignments())
            .speedrunnerCount(config.getSpeedrunnerCount())
            .allyCount(config.getAllyCount())
            .specialRolesOnly(config.isSpecialRolesOnly())
            .surpriseSpeedrunner(config.isHiddenSpeedrunner())
            .resetPlayerStuff(config.isResetPlayerStuff())
            .minutesBeforeRoleReveal(config.getRoleRevealDelayMinutes())
            .build();

        // Start via saga
        currentSessionId = startGameSaga.start(command);

        // Start optional plugins
        for (GamePlugin plugin : optionalPlugins) {
            try {
                plugin.start();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to start optional plugin: " + e.getMessage());
            }
        }

        running = true;
        return currentSessionId;
    }

    /**
     * Stop the currently running game.
     * @param reason reason for stopping (must not be null)
     */
    public synchronized void stopGame(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Reason cannot be null");
        }
        if (!running) {
            Bukkit.getLogger().info("stopGame called but game not running - ignoring");
            return;
        }

        // Stop optional plugins
        for (GamePlugin plugin : optionalPlugins) {
            try {
                plugin.stop();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to stop optional plugin: " + e.getMessage());
            }
        }

        // End via saga
        if (currentSessionId != null) {
            EndGameCommand command = new EndGameCommand(currentSessionId, reason);
            endGameSaga.endGame(command);
            currentSessionId = null;
        }

        running = false;
    }

    /**
     * Check if a game is currently running.
     * @return true if game running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Register an optional plugin to start/stop with game.
     * @param plugin plugin to register (must not be null)
     */
    public synchronized void registerOptionalPlugin(GamePlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        optionalPlugins.add(plugin);
    }

    /**
     * Event handler for GameStartFailedEvent.
     * Resets running state when saga compensation occurs.
     * @param event the game start failed event
     */
    private synchronized void onGameStartFailed(GameStartFailedEvent event) {
        // If saga compensation occurred, the game actually stopped even though we set running=true
        if (running && event.getSessionId().equals(currentSessionId)) {
            Bukkit.getLogger().warning(
                "Game start failed for session " + event.getSessionId() +
                " - resetting state. Cause: " + event.getCause().getMessage()
            );
            running = false;
            currentSessionId = null;
        }
    }
}
