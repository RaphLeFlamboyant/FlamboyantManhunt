package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable value object representing the outcome of a win condition evaluation.
 * Part of the Win Condition bounded context.
 */
public final class WinOutcome {

    private final Set<ManhuntRoleType> winners;
    private final String description;
    private final WinCondition triggeringCondition;

    private WinOutcome(Set<ManhuntRoleType> winners, String description, WinCondition triggeringCondition) {
        this.winners = Collections.unmodifiableSet(new HashSet<>(winners)); // Defensive copy for immutability
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.triggeringCondition = Objects.requireNonNull(triggeringCondition, "Triggering condition cannot be null");
    }

    /**
     * Create a WinOutcome from a win condition.
     *
     * @param condition The win condition that was met
     * @return Immutable WinOutcome
     */
    public static WinOutcome of(WinCondition condition) {
        return new WinOutcome(
            condition.getWinners(),
            condition.getDescription(),
            condition
        );
    }

    /**
     * Create a WinOutcome for hunters winning with a custom description.
     * Convenience factory for tests and scenarios where a full WinCondition is not available.
     *
     * @param description The win description
     * @return Immutable WinOutcome with HUNTER as winner
     */
    public static WinOutcome huntersWin(String description) {
        return new WinOutcome(
            Collections.singleton(me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.HUNTER),
            description,
            new SimpleWinCondition(
                Collections.singleton(me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.HUNTER),
                description
            )
        );
    }

    /**
     * Create a WinOutcome for speedrunners winning with a custom description.
     * Convenience factory for tests and scenarios where a full WinCondition is not available.
     *
     * @param description The win description
     * @return Immutable WinOutcome with SPEEDRUNNER as winner
     */
    public static WinOutcome speedrunnersWin(String description) {
        return new WinOutcome(
            Collections.singleton(me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.SPEEDRUNNER),
            description,
            new SimpleWinCondition(
                Collections.singleton(me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.SPEEDRUNNER),
                description
            )
        );
    }

    /**
     * Simple win condition implementation for factory methods.
     */
    private static class SimpleWinCondition implements WinCondition {
        private final Set<me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType> winners;
        private final String description;

        SimpleWinCondition(Set<me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType> winners, String description) {
            this.winners = winners;
            this.description = description;
        }

        @Override
        public boolean isMet(me.flamboyant.manhunt.domain.game.GameSession session) {
            return true; // Already met since this is for creating an outcome
        }

        @Override
        public Set<me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType> getWinners() {
            return winners;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    /**
     * Get the winning team(s).
     *
     * @return Immutable set of winning role types
     */
    public Set<ManhuntRoleType> getWinners() {
        return winners;
    }

    /**
     * Get the description of why the game ended.
     *
     * @return Description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the condition that triggered this outcome.
     *
     * @return The win condition
     */
    public WinCondition getTriggeringCondition() {
        return triggeringCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WinOutcome that = (WinOutcome) o;
        return Objects.equals(winners, that.winners) &&
               Objects.equals(description, that.description) &&
               Objects.equals(triggeringCondition, that.triggeringCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(winners, description, triggeringCondition);
    }

    @Override
    public String toString() {
        return "WinOutcome{" +
               "winners=" + winners +
               ", description='" + description + '\'' +
               '}';
    }
}
