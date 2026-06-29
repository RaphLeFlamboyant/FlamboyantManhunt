package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.ability.Ability;
import me.flamboyant.manhunt.domain.role.ability.AbilityContext;
import me.flamboyant.manhunt.domain.role.ability.AbilityManager;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinition;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class Role extends AManhuntRole implements Listener {
    private final ManhuntRoleIdentifier identifier;
    private final ManhuntRoleType roleType;
    private final String name;
    private final String description;
    private final List<Ability> abilities;
    private final AbilityManager abilityManager;
    private final AbilityContext context;

    @Inject
    public Role(
        @Assisted Player owner,
        @Assisted RoleDefinition definition,
        AbilityManager abilityManager,
        GameSessionManager sessionManager,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration,
        Server server,
        Plugin plugin
    ) {
        super(owner);
        this.identifier = definition.getIdentifier();
        this.roleType = definition.getRoleType();
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.abilityManager = abilityManager;

        // Build context
        this.context = new AbilityContext(
            owner,
            sessionManager,
            messageService,
            itemService,
            eventRegistration,
            server,
            plugin,
            abilityManager
        );

        // Create abilities
        this.abilities = definition.createAbilities(context);
    }

    @Override
    protected boolean doStart() {
        GameSession session = context.getSessionManager().getActiveSessionForPlayer(owner);
        if (session == null) {
            return false;
        }
        context.setSession(session);

        // Start all abilities
        abilities.forEach(ability -> {
            try {
                abilityManager.registerAbility(ability, context);
            } catch (Exception e) {
                org.bukkit.Bukkit.getLogger().warning(
                    "Failed to start ability " + ability.getName() +
                    " for role " + name + ": " + e.getMessage()
                );
            }
        });

        return true;
    }

    @Override
    protected boolean doStop() {
        // Stop all abilities
        abilities.forEach(ability -> {
            try {
                abilityManager.unregisterAbility(ability, context);
            } catch (Exception e) {
                org.bukkit.Bukkit.getLogger().warning(
                    "Failed to stop ability " + ability.getName() +
                    " for role " + name + ": " + e.getMessage()
                );
            }
        });

        return true;
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        // Default message
        boolean won = determineWinStatus();
        context.getMessageService().broadcastMessage(
            "&6" + owner.getDisplayName() + ", qui était " + name + " a " +
            (won ? "gagné" : "perdu") + " !"
        );
    }

    private boolean determineWinStatus() {
        GameSession session = context.getSession();
        if (session == null) {
            return false;
        }

        // Default logic - can be enhanced by WinConditionAbility instances
        if (roleType == ManhuntRoleType.SPEEDRUNNER) {
            return session.getRemainingSpeedrunners() > 0;
        } else {
            return session.getRemainingSpeedrunners() == 0;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected String getDescription() {
        return description;
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return roleType;
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return identifier;
    }

    // Bukkit event handlers - route to ability manager
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if ((Player) event.getEntity() != owner) return;
        abilityManager.routeEvent(event, owner);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != owner) return;
        abilityManager.routeEvent(event, owner);
    }
}
