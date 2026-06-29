package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class UIPickerCompassAbility extends CompassAbility {
    private PlayerSelectionView trackView;

    public UIPickerCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        List<Player> otherPlayers = context.getSession().getPlayers().stream()
            .filter(p -> p != context.getOwner())
            .collect(Collectors.toList());
        trackView = new PlayerSelectionView(otherPlayers, "Track Selection");

        context.registerEventHandler(InventoryCloseEvent.class, this::onInventoryClose);
    }

    @Override
    protected Player selectTarget() {
        context.getServer().getPluginManager().registerEvents(trackView, context.getPlugin());
        context.getOwner().openInventory(trackView.getView());
        return null; // Target selected via UI callback
    }

    private void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != context.getOwner()) {
            return;
        }
        if (event.getInventory() != trackView.getView()) {
            return;
        }

        Player selectedPlayer = trackView.getSelectedPlayer();
        if (selectedPlayer != null) {
            updateCompass(selectedPlayer);
        }
        trackView.close();
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
