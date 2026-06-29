package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class PortalTrackingAbility extends PassiveAbility {

    public PortalTrackingAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(EntityPortalEnterEvent.class, this::onEntityPortalEnter);
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        // Initialize portal tracking
        Player owner = context.getOwner();
        context.getSession().recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
        context.getSession().recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);
    }

    private void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        String worldName = event.getLocation().getWorld().getName();

        if (worldName.equals("world")) {
            context.getSession().recordPortalEntry(player, event.getLocation(), World.Environment.NORMAL);
        } else if (worldName.equals("world_nether")) {
            context.getSession().recordPortalEntry(player, event.getLocation(), World.Environment.NETHER);
        }
    }

    @Override
    public String getName() {
        return "Portal Tracking";
    }

    @Override
    public String getDescription() {
        return "Suit les entrées de portail pour la boussole cross-dimension";
    }
}
