package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownTracker {
    private final Map<Player, Map<String, Instant>> cooldowns;

    public CooldownTracker() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public boolean isOnCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) {
            return false;
        }

        Instant cooldownEnd = playerCooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return false;
        }

        return Instant.now().isBefore(cooldownEnd);
    }

    public void setCooldown(Player player, String abilityId, Duration duration) {
        Instant cooldownEnd = Instant.now().plus(duration);
        cooldowns
            .computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(abilityId, cooldownEnd);
    }

    public void clearCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns != null) {
            playerCooldowns.remove(abilityId);
        }
    }

    public void clearAllCooldowns(Player player) {
        cooldowns.remove(player);
    }
}
