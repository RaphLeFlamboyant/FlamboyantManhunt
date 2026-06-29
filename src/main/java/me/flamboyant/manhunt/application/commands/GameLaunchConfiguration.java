package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Value object holding game launch configuration.
 * Decouples application layer from framework parameter system.
 */
public class GameLaunchConfiguration {
    private final List<Player> players;
    private final Map<Player, ManhuntRoleIdentifier> playerRoleAssignments;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final boolean hiddenSpeedrunner;
    private final boolean resetPlayerStuff;
    private final int roleRevealDelayMinutes;

    private GameLaunchConfiguration(Builder builder) {
        this.players = builder.players != null
            ? Collections.unmodifiableList(new ArrayList<>(builder.players))
            : Collections.emptyList();
        this.playerRoleAssignments = builder.playerRoleAssignments != null
            ? Collections.unmodifiableMap(new HashMap<>(builder.playerRoleAssignments))
            : Collections.emptyMap();
        this.speedrunnerCount = builder.speedrunnerCount;
        this.allyCount = builder.allyCount;
        this.specialRolesOnly = builder.specialRolesOnly;
        this.hiddenSpeedrunner = builder.hiddenSpeedrunner;
        this.resetPlayerStuff = builder.resetPlayerStuff;
        this.roleRevealDelayMinutes = builder.roleRevealDelayMinutes;
    }

    public List<Player> getPlayers() { return players; }
    public Map<Player, ManhuntRoleIdentifier> getPlayerRoleAssignments() { return playerRoleAssignments; }
    public int getSpeedrunnerCount() { return speedrunnerCount; }
    public int getAllyCount() { return allyCount; }
    public boolean isSpecialRolesOnly() { return specialRolesOnly; }
    public boolean isHiddenSpeedrunner() { return hiddenSpeedrunner; }
    public boolean isResetPlayerStuff() { return resetPlayerStuff; }
    public int getRoleRevealDelayMinutes() { return roleRevealDelayMinutes; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Player> players = Collections.emptyList();
        private Map<Player, ManhuntRoleIdentifier> playerRoleAssignments = Collections.emptyMap();
        private int speedrunnerCount = 1;
        private int allyCount = 0;
        private boolean specialRolesOnly = false;
        private boolean hiddenSpeedrunner = false;
        private boolean resetPlayerStuff = false;
        private int roleRevealDelayMinutes = 10;

        public Builder players(List<Player> players) {
            this.players = players;
            return this;
        }

        public Builder playerRoleAssignments(Map<Player, ManhuntRoleIdentifier> assignments) {
            this.playerRoleAssignments = assignments;
            return this;
        }

        public Builder speedrunnerCount(int count) {
            this.speedrunnerCount = count;
            return this;
        }

        public Builder allyCount(int count) {
            this.allyCount = count;
            return this;
        }

        public Builder specialRolesOnly(boolean specialRolesOnly) {
            this.specialRolesOnly = specialRolesOnly;
            return this;
        }

        public Builder hiddenSpeedrunner(boolean hidden) {
            this.hiddenSpeedrunner = hidden;
            return this;
        }

        public Builder resetPlayerStuff(boolean reset) {
            this.resetPlayerStuff = reset;
            return this;
        }

        public Builder roleRevealDelayMinutes(int minutes) {
            this.roleRevealDelayMinutes = minutes;
            return this;
        }

        public GameLaunchConfiguration build() {
            return new GameLaunchConfiguration(this);
        }
    }
}
