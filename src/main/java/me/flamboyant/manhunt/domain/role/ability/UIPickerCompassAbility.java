package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class UIPickerCompassAbility extends CompassAbility {
    private Object trackView; // Generic object to avoid infrastructure dependency

    public UIPickerCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        List<Player> otherPlayers = context.getSession().getPlayers().stream()
            .filter(p -> p != context.getOwner())
            .collect(Collectors.toList());

        // Create view using reflection to avoid direct dependency
        // Temporary fix - proper solution requires infrastructure refactoring
        try {
            Class<?> viewClass = Class.forName("me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView");
            trackView = viewClass.getConstructor(List.class, String.class)
                .newInstance(otherPlayers, "Track Selection");
        } catch (Exception e) {
            context.getMessagingPort().sendMessage(context.getOwner(),
                "Failed to create player selection view");
            return;
        }

        context.registerEventHandler(InventoryCloseEvent.class, this::onInventoryClose);
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        super.onRoleStop(context);

        // Unregister trackView listener if it was registered
        if (trackView instanceof org.bukkit.event.Listener) {
            context.getEventRegistrationPort().unregisterEvents(
                (org.bukkit.event.Listener) trackView
            );
        }
        trackView = null;
    }

    @Override
    protected Player selectTarget() {
        // Use context's registerListener instead of direct plugin manager access
        if (trackView instanceof org.bukkit.event.Listener) {
            context.registerListener((org.bukkit.event.Listener) trackView);
        }

        // Open view using reflection
        try {
            Object inventory = trackView.getClass().getMethod("getView").invoke(trackView);
            context.getOwner().openInventory((org.bukkit.inventory.Inventory) inventory);
        } catch (Exception e) {
            context.getMessagingPort().sendMessage(context.getOwner(),
                "Failed to open player selection");
        }
        return null; // Target selected via UI callback
    }

    private void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != context.getOwner()) {
            return;
        }

        // Compare inventory using reflection
        try {
            Object inventory = trackView.getClass().getMethod("getView").invoke(trackView);
            if (event.getInventory() != inventory) {
                return;
            }

            Player selectedPlayer = (Player) trackView.getClass().getMethod("getSelectedPlayer").invoke(trackView);
            if (selectedPlayer != null) {
                updateCompass(selectedPlayer);
            }
            trackView.getClass().getMethod("close").invoke(trackView);
        } catch (Exception e) {
            context.getMessagingPort().sendMessage(context.getOwner(),
                "Failed to handle inventory close");
        }
    }

    @Override
    public String getName() {
        return "UI Picker Compass";
    }

    @Override
    public String getDescription() {
        return "Sélectionne un hunter via l'interface pour le traquer avec la boussole";
    }
}
