package me.flamboyant.manhunt.application.sagas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.domain.event.GameEndedEvent;

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

    @Inject
    public EndGameSaga(GameLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /**
     * Entry point - end game.
     * Called by infrastructure (NewManhuntLauncher).
     *
     * @param command End game command
     */
    public void end(EndGameCommand command) {
        try {
            lifecycleService.endSession(command);
        } catch (Exception e) {
            // End game is best-effort - log but don't fail
            logger.severe("Error ending game: " + e.getMessage());
            e.printStackTrace();
        }
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
