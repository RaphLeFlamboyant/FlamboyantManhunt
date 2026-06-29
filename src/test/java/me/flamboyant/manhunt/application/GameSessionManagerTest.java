package me.flamboyant.manhunt.application;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class GameSessionManagerTest {

    private GameSessionManager manager;

    @Before
    public void setUp() {
        manager = GameSessionManager.getInstance();
        manager.removeAllSessions(); // Clean state for each test
    }

    @Test
    public void testGetInstance_returnsSingleton() {
        GameSessionManager manager1 = GameSessionManager.getInstance();
        GameSessionManager manager2 = GameSessionManager.getInstance();

        assertSame(manager1, manager2);
    }

    @Test
    public void testCreateSession_generatesNewSession() {
        GameSession session = manager.createSession();

        assertNotNull(session);
        assertNotNull(session.getId());
    }

    @Test
    public void testCreateSession_eachSessionHasUniqueId() {
        GameSession session1 = manager.createSession();
        GameSession session2 = manager.createSession();

        assertNotEquals(session1.getId(), session2.getId());
    }

    @Test
    public void testCreateSession_withSpecificId() {
        GameSessionId id = GameSessionId.generate();
        GameSession session = manager.createSession(id);

        assertNotNull(session);
        assertEquals(id, session.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSession_rejectsDuplicateId() {
        GameSessionId id = GameSessionId.generate();
        manager.createSession(id);
        manager.createSession(id); // Should throw
    }

    @Test
    public void testGetSession_retrievesByIdAfterCreation() {
        GameSession created = manager.createSession();
        GameSession retrieved = manager.getSession(created.getId());

        assertSame(created, retrieved);
    }

    @Test
    public void testGetSession_returnsNullForUnknownId() {
        GameSessionId unknownId = GameSessionId.generate();
        GameSession session = manager.getSession(unknownId);

        assertNull(session);
    }

    @Test
    public void testGetActiveSessionForPlayer_findsSessionContainingPlayer() {
        Player player = mock(Player.class);
        AManhuntRole role = mock(AManhuntRole.class);

        GameSession session = manager.createSession();
        session.assignRole(player, role);

        GameSession found = manager.getActiveSessionForPlayer(player);

        assertSame(session, found);
    }

    @Test
    public void testGetActiveSessionForPlayer_returnsNullIfPlayerNotInAnySession() {
        Player player = mock(Player.class);
        manager.createSession(); // Session exists but player not assigned

        GameSession found = manager.getActiveSessionForPlayer(player);

        assertNull(found);
    }

    @Test
    public void testGetActiveSessionForPlayer_findsCorrectSessionWithMultipleSessions() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        AManhuntRole role = mock(AManhuntRole.class);

        GameSession session1 = manager.createSession();
        GameSession session2 = manager.createSession();

        session1.assignRole(player1, role);
        session2.assignRole(player2, role);

        assertEquals(session1, manager.getActiveSessionForPlayer(player1));
        assertEquals(session2, manager.getActiveSessionForPlayer(player2));
    }

    @Test
    public void testGetAllSessions_returnsAllActiveSessions() {
        GameSession session1 = manager.createSession();
        GameSession session2 = manager.createSession();

        Set<GameSession> sessions = manager.getAllSessions();

        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(session1));
        assertTrue(sessions.contains(session2));
    }

    @Test
    public void testGetAllSessions_returnsUnmodifiableSet() {
        manager.createSession();
        Set<GameSession> sessions = manager.getAllSessions();

        try {
            sessions.clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testRemoveSession_removesSessionById() {
        GameSession session = manager.createSession();
        GameSessionId id = session.getId();

        manager.removeSession(id);

        assertNull(manager.getSession(id));
        assertEquals(0, manager.getActiveSessionCount());
    }

    @Test
    public void testRemoveSession_doesNotThrowForUnknownId() {
        GameSessionId unknownId = GameSessionId.generate();
        manager.removeSession(unknownId); // Should not throw
    }

    @Test
    public void testRemoveAllSessions_clearsAllSessions() {
        manager.createSession();
        manager.createSession();
        manager.createSession();

        manager.removeAllSessions();

        assertEquals(0, manager.getActiveSessionCount());
        assertEquals(0, manager.getAllSessions().size());
    }

    @Test
    public void testGetActiveSessionCount_countsActiveSessions() {
        assertEquals(0, manager.getActiveSessionCount());

        manager.createSession();
        assertEquals(1, manager.getActiveSessionCount());

        manager.createSession();
        assertEquals(2, manager.getActiveSessionCount());

        manager.removeAllSessions();
        assertEquals(0, manager.getActiveSessionCount());
    }
}
