package me.flamboyant.manhunt.domain.role.distribution.strategies;

import me.flamboyant.manhunt.domain.role.distribution.*;

public class TieredRoleCountStrategy implements RoleCountStrategy {
    @Override
    public RoleCounts determineRoleCounts(int playerCount, RoleDistributionConfig config) {
        // If admin specified explicit counts, use those
        if (config.getSpeedrunnerCount() > 0 || config.getAllyCount() > 0) {
            int speedrunners = config.getSpeedrunnerCount();
            int allies = config.getAllyCount();
            int hunters = playerCount - speedrunners - allies;
            return new RoleCounts(speedrunners, allies, hunters, 0);
        }

        // Find matching tier
        PlayerCountTier matchingTier = config.getTiers().stream()
            .filter(tier -> tier.matches(playerCount))
            .findFirst()
            .orElseThrow(() -> new InsufficientPlayersException(
                "No tier found for player count: " + playerCount
            ));

        int speedrunners = matchingTier.getSpeedrunners();
        int allies = matchingTier.getAllies();
        int hunters = playerCount - speedrunners - allies;

        return new RoleCounts(speedrunners, allies, hunters, 0);
    }
}
