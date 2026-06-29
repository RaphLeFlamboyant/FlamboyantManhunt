package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.application.services.ItemService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemActivatedAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private ItemService mockItemService;
    private AbilityManager mockAbilityManager;
    private ItemStack triggerItem;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        mockItemService = mock(ItemService.class);
        mockAbilityManager = mock(AbilityManager.class);
        triggerItem = new ItemStack(Material.COMPASS);

        when(mockContext.getOwner()).thenReturn(mockOwner);
        when(mockContext.getItemService()).thenReturn(mockItemService);
        when(mockContext.getAbilityManager()).thenReturn(mockAbilityManager);
    }

    @Test
    void onRoleStartRegistersEventHandler() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));

        ability.onRoleStart(mockContext);

        verify(mockContext).registerEventHandler(eq(PlayerInteractEvent.class), any());
    }

    @Test
    void activateCalledWhenItemMatches() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));
        ability.onRoleStart(mockContext);

        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        ItemStack eventItem = new ItemStack(Material.COMPASS);
        when(mockEvent.hasItem()).thenReturn(true);
        when(mockEvent.getItem()).thenReturn(eventItem);
        when(mockItemService.isSameItemKind(eventItem, triggerItem)).thenReturn(true);
        when(mockAbilityManager.isOnCooldown(mockOwner, ability.getName())).thenReturn(false);

        // Simulate event routing
        ability.handleInteract(mockEvent);

        assertTrue(ability.wasActivated());
        verify(mockAbilityManager).setCooldown(mockOwner, ability.getName(), Duration.ofSeconds(10));
    }

    @Test
    void activateNotCalledWhenOnCooldown() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));
        ability.onRoleStart(mockContext);

        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        ItemStack eventItem = new ItemStack(Material.COMPASS);
        when(mockEvent.hasItem()).thenReturn(true);
        when(mockEvent.getItem()).thenReturn(eventItem);
        when(mockItemService.isSameItemKind(eventItem, triggerItem)).thenReturn(true);
        when(mockAbilityManager.isOnCooldown(mockOwner, ability.getName())).thenReturn(true);

        ability.handleInteract(mockEvent);

        assertFalse(ability.wasActivated());
    }

    // Test helper class
    private static class TestItemAbility extends ItemActivatedAbility {
        private boolean activated = false;

        public TestItemAbility(AbilityContext context, ItemStack triggerItem, Duration cooldown) {
            super(context, triggerItem, cooldown);
        }

        @Override
        protected void activate() {
            activated = true;
        }

        @Override
        public String getName() {
            return "Test Item Ability";
        }

        @Override
        public String getDescription() {
            return "Test description";
        }

        boolean wasActivated() {
            return activated;
        }

        // Expose for testing
        void handleInteract(PlayerInteractEvent event) {
            super.handleInteractEvent(event);
        }
    }
}
