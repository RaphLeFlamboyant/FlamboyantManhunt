package me.flamboyant.manhunt.domain.role.ability;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {
    private final Map<Player, Map<Class<? extends Event>, List<EventHandler>>> eventHandlers;
    private final CooldownTracker cooldownTracker;

    @Inject
    public AbilityManager(CooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
        this.eventHandlers = new ConcurrentHashMap<>();
    }

    public void registerAbility(Ability ability, AbilityContext context) {
        ability.onRoleStart(context);
    }

    public void unregisterAbility(Ability ability, AbilityContext context) {
        ability.onRoleStop(context);
    }

    public void routeEvent(Event event, Player owner) {
        Map<Class<? extends Event>, List<EventHandler>> playerHandlers = eventHandlers.get(owner);
        if (playerHandlers == null) {
            return;
        }

        List<EventHandler> handlers = playerHandlers.get(event.getClass());
        if (handlers == null) {
            return;
        }

        handlers.forEach(handler -> {
            try {
                handler.handle(event);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Event handler failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void registerEventHandler(Player owner, Class<? extends Event> eventType, EventHandler handler) {
        eventHandlers
            .computeIfAbsent(owner, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(handler);
    }

    public void clearEventHandlers(Player owner) {
        eventHandlers.remove(owner);
    }

    public boolean isOnCooldown(Player player, String abilityId) {
        return cooldownTracker.isOnCooldown(player, abilityId);
    }

    public void setCooldown(Player player, String abilityId, Duration duration) {
        cooldownTracker.setCooldown(player, abilityId, duration);
    }

    public void clearCooldown(Player player, String abilityId) {
        cooldownTracker.clearCooldown(player, abilityId);
    }

    @FunctionalInterface
    public interface EventHandler {
        void handle(Event event);
    }
}
