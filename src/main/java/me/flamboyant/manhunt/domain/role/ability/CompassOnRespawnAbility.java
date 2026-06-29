package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class CompassOnRespawnAbility extends PassiveAbility {

    public CompassOnRespawnAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(PlayerRespawnEvent.class, this::onPlayerRespawn);
    }

    private void onPlayerRespawn(PlayerRespawnEvent event) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        event.getPlayer().getInventory().addItem(compass);
    }

    @Override
    public String getName() {
        return "Compass On Respawn";
    }

    @Override
    public String getDescription() {
        return "Donne une boussole à chaque respawn";
    }
}
