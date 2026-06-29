package me.flamboyant.manhunt.domain.role.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoleDistributionConfig {
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final double specialRoleProbability;
    private final List<PlayerCountTier> tiers;

    public RoleDistributionConfig(int speedrunnerCount, int allyCount, boolean specialRolesOnly,
                                  double specialRoleProbability, List<PlayerCountTier> tiers) {
        if (speedrunnerCount < 0 || allyCount < 0) {
            throw new InvalidConfigurationException("Counts must be non-negative");
        }
        if (specialRoleProbability < 0.0 || specialRoleProbability > 1.0) {
            throw new InvalidConfigurationException(
                "specialRoleProbability must be between 0.0 and 1.0, got: " + specialRoleProbability
            );
        }
        if (tiers == null || tiers.isEmpty()) {
            throw new InvalidConfigurationException("Tiers cannot be null or empty");
        }

        this.speedrunnerCount = speedrunnerCount;
        this.allyCount = allyCount;
        this.specialRolesOnly = specialRolesOnly;
        this.specialRoleProbability = specialRoleProbability;
        this.tiers = Collections.unmodifiableList(new ArrayList<>(tiers));
    }

    public int getSpeedrunnerCount() {
        return speedrunnerCount;
    }

    public int getAllyCount() {
        return allyCount;
    }

    public boolean isSpecialRolesOnly() {
        return specialRolesOnly;
    }

    public double getSpecialRoleProbability() {
        return specialRoleProbability;
    }

    public List<PlayerCountTier> getTiers() {
        return tiers;
    }

    private static List<PlayerCountTier> createDefaultTiers() {
        List<PlayerCountTier> defaultTiers = new ArrayList<>();
        defaultTiers.add(new PlayerCountTier(4, 7, 1, 0));
        defaultTiers.add(new PlayerCountTier(8, 12, 2, 0));
        defaultTiers.add(new PlayerCountTier(13, 16, 2, 1));
        defaultTiers.add(new PlayerCountTier(17, 999, 3, 1));
        return defaultTiers;
    }

    public static RoleDistributionConfig balanced() {
        return new RoleDistributionConfig(0, 0, false, 0.3, createDefaultTiers());
    }

    public static RoleDistributionConfig custom(int speedrunners, int allies) {
        return new RoleDistributionConfig(
            speedrunners,
            allies,
            false,
            0.3,
            createDefaultTiers()
        );
    }

    public static RoleDistributionConfig specialOnly() {
        return new RoleDistributionConfig(0, 0, true, 1.0, createDefaultTiers());
    }
}
