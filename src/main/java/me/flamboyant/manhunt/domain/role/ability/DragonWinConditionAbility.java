package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.NewManhuntManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

public class DragonWinConditionAbility extends WinConditionAbility {
    private static BukkitTask onWinConTask;
    private static boolean winconMet;

    public DragonWinConditionAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerWinConditionHandlers(AbilityContext context) {
        winconMet = false;
        onWinConTask = null;

        context.registerEventHandler(EntityDamageEvent.class, this::onEntityDamage);
    }

    private void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            return;
        }

        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0) {
            winconMet = true;
            // TODO: Properly integrate with EndGameSaga
            // For now, log that the dragon was killed
            if (onWinConTask == null) {
                onWinConTask = Bukkit.getScheduler().runTaskLater(
                    context.getPlugin(),
                    () -> Bukkit.getLogger().info("Dragon killed - game should end"),
                    1
                );
            }
        }
    }

    @Override
    public String getName() {
        return "Dragon Win Condition";
    }

    @Override
    public String getDescription() {
        return "Gagne quand le dragon de l'Ender meurt";
    }
}
