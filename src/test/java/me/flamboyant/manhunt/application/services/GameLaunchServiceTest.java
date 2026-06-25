package me.flamboyant.manhunt.application.services;

import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.commands.GameLaunchConfiguration;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.plugin.GamePlugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GameLaunchServiceTest {
    private GameLaunchService gameLaunchService;
    private StartGameSaga mockStartGameSaga;
    private EndGameSaga mockEndGameSaga;
    private DomainEventPublisher mockEventPublisher;
    private GamePlugin mockPlugin;

    @Before
    public void setup() {
        mockStartGameSaga = mock(StartGameSaga.class);
        mockEndGameSaga = mock(EndGameSaga.class);
        mockEventPublisher = mock(DomainEventPublisher.class);
        mockPlugin = mock(GamePlugin.class);
        gameLaunchService = new GameLaunchService(mockStartGameSaga, mockEndGameSaga, mockEventPublisher);
    }

    @Test
    public void testStartGame_delegatesToSaga() throws GameStartException {
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        GameSessionId expectedId = new GameSessionId();
        when(mockStartGameSaga.start(any(StartGameCommand.class))).thenReturn(expectedId);

        GameSessionId result = gameLaunchService.startGame(config);

        assertEquals("Should return session ID from saga", expectedId, result);
        assertTrue("Game should be running", gameLaunchService.isRunning());
        verify(mockStartGameSaga).start(any(StartGameCommand.class));
    }

    @Test(expected = GameStartException.class)
    public void testStartGame_alreadyRunning_throwsException() throws GameStartException {
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        when(mockStartGameSaga.start(any())).thenReturn(new GameSessionId());

        gameLaunchService.startGame(config);
        gameLaunchService.startGame(config); // Should throw
    }

    @Test
    public void testStartGame_startsOptionalPlugins() throws GameStartException {
        gameLaunchService.registerOptionalPlugin(mockPlugin);
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        when(mockStartGameSaga.start(any())).thenReturn(new GameSessionId());

        gameLaunchService.startGame(config);

        verify(mockPlugin).start();
    }

    @Test
    public void testStartGame_optionalPluginFails_continuesAnyway() throws GameStartException {
        gameLaunchService.registerOptionalPlugin(mockPlugin);
        doThrow(new RuntimeException("Plugin failure")).when(mockPlugin).start();
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        when(mockStartGameSaga.start(any())).thenReturn(new GameSessionId());

        // Should not throw
        GameSessionId result = gameLaunchService.startGame(config);

        assertNotNull("Should still return session ID", result);
        assertTrue("Game should be running", gameLaunchService.isRunning());
    }

    @Test
    public void testStopGame_endsViaEndGameSaga() throws GameStartException {
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        when(mockStartGameSaga.start(any())).thenReturn(new GameSessionId());
        gameLaunchService.startGame(config);

        gameLaunchService.stopGame("Test reason");

        assertFalse("Game should not be running", gameLaunchService.isRunning());
        verify(mockEndGameSaga).endGame(any(EndGameCommand.class));
    }

    @Test
    public void testStopGame_stopsOptionalPlugins() throws GameStartException {
        gameLaunchService.registerOptionalPlugin(mockPlugin);
        GameLaunchConfiguration config = GameLaunchConfiguration.builder().build();
        when(mockStartGameSaga.start(any())).thenReturn(new GameSessionId());
        gameLaunchService.startGame(config);

        gameLaunchService.stopGame("Test reason");

        verify(mockPlugin).stop();
    }

    @Test
    public void testStopGame_notRunning_ignores() {
        // Should not throw
        gameLaunchService.stopGame("Test reason");

        verify(mockEndGameSaga, never()).endGame(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterOptionalPlugin_nullPlugin_throwsException() {
        gameLaunchService.registerOptionalPlugin(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartGame_nullConfiguration_throwsException() throws GameStartException {
        gameLaunchService.startGame(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStopGame_nullReason_throwsException() {
        gameLaunchService.stopGame(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullStartSaga_throwsException() {
        new GameLaunchService(null, mockEndGameSaga, mockEventPublisher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullEndSaga_throwsException() {
        new GameLaunchService(mockStartGameSaga, null, mockEventPublisher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullEventPublisher_throwsException() {
        new GameLaunchService(mockStartGameSaga, mockEndGameSaga, null);
    }
}
