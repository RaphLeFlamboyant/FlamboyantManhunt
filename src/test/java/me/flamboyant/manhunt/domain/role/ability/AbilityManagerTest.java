package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityManagerTest {
    private AbilityManager abilityManager;
    private CooldownTracker mockCooldownTracker;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        mockCooldownTracker = mock(CooldownTracker.class);
        abilityManager = new AbilityManager(mockCooldownTracker);
        mockPlayer = mock(Player.class);
    }

    @Test
    void registerAbilityCallsOnRoleStart() {
        Ability mockAbility = mock(Ability.class);
        AbilityContext mockContext = mock(AbilityContext.class);

        abilityManager.registerAbility(mockAbility, mockContext);

        verify(mockAbility).onRoleStart(mockContext);
    }

    @Test
    void unregisterAbilityCallsOnRoleStop() {
        Ability mockAbility = mock(Ability.class);
        AbilityContext mockContext = mock(AbilityContext.class);

        abilityManager.unregisterAbility(mockAbility, mockContext);

        verify(mockAbility).onRoleStop(mockContext);
    }

    @Test
    void routeEventCallsRegisteredHandler() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);

        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, mockPlayer);

        assertTrue(handlerCalled.get());
    }

    @Test
    void routeEventDoesNotCallUnregisteredPlayer() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        Player otherPlayer = mock(Player.class);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);

        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, otherPlayer);

        assertFalse(handlerCalled.get());
    }

    @Test
    void clearEventHandlersRemovesAllHandlers() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);

        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        abilityManager.clearEventHandlers(mockPlayer);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, mockPlayer);

        assertFalse(handlerCalled.get());
    }

    @Test
    void isOnCooldownDelegatesToTracker() {
        when(mockCooldownTracker.isOnCooldown(mockPlayer, "test-ability")).thenReturn(true);

        assertTrue(abilityManager.isOnCooldown(mockPlayer, "test-ability"));
    }

    @Test
    void setCooldownDelegatesToTracker() {
        Duration duration = Duration.ofSeconds(10);

        abilityManager.setCooldown(mockPlayer, "test-ability", duration);

        verify(mockCooldownTracker).setCooldown(mockPlayer, "test-ability", duration);
    }
}
