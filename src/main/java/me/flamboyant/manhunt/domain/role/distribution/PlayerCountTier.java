package me.flamboyant.manhunt.domain.role.distribution;

public class PlayerCountTier {
    private final int minPlayers;
    private final int maxPlayers;
    private final int speedrunners;
    private final int allies;

    public PlayerCountTier(int minPlayers, int maxPlayers, int speedrunners, int allies) {
        if (minPlayers < 1) {
            throw new InvalidConfigurationException("minPlayers must be at least 1");
        }
        if (maxPlayers < minPlayers) {
            throw new InvalidConfigurationException(
                "maxPlayers (" + maxPlayers + ") must be >= minPlayers (" + minPlayers + ")"
            );
        }
        if (speedrunners < 0 || allies < 0) {
            throw new InvalidConfigurationException("Role counts must be non-negative");
        }

        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.speedrunners = speedrunners;
        this.allies = allies;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getSpeedrunners() {
        return speedrunners;
    }

    public int getAllies() {
        return allies;
    }

    public boolean matches(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }
}
