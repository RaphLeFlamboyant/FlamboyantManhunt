package me.flamboyant.manhunt.application.sagas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.application.commands.AssignRolesCommand;
import me.flamboyant.manhunt.application.commands.DistributeRolesCommand;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.application.exceptions.CompensationStatus;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.services.EventHandlerRegistrationService;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.RoleAssignmentService;
import me.flamboyant.manhunt.application.services.RoleDistributionService;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.GameStartFailedEvent;
import me.flamboyant.manhunt.domain.event.HandlersRegisteredEvent;
import me.flamboyant.manhunt.domain.event.RolesAssignedEvent;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Saga coordinator for the "start game" workflow.
 * Orchestrates the complete game start process through event-driven steps.
 *
 * Workflow Steps:
 * 1. Create game session
 * 2. Distribute roles to players
 * 3. Assign roles to session
 * 4. Register Bukkit event handlers
 * 5. Start session (activate roles, schedule reveal)
 *
 * Compensation Logic:
 * On failure at any step, rollback in reverse order:
 * 1. Unregister handlers (if registered)
 * 2. Clear role assignments (if assigned)
 * 3. Delete session (if created)
 * 4. Publish GameStartFailedEvent with compensation status
 */
@Singleton
public class StartGameSaga {
    private static final Logger logger = Bukkit.getLogger();

    private final GameLifecycleService lifecycleService;
    private final RoleDistributionService distributionService;
    private final RoleAssignmentService assignmentService;
    private final EventHandlerRegistrationService handlerService;
    private final GameSessionManager sessionManager;
    private final DomainEventPublisher eventPublisher;

    // Track in-flight workflows for compensation
    private final Map<GameSessionId, StartGameWorkflow> activeWorkflows = new ConcurrentHashMap<>();

    // Optional: Listener provider for handler registration (set via setter for testability)
    private ListenerProvider listenerProvider;

    @Inject
    public StartGameSaga(GameLifecycleService lifecycleService,
                         RoleDistributionService distributionService,
                         RoleAssignmentService assignmentService,
                         EventHandlerRegistrationService handlerService,
                         GameSessionManager sessionManager,
                         DomainEventPublisher eventPublisher) {
        this.lifecycleService = lifecycleService;
        this.distributionService = distributionService;
        this.assignmentService = assignmentService;
        this.handlerService = handlerService;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Sets the listener provider for Bukkit event handler registration.
     * This allows decoupling from NewManhuntManager for testability.
     *
     * @param listenerProvider Provider for listeners to register
     */
    public void setListenerProvider(ListenerProvider listenerProvider) {
        this.listenerProvider = listenerProvider;
    }

    /**
     * Entry point - initiates workflow.
     * Called by infrastructure (NewManhuntLauncher).
     *
     * @param command Start game command with all parameters
     * @return Session ID of the created game
     * @throws GameStartException if game start fails (after compensation)
     */
    public GameSessionId start(StartGameCommand command) throws GameStartException {
        StartGameWorkflow workflow = new StartGameWorkflow(command);

        try {
            // Step 1: Create session
            GameSessionId sessionId = lifecycleService.createSession(command.getPlayers());
            workflow.setSessionId(sessionId);
            activeWorkflows.put(sessionId, workflow);

            // Subsequent steps driven by events
            return sessionId;

        } catch (Exception e) {
            compensate(workflow, e);
            throw new GameStartException("Failed to start game", e);
        }
    }

    /**
     * Event handler: GameSessionCreatedEvent -> distribute roles
     * Step 2 of the workflow.
     *
     * @param event Session created event
     */
    public void onSessionCreated(GameSessionCreatedEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) {
            return;
        }

        try {
            DistributeRolesCommand distCmd = workflow.buildDistributeCommand();
            Map<Player, ManhuntRoleIdentifier> assignments =
                distributionService.distributeRoles(distCmd);
            workflow.setRoleAssignments(assignments);

        } catch (Exception e) {
            compensate(workflow, e);
        }
    }

    /**
     * Event handler: RolesDistributedEvent -> assign roles
     * Step 3 of the workflow.
     *
     * @param event Roles distributed event
     */
    public void onRolesDistributed(RolesDistributedEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) {
            return;
        }

        try {
            AssignRolesCommand assignCmd = workflow.buildAssignCommand();
            assignmentService.assignRoles(assignCmd);

        } catch (Exception e) {
            compensate(workflow, e);
        }
    }

    /**
     * Event handler: RolesAssignedEvent -> register handlers
     * Step 4 of the workflow.
     *
     * @param event Roles assigned event
     */
    public void onRolesAssigned(RolesAssignedEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) {
            return;
        }

        try {
            // Get the game session
            GameSession session = sessionManager.getSession(event.getSessionId());

            // Collect all role listeners for registration
            Collection<AManhuntRole> roles = session.getAllRoles().values();
            List<Listener> roleListeners = new ArrayList<>();
            for (AManhuntRole role : roles) {
                if (role instanceof Listener) {
                    roleListeners.add((Listener) role);
                }
            }

            // Register all role handlers at once
            if (!roleListeners.isEmpty()) {
                HandlerRegistration registration = handlerService.registerHandlers(
                    event.getSessionId(),
                    roleListeners.toArray(new Listener[0])
                );

                // Store registration in session for cleanup
                session.setHandlerRegistration(registration);
                workflow.setHandlerRegistration(registration);
            }

            // Publish event to progress to next step
            eventPublisher.publish(new HandlersRegisteredEvent(event.getSessionId()));

        } catch (Exception e) {
            compensate(workflow, e);
        }
    }

    /**
     * Event handler: HandlersRegisteredEvent -> start session
     * Step 5 (final) of the workflow.
     *
     * @param event Handlers registered event
     */
    public void onHandlersRegistered(HandlersRegisteredEvent event) {
        StartGameWorkflow workflow = activeWorkflows.get(event.getSessionId());
        if (workflow == null) {
            return;
        }

        try {
            lifecycleService.startSession(
                event.getSessionId(),
                workflow.getMinutesBeforeReveal(),
                workflow.isSurpriseSpeedrunner()
            );

            // Workflow complete
            activeWorkflows.remove(event.getSessionId());

        } catch (Exception e) {
            compensate(workflow, e);
        }
    }

    /**
     * Gets listeners to register for a game session.
     * Uses the listener provider if set, otherwise returns empty array.
     *
     * @param sessionId Session ID
     * @return Array of listeners to register
     */
    private Listener[] getListenersForSession(GameSessionId sessionId) {
        if (listenerProvider != null) {
            return listenerProvider.getListeners(sessionId);
        }
        return new Listener[0];
    }

    /**
     * Compensation - rollback in reverse order.
     * Ensures no partial state remains after failure.
     *
     * @param workflow The workflow to compensate
     * @param cause The exception that triggered compensation
     */
    private void compensate(StartGameWorkflow workflow, Exception cause) {
        CompensationStatus status = new CompensationStatus();

        // Rollback step 4: unregister handlers
        try {
            if (workflow.getHandlerRegistration() != null) {
                handlerService.unregisterHandlers(workflow.getHandlerRegistration());
                status.markHandlersUnregistered();
            }
        } catch (Exception e) {
            logger.warning("Failed to unregister handlers during compensation: " + e.getMessage());
        }

        // Rollback step 3: clear roles
        try {
            if (workflow.getSessionId() != null) {
                GameSession session = sessionManager.getSession(workflow.getSessionId());
                if (session != null) {
                    session.clear();
                    status.markRolesCleared();
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to clear roles during compensation: " + e.getMessage());
        }

        // Rollback step 1: delete session
        try {
            if (workflow.getSessionId() != null) {
                sessionManager.removeSession(workflow.getSessionId());
                status.markSessionDeleted();
            }
        } catch (Exception e) {
            logger.warning("Failed to delete session during compensation: " + e.getMessage());
        }

        // Publish failure event
        if (workflow.getSessionId() != null) {
            eventPublisher.publish(new GameStartFailedEvent(
                workflow.getSessionId(),
                cause,
                status
            ));
        }

        activeWorkflows.remove(workflow.getSessionId());
    }

    /**
     * Returns the number of active workflows.
     * Useful for testing and diagnostics.
     *
     * @return Number of active workflows
     */
    public int getActiveWorkflowCount() {
        return activeWorkflows.size();
    }

    /**
     * Checks if a workflow is active for a session.
     * Useful for testing.
     *
     * @param sessionId Session ID to check
     * @return true if workflow is active
     */
    public boolean hasActiveWorkflow(GameSessionId sessionId) {
        return activeWorkflows.containsKey(sessionId);
    }

    /**
     * Functional interface for providing Bukkit listeners.
     * Allows decoupling from NewManhuntManager for testability.
     */
    @FunctionalInterface
    public interface ListenerProvider {
        /**
         * Gets listeners to register for a game session.
         *
         * @param sessionId Session ID
         * @return Array of listeners
         */
        Listener[] getListeners(GameSessionId sessionId);
    }

    /**
     * Inner class - tracks workflow state.
     * Encapsulates all state needed for a single game start workflow.
     */
    private static class StartGameWorkflow {
        private final StartGameCommand command;
        private GameSessionId sessionId;
        private Map<Player, ManhuntRoleIdentifier> roleAssignments;
        private HandlerRegistration handlerRegistration;

        public StartGameWorkflow(StartGameCommand command) {
            this.command = command;
        }

        public StartGameCommand getCommand() {
            return command;
        }

        public GameSessionId getSessionId() {
            return sessionId;
        }

        public void setSessionId(GameSessionId sessionId) {
            this.sessionId = sessionId;
        }

        public Map<Player, ManhuntRoleIdentifier> getRoleAssignments() {
            return roleAssignments;
        }

        public void setRoleAssignments(Map<Player, ManhuntRoleIdentifier> roleAssignments) {
            this.roleAssignments = roleAssignments;
        }

        public HandlerRegistration getHandlerRegistration() {
            return handlerRegistration;
        }

        public void setHandlerRegistration(HandlerRegistration handlerRegistration) {
            this.handlerRegistration = handlerRegistration;
        }

        public int getMinutesBeforeReveal() {
            return command.getMinutesBeforeRoleReveal();
        }

        public boolean isSurpriseSpeedrunner() {
            return command.isSurpriseSpeedrunner();
        }

        /**
         * Builds a DistributeRolesCommand from the workflow state.
         *
         * @return DistributeRolesCommand for step 2
         */
        public DistributeRolesCommand buildDistributeCommand() {
            return new DistributeRolesCommand(
                sessionId,
                command.getPlayers(),
                command.getSpeedrunnerCount(),
                command.getAllyCount(),
                command.isSpecialRolesOnly(),
                command.getFixedRoleAssignments()
            );
        }

        /**
         * Builds an AssignRolesCommand from the workflow state.
         *
         * @return AssignRolesCommand for step 3
         */
        public AssignRolesCommand buildAssignCommand() {
            return new AssignRolesCommand(sessionId, roleAssignments);
        }
    }
}
