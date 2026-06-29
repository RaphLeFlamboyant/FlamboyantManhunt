package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public abstract class ItemActivatedAbility implements Ability {
    protected final ItemStack triggerItem;
    protected final Duration cooldown;
    protected final AbilityContext context;

    protected ItemActivatedAbility(AbilityContext context, ItemStack triggerItem, Duration cooldown) {
        this.context = context;
        this.triggerItem = triggerItem;
        this.cooldown = cooldown;
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        context.registerEventHandler(PlayerInteractEvent.class, this::handleInteractEvent);
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        // Cleanup handled by EventRegistrationService
    }

    protected void handleInteractEvent(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!context.getItemService().isSameItemKind(event.getItem(), triggerItem)) {
            return;
        }
        if (context.getAbilityManager().isOnCooldown(context.getOwner(), getName())) {
            return;
        }

        activate();

        context.getAbilityManager().setCooldown(context.getOwner(), getName(), cooldown);
    }

    protected abstract void activate();
}
