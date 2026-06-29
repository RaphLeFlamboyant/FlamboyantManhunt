package me.flamboyant.manhunt.domain.role.distribution;

public class RoleCounts {
    private final int speedrunners;
    private final int allies;
    private final int hunters;
    private final int neutrals;

    public RoleCounts(int speedrunners, int allies, int hunters, int neutrals) {
        if (speedrunners < 0 || allies < 0 || hunters < 0 || neutrals < 0) {
            throw new InvalidConfigurationException(
                "Role counts must be non-negative: speedrunners=" + speedrunners +
                ", allies=" + allies + ", hunters=" + hunters + ", neutrals=" + neutrals
            );
        }
        this.speedrunners = speedrunners;
        this.allies = allies;
        this.hunters = hunters;
        this.neutrals = neutrals;
    }

    public int getSpeedrunners() {
        return speedrunners;
    }

    public int getAllies() {
        return allies;
    }

    public int getHunters() {
        return hunters;
    }

    public int getNeutrals() {
        return neutrals;
    }

    public int total() {
        return speedrunners + allies + hunters + neutrals;
    }

    public RoleCounts subtract(RoleCounts other) {
        int newSpeedrunners = this.speedrunners - other.speedrunners;
        int newAllies = this.allies - other.allies;
        int newHunters = this.hunters - other.hunters;
        int newNeutrals = this.neutrals - other.neutrals;

        if (newSpeedrunners < 0 || newAllies < 0 || newHunters < 0 || newNeutrals < 0) {
            throw new InvalidConfigurationException(
                "Subtraction would result in negative counts: speedrunners=" + newSpeedrunners +
                ", allies=" + newAllies + ", hunters=" + newHunters + ", neutrals=" + newNeutrals
            );
        }

        return new RoleCounts(newSpeedrunners, newAllies, newHunters, newNeutrals);
    }

    public boolean exceedsAny(RoleCounts limit) {
        return this.speedrunners > limit.speedrunners
            || this.allies > limit.allies
            || this.hunters > limit.hunters
            || this.neutrals > limit.neutrals;
    }
}
