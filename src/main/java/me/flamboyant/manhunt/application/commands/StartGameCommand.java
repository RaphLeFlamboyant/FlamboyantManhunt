package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartGameCommand {
    private final List<Player> players;
    private final Map<Player, ManhuntRoleIdentifier> fixedRoleAssignments;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final boolean resetPlayerStuff;
    private final boolean surpriseSpeedrunner;
    private final int minutesBeforeRoleReveal;

    private StartGameCommand(Builder builder) {
        // Validation
        if (builder.players == null || builder.players.isEmpty()) {
            throw new IllegalArgumentException("Cannot start game with no players");
        }
        if (builder.speedrunnerCount + builder.allyCount > builder.players.size()) {
            throw new IllegalArgumentException(
                "Too many speedrunners + allies for player count: " +
                (builder.speedrunnerCount + builder.allyCount) + " > " + builder.players.size()
            );
        }
        if (builder.minutesBeforeRoleReveal < 0 || builder.minutesBeforeRoleReveal > 60) {
            throw new IllegalArgumentException(
                "Role reveal time must be 0-60 minutes, got: " + builder.minutesBeforeRoleReveal
            );
        }
        if (builder.speedrunnerCount < 0 || builder.allyCount < 0) {
            throw new IllegalArgumentException("Counts must be non-negative");
        }

        // Immutable assignments
        this.players = Collections.unmodifiableList(new ArrayList<>(builder.players));
        this.fixedRoleAssignments = builder.fixedRoleAssignments != null
            ? Collections.unmodifiableMap(new HashMap<>(builder.fixedRoleAssignments))
            : Collections.emptyMap();
        this.speedrunnerCount = builder.speedrunnerCount;
        this.allyCount = builder.allyCount;
        this.specialRolesOnly = builder.specialRolesOnly;
        this.resetPlayerStuff = builder.resetPlayerStuff;
        this.surpriseSpeedrunner = builder.surpriseSpeedrunner;
        this.minutesBeforeRoleReveal = builder.minutesBeforeRoleReveal;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public List<Player> getPlayers() { return players; }
    public Map<Player, ManhuntRoleIdentifier> getFixedRoleAssignments() { return fixedRoleAssignments; }
    public int getSpeedrunnerCount() { return speedrunnerCount; }
    public int getAllyCount() { return allyCount; }
    public boolean isSpecialRolesOnly() { return specialRolesOnly; }
    public boolean isResetPlayerStuff() { return resetPlayerStuff; }
    public boolean isSurpriseSpeedrunner() { return surpriseSpeedrunner; }
    public int getMinutesBeforeRoleReveal() { return minutesBeforeRoleReveal; }

    public static class Builder {
        private List<Player> players;
        private Map<Player, ManhuntRoleIdentifier> fixedRoleAssignments = Collections.emptyMap();
        private int speedrunnerCount = 1;
        private int allyCount = 0;
        private boolean specialRolesOnly = false;
        private boolean resetPlayerStuff = false;
        private boolean surpriseSpeedrunner = false;
        private int minutesBeforeRoleReveal = 10;

        public Builder players(List<Player> players) {
            this.players = players;
            return this;
        }

        public Builder fixedRoleAssignments(Map<Player, ManhuntRoleIdentifier> assignments) {
            this.fixedRoleAssignments = assignments;
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

        public Builder specialRolesOnly(boolean specialOnly) {
            this.specialRolesOnly = specialOnly;
            return this;
        }

        public Builder resetPlayerStuff(boolean reset) {
            this.resetPlayerStuff = reset;
            return this;
        }

        public Builder surpriseSpeedrunner(boolean surprise) {
            this.surpriseSpeedrunner = surprise;
            return this;
        }

        public Builder minutesBeforeRoleReveal(int minutes) {
            this.minutesBeforeRoleReveal = minutes;
            return this;
        }

        public StartGameCommand build() {
            return new StartGameCommand(this);
        }
    }
}
