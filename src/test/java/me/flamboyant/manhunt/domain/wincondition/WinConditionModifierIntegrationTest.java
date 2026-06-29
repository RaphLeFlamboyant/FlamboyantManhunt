package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.SuperHunterRole;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WinConditionModifierIntegrationTest {

    @Test
    void superHunter_shouldBlockHunterWinUntilRequirementMet() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);

        // Set up evaluator with hunter win condition
        AllSpeedrunnersDeadCondition hunterWin = new AllSpeedrunnersDeadCondition();
        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(hunterWin));

        // Create SuperHunter and register as modifier
        Player superHunterPlayer = mock(Player.class);
        SuperHunterRole superHunter = new SuperHunterRole(superHunterPlayer);
        evaluator.registerModifier(superHunter);

        // All speedrunners dead
        session.setRemainingSpeedrunners(0);

        // When - SuperHunter requirement NOT met (0 kills)
        Optional<WinOutcome> result1 = evaluator.evaluate(session);

        // Then
        assertFalse(result1.isPresent(), "Should block hunter win when SuperHunter requirement not met");

        // When - SuperHunter meets requirement (3 kills)
        superHunter.incrementKillCount();
        superHunter.incrementKillCount();
        superHunter.incrementKillCount();
        Optional<WinOutcome> result2 = evaluator.evaluate(session);

        // Then
        assertTrue(result2.isPresent(), "Should allow hunter win when SuperHunter requirement met");
    }

    @Test
    void multipleModifiers_shouldAllowWinWhenAllAllow() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);
        session.setRemainingSpeedrunners(0);

        AllSpeedrunnersDeadCondition hunterWin = new AllSpeedrunnersDeadCondition();
        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(hunterWin));

        // Two modifiers, both allow
        WinConditionModifier modifier1 = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }

            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };

        WinConditionModifier modifier2 = new WinConditionModifier() {
            @Override
            public boolean allowsWin(WinCondition condition, GameSession session) {
                return true;
            }

            @Override
            public Optional<WinCondition> getAlternativeWin(GameSession session) {
                return Optional.empty();
            }
        };

        evaluator.registerModifier(modifier1);
        evaluator.registerModifier(modifier2);

        // When
        Optional<WinOutcome> result = evaluator.evaluate(session);

        // Then
        assertTrue(result.isPresent(), "Should allow win when all modifiers allow");
    }

    @Test
    void dragonKilled_shouldNotBeBlockedByModifiers() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        GameSession session = new GameSession(sessionId);

        DragonKilledCondition speedrunnerWin = new DragonKilledCondition();
        speedrunnerWin.markDragonKilled();

        WinConditionEvaluator evaluator = new WinConditionEvaluator(List.of(speedrunnerWin));

        // SuperHunter modifier registered (but shouldn't block dragon win)
        Player superHunterPlayer = mock(Player.class);
        SuperHunterRole superHunter = new SuperHunterRole(superHunterPlayer);
        evaluator.registerModifier(superHunter);

        // When
        Optional<WinOutcome> result = evaluator.evaluate(session);

        // Then
        assertTrue(result.isPresent(), "Dragon win should not be blocked by SuperHunter modifier");
    }
}
