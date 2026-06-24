package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates win conditions and applies registered modifiers.
 * Instance-scoped to GameSession (modifiers cleared on session end).
 */
public class WinConditionEvaluator {

    private final List<WinCondition> conditions;
    private final List<WinConditionModifier> modifiers;

    /**
     * Create an evaluator with a list of win conditions.
     *
     * @param conditions The win conditions to check (order matters - first match wins)
     */
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.conditions = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(conditions, "Conditions cannot be null")));
        this.modifiers = new ArrayList<>();
    }

    /**
     * Register a modifier that can block or provide alternative wins.
     * Modifiers are session-scoped and cleared when evaluator is destroyed.
     *
     * @param modifier The modifier to register
     * @throws NullPointerException if modifier is null
     */
    public void registerModifier(WinConditionModifier modifier) {
        Objects.requireNonNull(modifier, "Modifier cannot be null");
        this.modifiers.add(modifier);
    }

    /**
     * Evaluate all win conditions with modifier support.
     *
     * Flow:
     * 1. Check base conditions (AllSpeedrunnersDead, DragonKilled)
     * 2. Apply modifiers - any can block
     * 3. Check alternative wins from modifiers
     * 4. Return first match or empty
     *
     * @param session The game session to evaluate
     * @return Optional containing WinOutcome if any condition is met
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        Objects.requireNonNull(session, "Session cannot be null");

        // 1. Check base conditions
        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                // 2. Apply modifiers - check if allowed
                if (isWinAllowedByModifiers(condition, session)) {
                    return Optional.of(WinOutcome.of(condition));
                }
            }
        }

        // 3. Check alternative wins from modifiers
        return checkAlternativeWins(session);
    }

    /**
     * Check if all modifiers allow this win condition.
     * ANY modifier returning false will block the win.
     */
    private boolean isWinAllowedByModifiers(WinCondition condition, GameSession session) {
        for (WinConditionModifier modifier : modifiers) {
            if (!modifier.allowsWin(condition, session)) {
                return false; // Blocked by this modifier
            }
        }
        return true; // All modifiers allow it
    }

    /**
     * Check if any modifier provides an alternative win.
     * Returns first alternative found.
     */
    private Optional<WinOutcome> checkAlternativeWins(GameSession session) {
        for (WinConditionModifier modifier : modifiers) {
            Optional<WinCondition> alternative = modifier.getAlternativeWin(session);
            if (alternative.isPresent()) {
                WinCondition condition = alternative.get();
                if (condition.isMet(session)) {
                    return Optional.of(WinOutcome.of(condition));
                }
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

    /**
     * Get currently registered modifiers.
     *
     * @return Immutable copy of registered modifiers
     */
    public List<WinConditionModifier> getModifiers() {
        return Collections.unmodifiableList(new ArrayList<>(modifiers));
    }
}
