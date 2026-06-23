package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.GameStartedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class GameLifecycleService {
    private final GameSessionManager sessionManager;
    private final DomainEventPublisher eventPublisher;

    @Inject
    public GameLifecycleService(GameSessionManager sessionManager,
                                DomainEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new game session.
     * Publishes: GameSessionCreatedEvent
     *
     * @param players Players in the session
     * @return Session ID
     */
    public GameSessionId createSession(List<Player> players) {
        GameSession session = sessionManager.createSession();
        GameSessionId sessionId = session.getId();

        eventPublisher.publish(new GameSessionCreatedEvent(sessionId, players));

        return sessionId;
    }

    /**
     * Starts a game session (activates roles, schedules reveal).
     * Publishes: GameStartedEvent
     *
     * @param sessionId Session to start
     * @param minutesBeforeReveal Minutes before role reveal
     * @param surpriseSpeedrunner Whether speedrunner is hidden initially
     * @throws IllegalArgumentException if session not found
     */
    public void startSession(GameSessionId sessionId, int minutesBeforeReveal,
                            boolean surpriseSpeedrunner) {
        GameSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // TODO: Delegate to domain - start roles, schedule reveal
        // This will be wired up in later tasks when saga calls this

        eventPublisher.publish(new GameStartedEvent(
            sessionId,
            session.getPlayers(),
            session.getRemainingSpeedrunners()
        ));
    }

    /**
     * Ends a game session.
     * Domain publishes GameEndedEvent.
     *
     * @param command End game command
     */
    public void endSession(EndGameCommand command) {
        GameSession session = sessionManager.getSession(command.getSessionId());
        if (session != null) {
            session.notifyGameEnded(null, command.getReason());
            sessionManager.removeSession(command.getSessionId());
        }
    }
}
