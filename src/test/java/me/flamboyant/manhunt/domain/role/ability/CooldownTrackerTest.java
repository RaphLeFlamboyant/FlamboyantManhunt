package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class CooldownTrackerTest {

    private CooldownTracker tracker;
    private Player player1;
    private Player player2;

    @BeforeEach
    public void setUp() {
        tracker = new CooldownTracker();
        player1 = mock(Player.class);
        player2 = mock(Player.class);
    }

    @Test
    public void newPlayerHasNoCooldowns() {
        // Given: a new player
        // When: we check if they have a cooldown for any ability
        boolean isOnCooldown = tracker.isOnCooldown(player1, "fireball");

        // Then: they should not be on cooldown
        assertFalse(isOnCooldown);
    }

    @Test
    public void setCooldownMakesAbilityOnCooldown() {
        // Given: a tracker and a player
        // When: we set a cooldown for an ability
        tracker.setCooldown(player1, "fireball", Duration.ofSeconds(10));

        // Then: the ability should be on cooldown
        assertTrue(tracker.isOnCooldown(player1, "fireball"));
    }

    @Test
    public void clearCooldownRemovesCooldown() {
        // Given: a player with an active cooldown
        tracker.setCooldown(player1, "fireball", Duration.ofSeconds(10));
        assertTrue(tracker.isOnCooldown(player1, "fireball"));

        // When: we clear the cooldown
        tracker.clearCooldown(player1, "fireball");

        // Then: the ability should no longer be on cooldown
        assertFalse(tracker.isOnCooldown(player1, "fireball"));
    }

    @Test
    public void clearAllCooldownsRemovesAllForPlayer() {
        // Given: a player with multiple active cooldowns
        tracker.setCooldown(player1, "fireball", Duration.ofSeconds(10));
        tracker.setCooldown(player1, "frostbolt", Duration.ofSeconds(10));
        tracker.setCooldown(player1, "lightning", Duration.ofSeconds(10));

        // When: we clear all cooldowns for the player
        tracker.clearAllCooldowns(player1);

        // Then: all abilities should no longer be on cooldown
        assertFalse(tracker.isOnCooldown(player1, "fireball"));
        assertFalse(tracker.isOnCooldown(player1, "frostbolt"));
        assertFalse(tracker.isOnCooldown(player1, "lightning"));
    }

    @Test
    public void differentPlayersHaveIndependentCooldowns() {
        // Given: two different players
        // When: we set cooldowns for each
        tracker.setCooldown(player1, "fireball", Duration.ofSeconds(10));
        tracker.setCooldown(player2, "frostbolt", Duration.ofSeconds(10));

        // Then: cooldowns should be independent
        assertTrue(tracker.isOnCooldown(player1, "fireball"));
        assertFalse(tracker.isOnCooldown(player1, "frostbolt"));
        assertFalse(tracker.isOnCooldown(player2, "fireball"));
        assertTrue(tracker.isOnCooldown(player2, "frostbolt"));
    }

    @Test
    public void expiredCooldownReturnsFalse() throws InterruptedException {
        // Given: a cooldown with a very short duration
        tracker.setCooldown(player1, "fireball", Duration.ofMillis(100));

        // Then: immediately after setting, it should be on cooldown
        assertTrue(tracker.isOnCooldown(player1, "fireball"));

        // When: we wait for the cooldown to expire
        Thread.sleep(150); // Wait longer than the cooldown duration

        // Then: the cooldown should have expired and return false
        assertFalse(tracker.isOnCooldown(player1, "fireball"));
    }
}
