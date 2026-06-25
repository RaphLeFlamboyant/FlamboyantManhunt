package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.application.HandlerRegistration;
import me.flamboyant.manhunt.domain.event.MockEventPublisher;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameSessionTest {

    private GameSession session;
    private Player mockPlayer;
    private AManhuntRole mockRole;

    @BeforeEach
    public void setUp() {
        session = new GameSession(GameSessionId.generate());
        mockPlayer = mock(Player.class);
        mockRole = mock(AManhuntRole.class);
    }

    @Test
    public void testConstructor_createsSessionWithId() {
        GameSessionId id = GameSessionId.generate();
        GameSession session = new GameSession(id);

        assertNotNull(session);
        assertEquals(id, session.getId());
    }

    @Test
    public void testConstructor_rejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new GameSession(null));
    }

    @Test
    public void testAssignRole_storesRoleForPlayer() {
        session.assignRole(mockPlayer, mockRole);

        assertTrue(session.hasRole(mockPlayer));
        assertEquals(mockRole, session.getRole(mockPlayer));
    }

    @Test
    public void testAssignRole_replacesExistingRole() {
        AManhuntRole role1 = mock(AManhuntRole.class);
        AManhuntRole role2 = mock(AManhuntRole.class);

        session.assignRole(mockPlayer, role1);
        session.assignRole(mockPlayer, role2);

        assertEquals(role2, session.getRole(mockPlayer));
    }

    @Test
    public void testAssignRole_rejectsNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> session.assignRole(null, mockRole));
    }

    @Test
    public void testAssignRole_rejectsNullRole() {
        assertThrows(IllegalArgumentException.class, () -> session.assignRole(mockPlayer, null));
    }

    @Test
    public void testGetRole_returnsNullForUnassignedPlayer() {
        assertNull(session.getRole(mockPlayer));
        assertFalse(session.hasRole(mockPlayer));
    }

    @Test
    public void testGetPlayers_returnsAllPlayers() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        AManhuntRole role1 = mock(AManhuntRole.class);
        AManhuntRole role2 = mock(AManhuntRole.class);

        session.assignRole(player1, role1);
        session.assignRole(player2, role2);

        Set<Player> players = session.getPlayers();
        assertEquals(2, players.size());
        assertTrue(players.contains(player1));
        assertTrue(players.contains(player2));
    }

    @Test
    public void testGetPlayers_returnsUnmodifiableSet() {
        session.assignRole(mockPlayer, mockRole);
        Set<Player> players = session.getPlayers();

        try {
            players.clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetAllRoles_returnsAllMappings() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        AManhuntRole role1 = mock(AManhuntRole.class);
        AManhuntRole role2 = mock(AManhuntRole.class);

        session.assignRole(player1, role1);
        session.assignRole(player2, role2);

        Map<Player, AManhuntRole> roles = session.getAllRoles();
        assertEquals(2, roles.size());
        assertEquals(role1, roles.get(player1));
        assertEquals(role2, roles.get(player2));
    }

    @Test
    public void testGetAllRoles_returnsUnmodifiableMap() {
        session.assignRole(mockPlayer, mockRole);
        Map<Player, AManhuntRole> roles = session.getAllRoles();

        try {
            roles.clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testRecordPortalEntry_storesLocationByEnvironment() {
        Location overworldLoc = mock(Location.class);
        Location netherLoc = mock(Location.class);

        session.recordPortalEntry(mockPlayer, overworldLoc, World.Environment.NORMAL);
        session.recordPortalEntry(mockPlayer, netherLoc, World.Environment.NETHER);

        assertEquals(overworldLoc, session.getPortalLocation(mockPlayer, World.Environment.NORMAL));
        assertEquals(netherLoc, session.getPortalLocation(mockPlayer, World.Environment.NETHER));
    }

    @Test
    public void testRecordPortalEntry_replacesExistingLocation() {
        Location loc1 = mock(Location.class);
        Location loc2 = mock(Location.class);

        session.recordPortalEntry(mockPlayer, loc1, World.Environment.NORMAL);
        session.recordPortalEntry(mockPlayer, loc2, World.Environment.NORMAL);

        assertEquals(loc2, session.getPortalLocation(mockPlayer, World.Environment.NORMAL));
    }

    @Test
    public void testRecordPortalEntry_rejectsNullPlayer() {
        assertThrows(IllegalArgumentException.class,
            () -> session.recordPortalEntry(null, mock(Location.class), World.Environment.NORMAL));
    }

    @Test
    public void testRecordPortalEntry_rejectsNullLocation() {
        assertThrows(IllegalArgumentException.class,
            () -> session.recordPortalEntry(mockPlayer, null, World.Environment.NORMAL));
    }

    @Test
    public void testRecordPortalEntry_rejectsNullEnvironment() {
        assertThrows(IllegalArgumentException.class,
            () -> session.recordPortalEntry(mockPlayer, mock(Location.class), null));
    }

    @Test
    public void testGetPortalLocation_returnsNullForUnrecordedPlayer() {
        assertNull(session.getPortalLocation(mockPlayer, World.Environment.NORMAL));
    }

    @Test
    public void testSpeedrunnerCount_setAndGet() {
        session.setRemainingSpeedrunners(3);
        assertEquals(3, session.getRemainingSpeedrunners());
    }

    @Test
    public void testSpeedrunnerCount_defaultsToZero() {
        assertEquals(0, session.getRemainingSpeedrunners());
    }

    @Test
    public void testDecrementSpeedrunners_decreasesCount() {
        session.setRemainingSpeedrunners(3);

        assertEquals(2, session.decrementSpeedrunners());
        assertEquals(2, session.getRemainingSpeedrunners());
        assertEquals(1, session.decrementSpeedrunners());
        assertEquals(0, session.decrementSpeedrunners());
    }

    @Test
    public void testDecrementSpeedrunners_doesNotGoBelowZero() {
        session.setRemainingSpeedrunners(0);
        assertEquals(0, session.decrementSpeedrunners());
        assertEquals(0, session.getRemainingSpeedrunners());
    }

    @Test
    public void testClear_resetsAllState() {
        // Set up state
        session.assignRole(mockPlayer, mockRole);
        session.recordPortalEntry(mockPlayer, mock(Location.class), World.Environment.NORMAL);
        session.setRemainingSpeedrunners(5);

        // Clear
        session.clear();

        // Verify all cleared
        assertFalse(session.hasRole(mockPlayer));
        assertNull(session.getPortalLocation(mockPlayer, World.Environment.NORMAL));
        assertEquals(0, session.getRemainingSpeedrunners());
        assertEquals(0, session.getPlayers().size());
    }

    @Test
    public void testSessionHasEventPublisher() {
        GameSession session = new GameSession(GameSessionId.generate());

        assertNotNull(session.getEventPublisher());
    }

    @Test
    public void testSessionConstructorAcceptsCustomEventPublisher() {
        GameSessionId id = GameSessionId.generate();
        MockEventPublisher mockPublisher = new MockEventPublisher();

        GameSession session = new GameSession(id, new InMemoryPortalTracker(), mockPublisher);

        assertEquals(mockPublisher, session.getEventPublisher());
    }

    @Test
    public void testSessionConstructorRejectsNullEventPublisher() {
        GameSessionId id = GameSessionId.generate();

        assertThrows(IllegalArgumentException.class,
            () -> new GameSession(id, new InMemoryPortalTracker(), null));
    }

    @Test
    public void testSetAndGetHandlerRegistration() {
        HandlerRegistration registration = mock(HandlerRegistration.class);
        session.setHandlerRegistration(registration);
        assertEquals(registration, session.getHandlerRegistration());
    }

    @Test
    public void testGetHandlerRegistrationReturnsNullByDefault() {
        assertNull(session.getHandlerRegistration());
    }

    @Test
    public void testEndWithNullHandlerRegistrationDoesNotThrow() {
        session.setHandlerRegistration(null);
        session.end(); // Should not throw NPE
        // Verify session still ends properly
        assertTrue(session.getPlayers().isEmpty());
    }
}
