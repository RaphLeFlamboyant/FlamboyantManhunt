package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameSessionEventTest {

    private GameSession session;
    private InMemoryEventPublisher publisher;
    private GameSessionId sessionId;
    private Player mockPlayer;

    @BeforeEach
    public void setUp() {
        sessionId = GameSessionId.generate();
        publisher = new InMemoryEventPublisher();
        session = new GameSession(sessionId, new InMemoryPortalTracker(), publisher);
        mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getDisplayName()).thenReturn("TestPlayer");
    }

    @Test
    public void testNotifyGameStartedPublishesEvent() {
        List<GameStartedEvent> events = new ArrayList<>();
        publisher.subscribe(GameStartedEvent.class, events::add);

        session.notifyGameStarted();

        assertEquals(1, events.size());
        assertEquals(sessionId, events.get(0).getSessionId());
    }

    @Test
    public void testNotifySpeedrunnerDiedPublishesEvent() {
        List<SpeedrunnerDiedEvent> events = new ArrayList<>();
        publisher.subscribe(SpeedrunnerDiedEvent.class, events::add);

        session.setRemainingSpeedrunners(2);
        session.notifySpeedrunnerDied(mockPlayer);

        assertEquals(1, events.size());
        assertEquals(mockPlayer, events.get(0).getPlayer());
        assertEquals(1, events.get(0).getRemainingSpeedrunners());
    }

    @Test
    public void testNotifyDragonKilledPublishesEvent() {
        List<DragonKilledEvent> events = new ArrayList<>();
        publisher.subscribe(DragonKilledEvent.class, events::add);

        session.notifyDragonKilled(mockPlayer);

        assertEquals(1, events.size());
        assertEquals(mockPlayer, events.get(0).getKiller());
    }

    @Test
    public void testNotifyDragonKilledAcceptsNullKiller() {
        List<DragonKilledEvent> events = new ArrayList<>();
        publisher.subscribe(DragonKilledEvent.class, events::add);

        session.notifyDragonKilled(null);

        assertEquals(1, events.size());
        assertNull(events.get(0).getKiller());
        assertFalse(events.get(0).hasKiller());
    }

    @Test
    public void testNotifyRolesRevealedPublishesEvent() {
        List<RolesRevealedEvent> events = new ArrayList<>();
        publisher.subscribe(RolesRevealedEvent.class, events::add);

        session.notifyRolesRevealed();

        assertEquals(1, events.size());
        assertEquals(sessionId, events.get(0).getSessionId());
    }

    @Test
    public void testNotifyGameEndedPublishesEvent() {
        List<GameEndedEvent> events = new ArrayList<>();
        publisher.subscribe(GameEndedEvent.class, events::add);

        WinOutcome outcome = WinOutcome.huntersWin("Test win");
        session.notifyGameEnded(outcome, "Test reason");

        assertEquals(1, events.size());
        assertEquals(outcome, events.get(0).getOutcome());
        assertEquals("Test reason", events.get(0).getReason());
    }

    @Test
    public void testAssignRolePublishesEvent() {
        List<RoleAssignedEvent> events = new ArrayList<>();
        publisher.subscribe(RoleAssignedEvent.class, events::add);

        SpeedrunnerRole role = new SpeedrunnerRole(mockPlayer);
        session.assignRole(mockPlayer, role);

        assertEquals(1, events.size());
        assertEquals(mockPlayer, events.get(0).getPlayer());
        assertEquals(role.getRoleIdentifier(), events.get(0).getRoleIdentifier());
    }
}
