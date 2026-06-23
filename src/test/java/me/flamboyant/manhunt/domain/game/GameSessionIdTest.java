package me.flamboyant.manhunt.domain.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameSessionIdTest {

    @Test
    public void testGenerate_createsNonNullId() {
        GameSessionId id = GameSessionId.generate();

        assertNotNull(id);
        assertNotNull(id.getValue());
        assertFalse(id.getValue().isEmpty());
    }

    @Test
    public void testGenerate_createsUniqueIds() {
        GameSessionId id1 = GameSessionId.generate();
        GameSessionId id2 = GameSessionId.generate();

        assertNotEquals(id1, id2);
        assertNotEquals(id1.getValue(), id2.getValue());
    }

    @Test
    public void testEquals_sameValue() {
        String value = "test-session-123";
        GameSessionId id1 = new GameSessionId(value);
        GameSessionId id2 = new GameSessionId(value);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testEquals_differentValue() {
        GameSessionId id1 = new GameSessionId("session-1");
        GameSessionId id2 = new GameSessionId("session-2");

        assertNotEquals(id1, id2);
    }

    @Test
    public void testToString_returnsValue() {
        String value = "session-abc";
        GameSessionId id = new GameSessionId(value);

        assertEquals("GameSessionId{" + value + "}", id.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_rejectsNull() {
        new GameSessionId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_rejectsEmpty() {
        new GameSessionId("");
    }
}
