package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

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
        this.winners = Set.copyOf(winners); // Defensive copy for immutability
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
