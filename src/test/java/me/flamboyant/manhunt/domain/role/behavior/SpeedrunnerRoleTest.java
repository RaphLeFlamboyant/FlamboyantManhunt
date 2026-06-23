package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpeedrunnerRoleTest {

    private SpeedrunnerRole role;
    private Player mockPlayer;

    @Before
    public void setUp() {
        mockPlayer = mock(Player.class);
        role = new SpeedrunnerRole(mockPlayer);
    }

    @Test
    public void testHandleDamage_playerDies_whenHealthDropsToZero() {
        // Arrange
        double currentHealth = 10.0;
        double damageAmount = 10.0;
        when(mockPlayer.getHealth()).thenReturn(currentHealth);

        // Act
        DamageOutcome outcome = role.handleDamage(damageAmount);

        // Assert
        assertTrue("Player should have died", outcome.isDied());
        assertEquals("Remaining health should be 0", 0.0, outcome.getRemainingHealth(), 0.001);
    }

    @Test
    public void testHandleDamage_playerSurvives_whenHealthRemainsPositive() {
        // Arrange
        double currentHealth = 20.0;
        double damageAmount = 10.0;
        when(mockPlayer.getHealth()).thenReturn(currentHealth);

        // Act
        DamageOutcome outcome = role.handleDamage(damageAmount);

        // Assert
        assertFalse("Player should have survived", outcome.isDied());
        assertEquals("Remaining health should be 10", 10.0, outcome.getRemainingHealth(), 0.001);
    }

    @Test
    public void testHandleDamage_playerDies_whenDamageExceedsHealth() {
        // Arrange
        double currentHealth = 5.0;
        double damageAmount = 15.0;
        when(mockPlayer.getHealth()).thenReturn(currentHealth);

        // Act
        DamageOutcome outcome = role.handleDamage(damageAmount);

        // Assert
        assertTrue("Player should have died from overkill", outcome.isDied());
        assertEquals("Remaining health should be negative", -10.0, outcome.getRemainingHealth(), 0.001);
    }

    @Test
    public void testCalculateCompassTarget_sameDimension_returnsTargetLocation() {
        // Arrange
        Player targetPlayer = mock(Player.class);
        World mockWorld = mock(World.class);
        Location targetLocation = new Location(mockWorld, 100, 64, 200);

        when(mockPlayer.getWorld()).thenReturn(mockWorld);
        when(targetPlayer.getWorld()).thenReturn(mockWorld);
        when(targetPlayer.getLocation()).thenReturn(targetLocation);

        GameSession mockSession = new GameSession(GameSessionId.generate());

        // Act
        CompassTarget target = role.calculateCompassTarget(targetPlayer, mockSession);

        // Assert
        assertNotNull("Compass target should not be null", target);
        assertEquals("Target should be in same dimension", targetLocation, target.getLocation());
        assertFalse("Should not be cross-dimension", target.isCrossDimension());
    }

    @Test
    public void testCalculateCompassTarget_differentDimension_returnsPortalLocation() {
        // Arrange
        Player targetPlayer = mock(Player.class);
        World ownerWorld = mock(World.class);
        World targetWorld = mock(World.class);
        Location targetLocation = new Location(targetWorld, 100, 64, 200);
        Location portalLocation = new Location(ownerWorld, 80, 65, 160);

        when(ownerWorld.getName()).thenReturn("world");
        when(targetWorld.getName()).thenReturn("world_nether");
        when(mockPlayer.getWorld()).thenReturn(ownerWorld);
        when(targetPlayer.getWorld()).thenReturn(targetWorld);
        when(targetPlayer.getLocation()).thenReturn(targetLocation);

        GameSession mockSession = mock(GameSession.class);
        when(mockSession.getPortalLocation(targetPlayer, World.Environment.NORMAL))
            .thenReturn(portalLocation);

        // Act
        CompassTarget target = role.calculateCompassTarget(targetPlayer, mockSession);

        // Assert
        assertNotNull("Compass target should not be null", target);
        assertEquals("Should use portal location for cross-dimension", portalLocation, target.getLocation());
        assertTrue("Should be marked as cross-dimension", target.isCrossDimension());
        assertEquals("Should indicate target dimension", "world_nether", target.getTargetDimensionName());
    }
}
