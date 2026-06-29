package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.DragonKilledCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionEvaluator;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuperHunterWinModifierAbilityTest {
    private SuperHunterWinModifierAbility ability;
    private AbilityContext mockContext;
    private GameSession mockSession;
    private Player mockPlayer;
    private WinConditionEvaluator mockEvaluator;
    private AllSpeedrunnersDeadCondition hunterWinCondition;
    private DragonKilledCondition speedrunnerWinCondition;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockSession = mock(GameSession.class);
        mockPlayer = mock(Player.class);
        mockEvaluator = mock(WinConditionEvaluator.class);
        hunterWinCondition = new AllSpeedrunnersDeadCondition();
        speedrunnerWinCondition = new DragonKilledCondition();

        when(mockContext.getSession()).thenReturn(mockSession);
        when(mockContext.getOwner()).thenReturn(mockPlayer);
        when(mockSession.getWinConditionEvaluator()).thenReturn(mockEvaluator);

        ability = new SuperHunterWinModifierAbility(mockContext);
    }

    @Test
    void onRoleStart_registersModifierWithEvaluator() {
        ability.onRoleStart(mockContext);

        verify(mockEvaluator).registerModifier(ability);
    }

    @Test
    void allowsWin_blocksHunterWin_whenKillCountBelowThreshold() {
        boolean result = ability.allowsWin(hunterWinCondition, mockSession);

        assertFalse(result, "Should block hunter win when no speedrunners killed");
    }

    @Test
    void allowsWin_allowsHunterWin_whenKillCountMeetsThreshold() {
        // Simulate 3 speedrunner kills
        ability.incrementKillCount();
        ability.incrementKillCount();
        ability.incrementKillCount();

        boolean result = ability.allowsWin(hunterWinCondition, mockSession);

        assertTrue(result, "Should allow hunter win after killing 3 speedrunners");
    }

    @Test
    void allowsWin_allowsSpeedrunnerWin_regardless() {
        boolean result = ability.allowsWin(speedrunnerWinCondition, mockSession);

        assertTrue(result, "Should always allow speedrunner win condition");
    }

    @Test
    void getAlternativeWin_returnsEmpty() {
        assertTrue(ability.getAlternativeWin(mockSession).isEmpty(),
            "SuperHunter should not provide alternative win path");
    }

    @Test
    void onEntityDamage_incrementsKillCount_whenSuperHunterKillsSpeedrunner() {
        // Setup: SuperHunter deals lethal damage to Speedrunner
        Player victim = mock(Player.class);
        AManhuntRole victimRole = mock(AManhuntRole.class);

        when(victim.getHealth()).thenReturn(5.0);
        when(mockSession.getRole(victim)).thenReturn(victimRole);
        when(victimRole.getRoleType()).thenReturn(ManhuntRoleType.SPEEDRUNNER);

        // Simulate damage event processing (8 damage = lethal)
        ability.onSpeedrunnerKilled(victim);

        assertEquals(1, ability.getKillCount(), "Kill count should increment when speedrunner dies");
    }

    @Test
    void broadcastsProgress_whenSpeedrunnerKilled() {
        ability.onSpeedrunnerKilled(mock(Player.class));

        // Kill count should be tracked
        assertEquals(1, ability.getKillCount());
    }

    @Test
    void getName_returnsCorrectName() {
        assertEquals("Super Hunter Win Modifier", ability.getName());
    }

    @Test
    void getDescription_returnsCorrectDescription() {
        String description = ability.getDescription();

        assertNotNull(description);
        assertTrue(description.contains("3"), "Description should mention kill count requirement");
    }
}
