package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CompassOnStartAbility extends PassiveAbility {

    public CompassOnStartAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // No event handlers
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        ItemStack compass = new ItemStack(Material.COMPASS);
        context.getOwner().getInventory().addItem(compass);
    }

    @Override
    public String getName() {
        return "Compass On Start";
    }

    @Override
    public String getDescription() {
        return "Donne une boussole au démarrage";
    }
}
