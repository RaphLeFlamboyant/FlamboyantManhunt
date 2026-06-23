package me.flamboyant.manhunt;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.behavior.HunterRole;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for GameSession aggregate root.
 * Tests full game lifecycle without mocking internal components.
 */
public class GameSessionIntegrationTest {

    private GameSessionManager sessionManager;
    private GameSession session;
    private Player player1;
    private Player player2;

    @Before
    public void setUp() {
        sessionManager = GameSessionManager.getInstance();
        sessionManager.removeAllSessions();
        session = sessionManager.createSession();

        player1 = mock(Player.class);
        player2 = mock(Player.class);
    }

    @After
    public void tearDown() {
        sessionManager.removeAllSessions();
    }

    @Test
    public void testFullGameLifecycle_createAssignQueryCleanup() {
        // Create session
        assertNotNull(session);
        assertEquals(0, session.getPlayers().size());

        // Assign roles
        AManhuntRole speedrunner = new SpeedrunnerRole(player1);
        AManhuntRole hunter = new HunterRole(player2);
        session.assignRole(player1, speedrunner);
        session.assignRole(player2, hunter);

        // Query state
        assertEquals(2, session.getPlayers().size());
        assertTrue(session.hasRole(player1));
        assertTrue(session.hasRole(player2));
        assertEquals(speedrunner, session.getRole(player1));
        assertEquals(hunter, session.getRole(player2));

        // Set speedrunner count
        session.setRemainingSpeedrunners(1);
        assertEquals(1, session.getRemainingSpeedrunners());

        // Decrement
        assertEquals(0, session.decrementSpeedrunners());
        assertEquals(0, session.getRemainingSpeedrunners());

        // Cleanup
        session.clear();
        assertEquals(0, session.getPlayers().size());
        assertFalse(session.hasRole(player1));
    }

    @Test
    public void testPortalTracking_multiplePlayersMultipleDimensions() {
        AManhuntRole role1 = new SpeedrunnerRole(player1);
        AManhuntRole role2 = new SpeedrunnerRole(player2);
        session.assignRole(player1, role1);
        session.assignRole(player2, role2);

        Location p1Overworld = mock(Location.class);
        Location p1Nether = mock(Location.class);
        Location p2Overworld = mock(Location.class);
        Location p2Nether = mock(Location.class);

        // Record portal entries
        session.recordPortalEntry(player1, p1Overworld, World.Environment.NORMAL);
        session.recordPortalEntry(player1, p1Nether, World.Environment.NETHER);
        session.recordPortalEntry(player2, p2Overworld, World.Environment.NORMAL);
        session.recordPortalEntry(player2, p2Nether, World.Environment.NETHER);

        // Verify independent tracking
        assertEquals(p1Overworld, session.getPortalLocation(player1, World.Environment.NORMAL));
        assertEquals(p1Nether, session.getPortalLocation(player1, World.Environment.NETHER));
        assertEquals(p2Overworld, session.getPortalLocation(player2, World.Environment.NORMAL));
        assertEquals(p2Nether, session.getPortalLocation(player2, World.Environment.NETHER));
    }

    @Test
    public void testMultipleConcurrentSessions() {
        GameSession session1 = sessionManager.createSession();
        GameSession session2 = sessionManager.createSession();

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);

        AManhuntRole role1 = new SpeedrunnerRole(p1);
        AManhuntRole role2 = new HunterRole(p2);

        // Assign to different sessions
        session1.assignRole(p1, role1);
        session2.assignRole(p2, role2);

        // Verify isolation
        assertTrue(session1.hasRole(p1));
        assertFalse(session1.hasRole(p2));
        assertFalse(session2.hasRole(p1));
        assertTrue(session2.hasRole(p2));

        // Verify session manager finds correct session
        assertEquals(session1, sessionManager.getActiveSessionForPlayer(p1));
        assertEquals(session2, sessionManager.getActiveSessionForPlayer(p2));

        // Verify independent state
        session1.setRemainingSpeedrunners(3);
        session2.setRemainingSpeedrunners(5);
        assertEquals(3, session1.getRemainingSpeedrunners());
        assertEquals(5, session2.getRemainingSpeedrunners());
    }

    @Test
    public void testSessionManagerCleanup() {
        GameSession s1 = sessionManager.createSession();
        GameSession s2 = sessionManager.createSession();

        Player p1 = mock(Player.class);
        s1.assignRole(p1, new SpeedrunnerRole(p1));

        assertEquals(2, sessionManager.getActiveSessionCount());

        // Remove specific session
        sessionManager.removeSession(s1.getId());
        assertEquals(1, sessionManager.getActiveSessionCount());
        assertNull(sessionManager.getSession(s1.getId()));
        assertNotNull(sessionManager.getSession(s2.getId()));

        // Remove all sessions
        sessionManager.removeAllSessions();
        assertEquals(0, sessionManager.getActiveSessionCount());
    }

    @Test
    public void testRoleAssignmentReplacementAndQuery() {
        AManhuntRole role1 = new SpeedrunnerRole(player1);
        AManhuntRole role2 = new HunterRole(player1);

        // Assign initial role
        session.assignRole(player1, role1);
        assertEquals(role1, session.getRole(player1));

        // Replace role
        session.assignRole(player1, role2);
        assertEquals(role2, session.getRole(player1));
        assertEquals(1, session.getPlayers().size());
    }

    @Test
    public void testSpeedrunnerCountManagement() {
        // Start at 0
        assertEquals(0, session.getRemainingSpeedrunners());

        // Set count
        session.setRemainingSpeedrunners(5);
        assertEquals(5, session.getRemainingSpeedrunners());

        // Decrement
        assertEquals(4, session.decrementSpeedrunners());
        assertEquals(3, session.decrementSpeedrunners());
        assertEquals(2, session.decrementSpeedrunners());
        assertEquals(1, session.decrementSpeedrunners());
        assertEquals(0, session.decrementSpeedrunners());

        // Cannot go below 0
        assertEquals(0, session.decrementSpeedrunners());
        assertEquals(0, session.getRemainingSpeedrunners());
    }

    @Test
    public void testSessionIsolation_portalLocations() {
        GameSession session1 = sessionManager.createSession();
        GameSession session2 = sessionManager.createSession();

        Player p1 = mock(Player.class);
        Location loc1 = mock(Location.class);
        Location loc2 = mock(Location.class);

        session1.assignRole(p1, new SpeedrunnerRole(p1));
        session2.assignRole(p1, new SpeedrunnerRole(p1));

        session1.recordPortalEntry(p1, loc1, World.Environment.NORMAL);
        session2.recordPortalEntry(p1, loc2, World.Environment.NORMAL);

        // Sessions maintain independent portal tracking
        assertEquals(loc1, session1.getPortalLocation(p1, World.Environment.NORMAL));
        assertEquals(loc2, session2.getPortalLocation(p1, World.Environment.NORMAL));
    }
}
