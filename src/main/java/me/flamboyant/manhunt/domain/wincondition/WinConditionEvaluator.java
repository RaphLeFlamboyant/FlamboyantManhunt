package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates all registered win conditions and returns the first one that is met.
 * Part of the Win Condition bounded context.
 */
public class WinConditionEvaluator {

    private final List<WinCondition> conditions;

    /**
     * Create an evaluator with a list of win conditions.
     *
     * @param conditions The win conditions to check (order matters - first match wins)
     */
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions, "Conditions cannot be null"));
    }

    /**
     * Evaluate all win conditions against the given session.
     * Returns the first condition that is met.
     *
     * @param session The game session to evaluate
     * @return Optional containing WinOutcome if any condition is met, empty otherwise
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        Objects.requireNonNull(session, "Session cannot be null");

        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }

        return Optional.empty();
    }

    /**
     * Get the list of conditions being evaluated.
     *
     * @return Immutable list of win conditions
     */
    public List<WinCondition> getConditions() {
        return conditions;
    }
}
