package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class PortalTrackerTest {

    private PortalTracker tracker;
    private Player player;
    private Location location;

    @Before
    public void setUp() {
        tracker = new InMemoryPortalTracker();
        player = mock(Player.class);
        location = mock(Location.class);
    }

    @Test
    public void shouldStoreAndRetrievePortalLocation() {
        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, location);
        Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);

        // Then
        assertTrue(result.isPresent());
        assertEquals(location, result.get());
    }

    @Test
    public void shouldReturnEmptyWhenNoPortalExists() {
        // When
        Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldStoreMultipleDimensionsPerPlayer() {
        Location netherPortal = mock(Location.class);
        Location endPortal = mock(Location.class);

        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, netherPortal);
        tracker.setPortalLocation(player, World.Environment.THE_END, endPortal);

        // Then
        assertEquals(netherPortal, tracker.getPortalLocation(player, World.Environment.NETHER).get());
        assertEquals(endPortal, tracker.getPortalLocation(player, World.Environment.THE_END).get());
    }

    @Test
    public void shouldClearAllPortalsForPlayer() {
        // Given
        tracker.setPortalLocation(player, World.Environment.NETHER, location);
        tracker.setPortalLocation(player, World.Environment.THE_END, location);

        // When
        tracker.clearPortals(player);

        // Then
        assertFalse(tracker.getPortalLocation(player, World.Environment.NETHER).isPresent());
        assertFalse(tracker.getPortalLocation(player, World.Environment.THE_END).isPresent());
    }

    @Test
    public void shouldCheckPortalExistence() {
        // Given
        tracker.setPortalLocation(player, World.Environment.NETHER, location);

        // Then
        assertTrue(tracker.hasPortal(player, World.Environment.NETHER));
        assertFalse(tracker.hasPortal(player, World.Environment.THE_END));
    }

    @Test
    public void shouldOverwriteExistingPortal() {
        Location oldLocation = mock(Location.class);
        Location newLocation = mock(Location.class);

        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, oldLocation);
        tracker.setPortalLocation(player, World.Environment.NETHER, newLocation);

        // Then
        Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);
        assertTrue(result.isPresent());
        assertEquals(newLocation, result.get());
    }

    @Test
    public void shouldIsolatePortalsBetweenPlayers() {
        Player player2 = mock(Player.class);
        Location location2 = mock(Location.class);

        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, location);
        tracker.setPortalLocation(player2, World.Environment.NETHER, location2);

        // Then
        assertEquals(location, tracker.getPortalLocation(player, World.Environment.NETHER).get());
        assertEquals(location2, tracker.getPortalLocation(player2, World.Environment.NETHER).get());
    }
}
