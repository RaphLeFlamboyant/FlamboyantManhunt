package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CyclingCompassAbility extends CompassAbility {
    private List<Player> speedrunners;
    private int targetIndex = 0;

    public CyclingCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
        this.speedrunners = new ArrayList<>();
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        speedrunners = new ArrayList<>();
        for (Player player : context.getSession().getPlayers()) {
            if (context.getSession().getRole(player).getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunners.add(player);
            }
        }
    }

    @Override
    protected Player selectTarget() {
        if (speedrunners.isEmpty()) {
            return null;
        }

        if (++targetIndex >= speedrunners.size()) {
            targetIndex = 0;
        }

        return speedrunners.get(targetIndex);
    }

    @Override
    public String getName() {
        return "Cycling Compass";
    }

    @Override
    public String getDescription() {
        return "Boussole qui cycle automatiquement entre les speedrunners";
    }
}
