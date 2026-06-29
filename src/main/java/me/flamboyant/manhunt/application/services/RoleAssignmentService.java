package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.commands.AssignRolesCommand;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.RolesAssignedEvent;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.AssistedRoleFactory;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinition;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinitionRegistry;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Map;

@Singleton
public class RoleAssignmentService {
    private final GameSessionManager sessionManager;
    private final AssistedRoleFactory roleFactory;
    private final RoleDefinitionRegistry roleDefinitionRegistry;
    private final DomainEventPublisher eventPublisher;

    @Inject
    public RoleAssignmentService(
        GameSessionManager sessionManager,
        AssistedRoleFactory roleFactory,
        RoleDefinitionRegistry roleDefinitionRegistry,
        DomainEventPublisher eventPublisher
    ) {
        this.sessionManager = sessionManager;
        this.roleFactory = roleFactory;
        this.roleDefinitionRegistry = roleDefinitionRegistry;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Assigns distributed roles to a game session.
     * Creates role instances and adds them to session.
     * Publishes: RolesAssignedEvent
     *
     * @param command Role assignment command
     * @throws IllegalArgumentException if session not found
     */
    public void assignRoles(AssignRolesCommand command) {
        GameSession session = sessionManager.getSession(command.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + command.getSessionId());
        }

        for (Map.Entry<Player, ManhuntRoleIdentifier> entry :
             command.getRoleAssignments().entrySet()) {
            Player player = entry.getKey();
            ManhuntRoleIdentifier roleId = entry.getValue();

            // Get role definition from registry
            RoleDefinition definition = roleDefinitionRegistry.getDefinition(roleId);

            // Create role using factory
            AManhuntRole role = roleFactory.create(player, definition);

            // Register role with session
            session.assignRole(player, role);
        }

        eventPublisher.publish(new RolesAssignedEvent(command.getSessionId()));
    }
}
