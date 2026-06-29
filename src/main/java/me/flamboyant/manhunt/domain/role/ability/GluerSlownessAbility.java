package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

public class GluerSlownessAbility extends PassiveAbility {

    public GluerSlownessAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Scheduled task approach
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        org.bukkit.Bukkit.getScheduler().runTaskTimer(
            context.getPlugin(),
            () -> applySlownessToNearbySpeedrunners(),
            20,
            20
        );
    }

    private void applySlownessToNearbySpeedrunners() {
        List<Player> nearbySpeedrunners = context.getSession().getPlayers().stream()
            .filter(p -> context.getSession().getRole(p).getRoleType() == ManhuntRoleType.SPEEDRUNNER)
            .filter(p -> p.getLocation().distance(context.getOwner().getLocation()) < 10)
            .collect(Collectors.toList());

        for (Player speedrunner : nearbySpeedrunners) {
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 2 * 20, 1, false, false);
            speedrunner.addPotionEffect(slowness);
        }
    }

    @Override
    public String getName() {
        return "Gluer Slowness";
    }

    @Override
    public String getDescription() {
        return "Applique Slowness 2 aux speedrunners proches";
    }
}
