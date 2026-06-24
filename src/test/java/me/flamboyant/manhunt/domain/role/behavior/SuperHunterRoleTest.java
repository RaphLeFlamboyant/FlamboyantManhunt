package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.DragonKilledCondition;
import me.flamboyant.manhunt.domain.wincondition.WinCondition;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SuperHunterRoleTest {

    private Player owner;
    private SuperHunterRole role;
    private GameSession session;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        role = new SuperHunterRole(owner);
        session = mock(GameSession.class);
    }

    @Test
    void modifier_shouldBlockHunterWinWhenKillCountBelowThreshold() {
        // Given
        WinCondition hunterWin = new AllSpeedrunnersDeadCondition();
        // Kill count is 0 (default)

        // When
        boolean allowed = role.allowsWin(hunterWin, session);

        // Then
        assertFalse(allowed, "Should block hunter win when kill count < 3");
    }

    @Test
    void modifier_shouldAllowHunterWinWhenKillCountMeetsThreshold() {
        // Given
        WinCondition hunterWin = new AllSpeedrunnersDeadCondition();

        // Simulate 3 kills
        role.incrementKillCount();
        role.incrementKillCount();
        role.incrementKillCount();

        // When
        boolean allowed = role.allowsWin(hunterWin, session);

        // Then
        assertTrue(allowed, "Should allow hunter win when kill count >= 3");
    }

    @Test
    void modifier_shouldNotBlockSpeedrunnerWin() {
        // Given
        WinCondition speedrunnerWin = new DragonKilledCondition();
        // Kill count is 0

        // When
        boolean allowed = role.allowsWin(speedrunnerWin, session);

        // Then
        assertTrue(allowed, "Should not block speedrunner wins regardless of kill count");
    }

    @Test
    void modifier_shouldNotProvideAlternativeWin() {
        // When
        Optional<WinCondition> alternative = role.getAlternativeWin(session);

        // Then
        assertFalse(alternative.isPresent(), "SuperHunterRole should not provide alternative wins");
    }
}
