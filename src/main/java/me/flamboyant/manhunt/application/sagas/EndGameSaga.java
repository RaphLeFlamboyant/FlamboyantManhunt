package me.flamboyant.manhunt.application.sagas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.event.GameEndedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * Saga coordinator for the "end game" workflow.
 * Orchestrates the game end process.
 *
 * Workflow Steps:
 * 1. End session via GameLifecycleService
 * 2. Domain publishes GameEndedEvent
 * 3. Saga performs application-layer cleanup
 *
 * Note: End game is best-effort - errors are logged but do not fail the operation.
 */
@Singleton
public class EndGameSaga {
    private static final Logger logger = Logger.getLogger(EndGameSaga.class.getName());

    private final GameLifecycleService lifecycleService;
    private final MessageService messageService;

    @Inject
    public EndGameSaga(GameLifecycleService lifecycleService, MessageService messageService) {
        this.lifecycleService = lifecycleService;
        this.messageService = messageService;
    }

    /**
     * Entry point - end game with win condition evaluation.
     * Called by infrastructure (NewManhuntLauncher).
     * Broadcasts role results for each player.
     *
     * @param command End game command with win outcome
     * @return The ended session
     */
    public GameSession endGame(EndGameCommand command) {
        // Get session before it's fully cleaned up
        GameSession session = lifecycleService.getSession(command.getSessionId());
        WinOutcome outcome = command.getWinOutcome();

        // Broadcast role results for each player
        if (session != null && outcome != null) {
            session.getAllRoles().forEach((player, role) -> {
                boolean playerWon = outcome.getWinners().contains(role.getRoleType());
                String result = String.format(
                    "%s, qui était %s a %s !",
                    player.getDisplayName(),
                    role.getName(),
                    playerWon ? "gagné" : "perdu"
                );
                messageService.broadcastMessage("&6" + result);
            });
        }

        // End the session
        return lifecycleService.endSession(command.getSessionId());
    }

    /**
     * Event handler: GameEndedEvent (from domain)
     * Perform application-layer cleanup.
     *
     * @param event Game ended event
     */
    public void onGameEnded(GameEndedEvent event) {
        // Cleanup any saga state
        // Unregister any remaining handlers
        // For now, this is a no-op - cleanup can be added later if needed
        logger.info("Game ended cleanup completed for session: " + event.getSessionId());
    }
}
