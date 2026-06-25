package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.GameStartedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class GameLifecycleService {
    private final GameSessionManager sessionManager;
    private final DomainEventPublisher eventPublisher;
    private final EventHandlerRegistrationService eventHandlerRegistrationService;

    @Inject
    public GameLifecycleService(GameSessionManager sessionManager,
                                DomainEventPublisher eventPublisher,
                                EventHandlerRegistrationService eventHandlerRegistrationService) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.eventHandlerRegistrationService = eventHandlerRegistrationService;
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
     * Unregisters handlers, notifies game ended, cleans up.
     * Domain publishes GameEndedEvent.
     *
     * @param command End game command
     * @return The ended session
     */
    public GameSession endSession(EndGameCommand command) {
        GameSession session = sessionManager.getSession(command.getSessionId());
        if (session == null) {
            return null; // Idempotent - already ended
        }

        // Unregister Bukkit event handlers (prevent events reaching stopped roles)
        HandlerRegistration registration = session.getHandlerRegistration();
        if (registration != null) {
            try {
                eventHandlerRegistrationService.unregisterHandlers(registration);
            } catch (Exception e) {
                // Log error but continue cleanup
                Bukkit.getLogger().severe("Failed to unregister handlers for session " + command.getSessionId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // End the session (notifies game ended)
        session.notifyGameEnded(null, command.getReason());

        // Remove session from manager
        sessionManager.removeSession(command.getSessionId());

        return session;
    }

    /**
     * Gets a session by ID.
     *
     * @param sessionId Session ID
     * @return Session or null if not found
     */
    public GameSession getSession(GameSessionId sessionId) {
        return sessionManager.getSession(sessionId);
    }

    /**
     * Ends a game session and performs cleanup.
     * Stops all roles, unregisters Bukkit event handlers, cleans session state.
     *
     * @param sessionId the session ID
     * @return The ended session
     */
    public GameSession endSession(GameSessionId sessionId) {
        GameSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            return null; // Idempotent - already ended
        }

        // Unregister Bukkit event handlers (prevent events reaching stopped roles)
        HandlerRegistration registration = session.getHandlerRegistration();
        if (registration != null) {
            try {
                eventHandlerRegistrationService.unregisterHandlers(registration);
            } catch (Exception e) {
                // Log error but continue cleanup
                Bukkit.getLogger().severe("Failed to unregister handlers for session " + sessionId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // End the session (notifies game ended)
        session.notifyGameEnded(null, "Game ended");

        // Remove session from manager
        sessionManager.removeSession(sessionId);

        return session;
    }
}
