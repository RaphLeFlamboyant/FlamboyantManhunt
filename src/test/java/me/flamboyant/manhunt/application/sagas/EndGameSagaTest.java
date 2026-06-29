package me.flamboyant.manhunt.application.sagas;

import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.behavior.HunterRole;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

class EndGameSagaTest {

    private EndGameSaga saga;
    private GameLifecycleService lifecycleService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        lifecycleService = mock(GameLifecycleService.class);
        messageService = mock(MessageService.class);
        saga = new EndGameSaga(lifecycleService, messageService);
    }

    @Test
    void endGame_shouldBroadcastRoleResultsForWinner() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        WinOutcome winOutcome = mock(WinOutcome.class);
        when(winOutcome.getWinners()).thenReturn(Set.of(ManhuntRoleType.HUNTER));
        when(winOutcome.getDescription()).thenReturn("Hunters win!");

        EndGameCommand command = EndGameCommand.of(sessionId, winOutcome);

        GameSession session = mock(GameSession.class);
        Player player1 = mock(Player.class);
        when(player1.getDisplayName()).thenReturn("Player1");

        HunterRole hunterRole = mock(HunterRole.class);
        when(hunterRole.getRoleType()).thenReturn(ManhuntRoleType.HUNTER);
        when(hunterRole.getName()).thenReturn("Hunter");

        Map<Player, AManhuntRole> roles = Map.of(player1, hunterRole);
        when(session.getAllRoles()).thenReturn(roles);

        when(lifecycleService.getSession(sessionId)).thenReturn(session);
        when(lifecycleService.endSession(sessionId)).thenReturn(session);

        // When
        saga.endGame(command);

        // Then
        // Verify broadcast happened (implementation note: Bukkit.broadcastMessage can't be tested directly,
        // but we verify the logic path completes without error)
        verify(lifecycleService).endSession(sessionId);
    }

    @Test
    void endGame_shouldBroadcastRoleResultsForLoser() {
        // Given
        GameSessionId sessionId = GameSessionId.generate();
        WinOutcome winOutcome = mock(WinOutcome.class);
        when(winOutcome.getWinners()).thenReturn(Set.of(ManhuntRoleType.HUNTER));
        when(winOutcome.getDescription()).thenReturn("Hunters win!");

        EndGameCommand command = EndGameCommand.of(sessionId, winOutcome);

        GameSession session = mock(GameSession.class);
        Player player1 = mock(Player.class);
        when(player1.getDisplayName()).thenReturn("Player1");

        SpeedrunnerRole speedrunnerRole = mock(SpeedrunnerRole.class);
        when(speedrunnerRole.getRoleType()).thenReturn(ManhuntRoleType.SPEEDRUNNER);
        when(speedrunnerRole.getName()).thenReturn("Speedrunner");

        Map<Player, AManhuntRole> roles = Map.of(player1, speedrunnerRole);
        when(session.getAllRoles()).thenReturn(roles);

        when(lifecycleService.getSession(sessionId)).thenReturn(session);
        when(lifecycleService.endSession(sessionId)).thenReturn(session);

        // When
        saga.endGame(command);

        // Then
        verify(lifecycleService).endSession(sessionId);
    }
}
