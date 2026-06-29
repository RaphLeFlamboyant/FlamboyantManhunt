package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SpeedrunnerDeathAbility extends WinConditionAbility {

    public SpeedrunnerDeathAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerWinConditionHandlers(AbilityContext context) {
        context.registerEventHandler(EntityDamageEvent.class, this::onEntityDamage);
    }

    private void onEntityDamage(EntityDamageEvent event) {
        Player owner = context.getOwner();
        if (event.getEntity() != owner) {
            return;
        }

        if (owner.getHealth() - event.getFinalDamage() <= 0) {
            owner.setGameMode(GameMode.SPECTATOR);
            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "Speedrunner Death Handler";
    }

    @Override
    public String getDescription() {
        return "Gère la mort du speedrunner (passage en spectateur)";
    }
}
