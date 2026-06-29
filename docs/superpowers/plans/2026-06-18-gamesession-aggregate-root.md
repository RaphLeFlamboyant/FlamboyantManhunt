# GameSession Aggregate Root Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace global static `GameData` with encapsulated `GameSession` aggregate root to enable proper lifecycle management, support multiple concurrent games, and establish DDD foundation.

**Architecture:** Create `GameSession` as the aggregate root containing all game state (player-role mappings, portal locations, counters). Introduce `GameSessionManager` for lifecycle management. Add temporary adapter layer in `GameData` for backward compatibility during migration. Gradually migrate all consumers to use `GameSession`, then remove adapter.

**Tech Stack:** Java 8, Spigot 1.20.1, Maven, JUnit (for tests)

## Global Constraints

- Java version: 1.8
- Spigot API: 1.20.1-R0.1-SNAPSHOT
- Maven project structure
- Package root: `me.flamboyant.manhunt`
- All game state must be encapsulated (no public fields)
- Support concurrent game sessions
- Backward compatibility during migration (adapter pattern)
- All changes must have tests before implementation (TDD)
- Commit after each passing test

---

## File Structure

### New Files
```
src/main/java/me/flamboyant/manhunt/domain/
└── game/
    ├── GameSession.java          # Aggregate root containing game state
    └── GameSessionId.java        # Value object for session identity

src/main/java/me/flamboyant/manhunt/application/
└── GameSessionManager.java       # Manages session lifecycle

src/test/java/me/flamboyant/manhunt/domain/game/
├── GameSessionTest.java          # Unit tests for GameSession
└── GameSessionIdTest.java        # Unit tests for GameSessionId

src/test/java/me/flamboyant/manhunt/application/
└── GameSessionManagerTest.java   # Unit tests for GameSessionManager
```

### Modified Files
```
src/main/java/me/flamboyant/manhunt/
├── GameData.java                 # Add adapter methods (Tasks 4, 8)
├── NewManhuntManager.java        # Use GameSession (Task 5)
└── NewManhuntLauncher.java       # Create GameSession (Task 6)

src/main/java/me/flamboyant/manhunt/roles/impl/
├── SpeedrunnerRole.java          # Use session context (Task 7)
└── HunterRole.java               # Use session context (Task 7)
```

---

## Task 1: Create GameSessionId Value Object

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/game/GameSessionId.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionIdTest.java`

**Interfaces:**
- Consumes: Nothing (foundational)
- Produces: `GameSessionId` value object with:
  - `String getValue()` - returns the session ID string
  - `boolean equals(Object)` - value equality
  - `int hashCode()` - hash based on value
  - `String toString()` - string representation
  - Static factory: `GameSessionId generate()` - creates unique ID

---

- [ ] **Step 1.1: Create test directory structure**

Run:
```bash
mkdir -p "src/test/java/me/flamboyant/manhunt/domain/game"
```

Expected: Directory created

- [ ] **Step 1.2: Write failing test for GameSessionId creation**

Create: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionIdTest.java`

```java
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
```

- [ ] **Step 1.3: Run test to verify it fails**

Run:
```bash
mvn test -Dtest=GameSessionIdTest
```

Expected: Compilation error - `GameSessionId` class does not exist

- [ ] **Step 1.4: Create domain directory structure**

Run:
```bash
mkdir -p "src/main/java/me/flamboyant/manhunt/domain/game"
```

Expected: Directory created

- [ ] **Step 1.5: Implement GameSessionId**

Create: `src/main/java/me/flamboyant/manhunt/domain/game/GameSessionId.java`

```java
package me.flamboyant.manhunt.domain.game;

import java.util.UUID;

public final class GameSessionId {
    private final String value;
    
    public GameSessionId(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        this.value = value;
    }
    
    public static GameSessionId generate() {
        return new GameSessionId("session-" + UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameSessionId that = (GameSessionId) obj;
        return value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return "GameSessionId{" + value + "}";
    }
}
```

- [ ] **Step 1.6: Run test to verify it passes**

Run:
```bash
mvn test -Dtest=GameSessionIdTest
```

Expected: All 7 tests pass

- [ ] **Step 1.7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/game/GameSessionId.java
git add src/test/java/me/flamboyant/manhunt/domain/game/GameSessionIdTest.java
git commit -m "feat(domain): add GameSessionId value object

- Create GameSessionId as immutable value object
- Add generate() factory method using UUID
- Implement value equality and hashCode
- Add validation for null/empty values
- Add comprehensive unit tests"
```

---

## Task 2: Create GameSession Aggregate Root

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java`

**Interfaces:**
- Consumes: `GameSessionId` from Task 1
- Produces: `GameSession` aggregate root with:
  - `GameSessionId getId()` - session identifier
  - `void assignRole(Player, AManhuntRole)` - assign role to player
  - `AManhuntRole getRole(Player)` - get player's role
  - `boolean hasRole(Player)` - check if player has role
  - `Set<Player> getPlayers()` - get all players
  - `Map<Player, AManhuntRole> getAllRoles()` - get all role assignments
  - `void recordPortalEntry(Player, Location, World.Environment)` - track portal locations
  - `Location getPortalLocation(Player, World.Environment)` - get stored portal location
  - `void setRemainingSpeedrunners(int)` - set speedrunner count
  - `int getRemainingSpeedrunners()` - get speedrunner count
  - `int decrementSpeedrunners()` - decrement and return new count
  - `void clear()` - reset all state

---

- [ ] **Step 2.1: Write failing test for GameSession creation**

Create: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java`

```java
package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameSessionTest {
    
    private GameSession session;
    private Player mockPlayer;
    private AManhuntRole mockRole;
    
    @Before
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
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_rejectsNullId() {
        new GameSession(null);
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
    
    @Test(expected = IllegalArgumentException.class)
    public void testAssignRole_rejectsNullPlayer() {
        session.assignRole(null, mockRole);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAssignRole_rejectsNullRole() {
        session.assignRole(mockPlayer, null);
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
    
    @Test(expected = IllegalArgumentException.class)
    public void testRecordPortalEntry_rejectsNullPlayer() {
        session.recordPortalEntry(null, mock(Location.class), World.Environment.NORMAL);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRecordPortalEntry_rejectsNullLocation() {
        session.recordPortalEntry(mockPlayer, null, World.Environment.NORMAL);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRecordPortalEntry_rejectsNullEnvironment() {
        session.recordPortalEntry(mockPlayer, mock(Location.class), null);
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
}
```

- [ ] **Step 2.2: Run test to verify it fails**

Run:
```bash
mvn test -Dtest=GameSessionTest
```

Expected: Compilation error - `GameSession` class does not exist

- [ ] **Step 2.3: Add Mockito dependency to pom.xml**

Modify: `pom.xml` - Add to `<dependencies>` section:

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>3.12.4</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2.4: Implement GameSession aggregate root**

Create: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`

```java
package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.roles.AManhuntRole;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSession {
    private final GameSessionId id;
    private final Map<Player, AManhuntRole> playerRoles;
    private final Map<Player, Map<World.Environment, Location>> portalLocations;
    private int remainingSpeedrunners;
    
    public GameSession(GameSessionId id) {
        if (id == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        this.id = id;
        this.playerRoles = new HashMap<>();
        this.portalLocations = new HashMap<>();
        this.remainingSpeedrunners = 0;
    }
    
    public GameSessionId getId() {
        return id;
    }
    
    public void assignRole(Player player, AManhuntRole role) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        playerRoles.put(player, role);
    }
    
    public AManhuntRole getRole(Player player) {
        return playerRoles.get(player);
    }
    
    public boolean hasRole(Player player) {
        return playerRoles.containsKey(player);
    }
    
    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(playerRoles.keySet());
    }
    
    public Map<Player, AManhuntRole> getAllRoles() {
        return Collections.unmodifiableMap(playerRoles);
    }
    
    public void recordPortalEntry(Player player, Location location, World.Environment environment) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }
        
        portalLocations.computeIfAbsent(player, k -> new HashMap<>())
                      .put(environment, location);
    }
    
    public Location getPortalLocation(Player player, World.Environment environment) {
        Map<World.Environment, Location> playerLocations = portalLocations.get(player);
        if (playerLocations == null) {
            return null;
        }
        return playerLocations.get(environment);
    }
    
    public void setRemainingSpeedrunners(int count) {
        this.remainingSpeedrunners = count;
    }
    
    public int getRemainingSpeedrunners() {
        return remainingSpeedrunners;
    }
    
    public int decrementSpeedrunners() {
        if (remainingSpeedrunners > 0) {
            remainingSpeedrunners--;
        }
        return remainingSpeedrunners;
    }
    
    public void clear() {
        playerRoles.clear();
        portalLocations.clear();
        remainingSpeedrunners = 0;
    }
}
```

- [ ] **Step 2.5: Run test to verify it passes**

Run:
```bash
mvn test -Dtest=GameSessionTest
```

Expected: All 24 tests pass

- [ ] **Step 2.6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java
git add src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java
git add pom.xml
git commit -m "feat(domain): add GameSession aggregate root

- Create GameSession as aggregate root for game state
- Encapsulate player-role mappings
- Encapsulate portal location tracking
- Encapsulate speedrunner counter
- Add clear() method for state reset
- Implement full encapsulation (no public fields)
- Add comprehensive unit tests with Mockito
- Add test dependencies to pom.xml"
```

---

## Task 3: Create GameSessionManager

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java`
- Create: `src/test/java/me/flamboyant/manhunt/application/GameSessionManagerTest.java`

**Interfaces:**
- Consumes: `GameSession` from Task 2, `GameSessionId` from Task 1
- Produces: `GameSessionManager` with:
  - `static GameSessionManager getInstance()` - singleton instance
  - `GameSession createSession()` - create new session with generated ID
  - `GameSession createSession(GameSessionId)` - create session with specific ID
  - `GameSession getSession(GameSessionId)` - retrieve session by ID
  - `GameSession getActiveSessionForPlayer(Player)` - find session containing player
  - `Set<GameSession> getAllSessions()` - get all active sessions
  - `void removeSession(GameSessionId)` - remove and cleanup session
  - `void removeAllSessions()` - remove all sessions
  - `int getActiveSessionCount()` - count active sessions

---

- [ ] **Step 3.1: Create test directory structure**

Run:
```bash
mkdir -p "src/test/java/me/flamboyant/manhunt/application"
```

Expected: Directory created

- [ ] **Step 3.2: Write failing test for GameSessionManager**

Create: `src/test/java/me/flamboyant/manhunt/application/GameSessionManagerTest.java`

```java
package me.flamboyant.manhunt.application;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.roles.AManhuntRole;
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
```

- [ ] **Step 3.3: Run test to verify it fails**

Run:
```bash
mvn test -Dtest=GameSessionManagerTest
```

Expected: Compilation error - `GameSessionManager` class does not exist

- [ ] **Step 3.4: Create application directory structure**

Run:
```bash
mkdir -p "src/main/java/me/flamboyant/manhunt/application"
```

Expected: Directory created

- [ ] **Step 3.5: Implement GameSessionManager**

Create: `src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java`

```java
package me.flamboyant.manhunt.application;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSessionManager {
    private static GameSessionManager instance;
    
    private final Map<GameSessionId, GameSession> sessions;
    
    private GameSessionManager() {
        this.sessions = new HashMap<>();
    }
    
    public static GameSessionManager getInstance() {
        if (instance == null) {
            instance = new GameSessionManager();
        }
        return instance;
    }
    
    public GameSession createSession() {
        GameSessionId id = GameSessionId.generate();
        return createSession(id);
    }
    
    public GameSession createSession(GameSessionId id) {
        if (sessions.containsKey(id)) {
            throw new IllegalArgumentException("Session with ID " + id + " already exists");
        }
        
        GameSession session = new GameSession(id);
        sessions.put(id, session);
        return session;
    }
    
    public GameSession getSession(GameSessionId id) {
        return sessions.get(id);
    }
    
    public GameSession getActiveSessionForPlayer(Player player) {
        for (GameSession session : sessions.values()) {
            if (session.hasRole(player)) {
                return session;
            }
        }
        return null;
    }
    
    public Set<GameSession> getAllSessions() {
        return Collections.unmodifiableSet(new HashSet<>(sessions.values()));
    }
    
    public void removeSession(GameSessionId id) {
        GameSession session = sessions.remove(id);
        if (session != null) {
            session.clear();
        }
    }
    
    public void removeAllSessions() {
        for (GameSession session : sessions.values()) {
            session.clear();
        }
        sessions.clear();
    }
    
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
```

- [ ] **Step 3.6: Run test to verify it passes**

Run:
```bash
mvn test -Dtest=GameSessionManagerTest
```

Expected: All 18 tests pass

- [ ] **Step 3.7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java
git add src/test/java/me/flamboyant/manhunt/application/GameSessionManagerTest.java
git commit -m "feat(application): add GameSessionManager

- Implement singleton GameSessionManager
- Support multiple concurrent sessions
- Track sessions by GameSessionId
- Find sessions by player membership
- Session lifecycle management (create, get, remove)
- Automatic cleanup on removal
- Comprehensive unit tests"
```

---

## Task 4: Add GameData Adapter Layer

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/GameData.java`
- Create: `src/test/java/me/flamboyant/manhunt/GameDataTest.java`

**Interfaces:**
- Consumes: `GameSession` from Task 2, `GameSessionManager` from Task 3
- Produces: Bridge methods in `GameData`:
  - `static void setCurrentSession(GameSession)` - set active session for bridge
  - `static GameSession getCurrentSession()` - get active session
  - `static AManhuntRole getRole(Player)` - bridge to session.getRole()
  - `static void assignRole(Player, AManhuntRole)` - bridge to session.assignRole()
  - `static void recordPortalEntry(Player, Location, World.Environment)` - bridge to session.recordPortalEntry()
  - `static Location getPortalLocation(Player, World.Environment)` - bridge to session.getPortalLocation()
  - `static void setRemainingSpeedrunners(int)` - bridge to session.setRemainingSpeedrunners()
  - `static int getRemainingSpeedrunners()` - bridge to session.getRemainingSpeedrunners()
  - `static int decrementSpeedrunners()` - bridge to session.decrementSpeedrunners()
  - Original fields marked `@Deprecated`

---

- [ ] **Step 4.1: Read current GameData implementation**

Read: `src/main/java/me/flamboyant/manhunt/GameData.java`

Current content:
```java
package me.flamboyant.manhunt;

import me.flamboyant.manhunt.roles.AManhuntRole;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class GameData {
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    public static HashMap<Player, Location> overworldLocationBeforePortal = new HashMap<>();
    public static HashMap<Player, Location> netherLocationBeforePortal = new HashMap<>();
    public static int remainingSpeedrunner;
}
```

- [ ] **Step 4.2: Write failing test for GameData adapter**

Create: `src/test/java/me/flamboyant/manhunt/GameDataTest.java`

```java
package me.flamboyant.manhunt;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.roles.AManhuntRole;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class GameDataTest {
    
    private GameSession session;
    private Player mockPlayer;
    private AManhuntRole mockRole;
    
    @Before
    public void setUp() {
        GameSessionManager manager = GameSessionManager.getInstance();
        manager.removeAllSessions();
        session = manager.createSession();
        GameData.setCurrentSession(session);
        
        mockPlayer = mock(Player.class);
        mockRole = mock(AManhuntRole.class);
    }
    
    @Test
    public void testSetCurrentSession_storesSession() {
        GameSession newSession = GameSessionManager.getInstance().createSession();
        GameData.setCurrentSession(newSession);
        
        assertEquals(newSession, GameData.getCurrentSession());
    }
    
    @Test
    public void testGetCurrentSession_returnsSetSession() {
        assertEquals(session, GameData.getCurrentSession());
    }
    
    @Test
    public void testAssignRole_delegatesToSession() {
        GameData.assignRole(mockPlayer, mockRole);
        
        assertTrue(session.hasRole(mockPlayer));
        assertEquals(mockRole, session.getRole(mockPlayer));
    }
    
    @Test
    public void testGetRole_delegatesToSession() {
        session.assignRole(mockPlayer, mockRole);
        
        assertEquals(mockRole, GameData.getRole(mockPlayer));
    }
    
    @Test
    public void testGetRole_returnsNullWhenNoSessionSet() {
        GameData.setCurrentSession(null);
        
        assertNull(GameData.getRole(mockPlayer));
    }
    
    @Test
    public void testGetRole_fallsBackToOldMapWhenNoSession() {
        GameData.setCurrentSession(null);
        GameData.playerClassList.put(mockPlayer, mockRole);
        
        assertEquals(mockRole, GameData.getRole(mockPlayer));
    }
    
    @Test
    public void testRecordPortalEntry_delegatesToSession() {
        Location location = mock(Location.class);
        
        GameData.recordPortalEntry(mockPlayer, location, World.Environment.NORMAL);
        
        assertEquals(location, session.getPortalLocation(mockPlayer, World.Environment.NORMAL));
    }
    
    @Test
    public void testGetPortalLocation_delegatesToSession() {
        Location location = mock(Location.class);
        session.recordPortalEntry(mockPlayer, location, World.Environment.NETHER);
        
        assertEquals(location, GameData.getPortalLocation(mockPlayer, World.Environment.NETHER));
    }
    
    @Test
    public void testGetPortalLocation_fallsBackToOldMapsWhenNoSession() {
        GameData.setCurrentSession(null);
        Location overworldLoc = mock(Location.class);
        Location netherLoc = mock(Location.class);
        
        GameData.overworldLocationBeforePortal.put(mockPlayer, overworldLoc);
        GameData.netherLocationBeforePortal.put(mockPlayer, netherLoc);
        
        assertEquals(overworldLoc, GameData.getPortalLocation(mockPlayer, World.Environment.NORMAL));
        assertEquals(netherLoc, GameData.getPortalLocation(mockPlayer, World.Environment.NETHER));
    }
    
    @Test
    public void testSetRemainingSpeedrunners_delegatesToSession() {
        GameData.setRemainingSpeedrunners(5);
        
        assertEquals(5, session.getRemainingSpeedrunners());
    }
    
    @Test
    public void testGetRemainingSpeedrunners_delegatesToSession() {
        session.setRemainingSpeedrunners(7);
        
        assertEquals(7, GameData.getRemainingSpeedrunners());
    }
    
    @Test
    public void testGetRemainingSpeedrunners_fallsBackToOldFieldWhenNoSession() {
        GameData.setCurrentSession(null);
        GameData.remainingSpeedrunner = 3;
        
        assertEquals(3, GameData.getRemainingSpeedrunners());
    }
    
    @Test
    public void testDecrementSpeedrunners_delegatesToSession() {
        session.setRemainingSpeedrunners(4);
        
        assertEquals(3, GameData.decrementSpeedrunners());
        assertEquals(3, session.getRemainingSpeedrunners());
    }
}
```

- [ ] **Step 4.3: Run test to verify it fails**

Run:
```bash
mvn test -Dtest=GameDataTest
```

Expected: Compilation errors - adapter methods don't exist

- [ ] **Step 4.4: Implement GameData adapter layer**

Modify: `src/main/java/me/flamboyant/manhunt/GameData.java`

Replace entire file with:
```java
package me.flamboyant.manhunt;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.roles.AManhuntRole;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * Adapter layer for backward compatibility during migration to GameSession.
 * 
 * DEPRECATED: This class provides bridge methods to the new GameSession aggregate.
 * Direct usage of static fields is deprecated. Use GameSession directly.
 * 
 * Migration Path:
 * 1. Old code uses static fields directly
 * 2. New code uses GameSession via adapter methods
 * 3. Migrate all callers to use GameSession directly
 * 4. Remove this class entirely
 */
public class GameData {
    
    // Current active session for backward compatibility
    private static GameSession currentSession;
    
    // Old static fields - DEPRECATED, kept for fallback during migration
    /**
     * @deprecated Use GameSession.assignRole() and GameSession.getRole() instead
     */
    @Deprecated
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    
    /**
     * @deprecated Use GameSession.recordPortalEntry() and GameSession.getPortalLocation() instead
     */
    @Deprecated
    public static HashMap<Player, Location> overworldLocationBeforePortal = new HashMap<>();
    
    /**
     * @deprecated Use GameSession.recordPortalEntry() and GameSession.getPortalLocation() instead
     */
    @Deprecated
    public static HashMap<Player, Location> netherLocationBeforePortal = new HashMap<>();
    
    /**
     * @deprecated Use GameSession.setRemainingSpeedrunners() and GameSession.getRemainingSpeedrunners() instead
     */
    @Deprecated
    public static int remainingSpeedrunner;
    
    // Adapter methods
    
    public static void setCurrentSession(GameSession session) {
        currentSession = session;
    }
    
    public static GameSession getCurrentSession() {
        return currentSession;
    }
    
    public static void assignRole(Player player, AManhuntRole role) {
        if (currentSession != null) {
            currentSession.assignRole(player, role);
        } else {
            playerClassList.put(player, role);
        }
    }
    
    public static AManhuntRole getRole(Player player) {
        if (currentSession != null) {
            return currentSession.getRole(player);
        }
        return playerClassList.get(player);
    }
    
    public static void recordPortalEntry(Player player, Location location, World.Environment environment) {
        if (currentSession != null) {
            currentSession.recordPortalEntry(player, location, environment);
        } else {
            // Fallback to old maps
            if (environment == World.Environment.NORMAL) {
                overworldLocationBeforePortal.put(player, location);
            } else if (environment == World.Environment.NETHER) {
                netherLocationBeforePortal.put(player, location);
            }
        }
    }
    
    public static Location getPortalLocation(Player player, World.Environment environment) {
        if (currentSession != null) {
            return currentSession.getPortalLocation(player, environment);
        }
        // Fallback to old maps
        if (environment == World.Environment.NORMAL) {
            return overworldLocationBeforePortal.get(player);
        } else if (environment == World.Environment.NETHER) {
            return netherLocationBeforePortal.get(player);
        }
        return null;
    }
    
    public static void setRemainingSpeedrunners(int count) {
        if (currentSession != null) {
            currentSession.setRemainingSpeedrunners(count);
        } else {
            remainingSpeedrunner = count;
        }
    }
    
    public static int getRemainingSpeedrunners() {
        if (currentSession != null) {
            return currentSession.getRemainingSpeedrunners();
        }
        return remainingSpeedrunner;
    }
    
    public static int decrementSpeedrunners() {
        if (currentSession != null) {
            return currentSession.decrementSpeedrunners();
        } else {
            if (remainingSpeedrunner > 0) {
                remainingSpeedrunner--;
            }
            return remainingSpeedrunner;
        }
    }
}
```

- [ ] **Step 4.5: Run test to verify it passes**

Run:
```bash
mvn test -Dtest=GameDataTest
```

Expected: All 13 tests pass

- [ ] **Step 4.6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/GameData.java
git add src/test/java/me/flamboyant/manhunt/GameDataTest.java
git commit -m "refactor(domain): add GameData adapter layer for migration

- Add bridge methods delegating to GameSession
- Mark old static fields as @Deprecated
- Maintain backward compatibility during migration
- Add fallback logic to old maps when no session set
- Document migration path in comments
- Add comprehensive adapter tests"
```

---

## Task 5: Update NewManhuntManager to Use GameSession

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

**Interfaces:**
- Consumes: `GameSession` from Task 2, adapter methods from Task 4
- Produces: Updated `NewManhuntManager` that:
  - Holds reference to GameSession instead of using static GameData
  - Uses `session.getRole()` instead of `GameData.playerClassList.get()`
  - Uses `session.decrementSpeedrunners()` instead of `--GameData.remainingSpeedrunner`
  - Uses `session.getAllRoles()` instead of `GameData.playerClassList`

---

- [ ] **Step 5.1: Read current NewManhuntManager implementation**

Read: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Note current GameData usage patterns

- [ ] **Step 5.2: Backup original implementation**

Run:
```bash
cp "src/main/java/me/flamboyant/manhunt/NewManhuntManager.java" "src/main/java/me/flamboyant/manhunt/NewManhuntManager.java.backup"
```

Expected: Backup created

- [ ] **Step 5.3: Refactor startGame to accept and store GameSession**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Change method signature from:
```java
public boolean startGame(int roleRevealDelayInMinutes, boolean speedrunnerSurprise)
```

To:
```java
private GameSession session;

public boolean startGame(GameSession session, int roleRevealDelayInMinutes, boolean speedrunnerSurprise)
```

Update implementation:
```java
public boolean startGame(GameSession session, int roleRevealDelayInMinutes, boolean speedrunnerSurprise) {
    this.session = session;
    session.setRemainingSpeedrunners(0);

    Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
        for (Player player : session.getPlayers()) {
            AManhuntRole role = session.getRole(player);
            if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                session.setRemainingSpeedrunners(session.getRemainingSpeedrunners() + 1);
            }
            role.start();
        }

        if (speedrunnerSurprise)
            Common.server.getPluginManager().registerEvents(this, Common.plugin);
    }, (roleRevealDelayInMinutes * 60 + 1) * 20);

    if (!speedrunnerSurprise)
        Common.server.getPluginManager().registerEvents(this, Common.plugin);

    return true;
}
```

- [ ] **Step 5.4: Update stopGame to use session**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Change stopGame implementation:
```java
public void stopGame(String reason) {
    EntityDamageEvent.getHandlerList().unregister(this);
    Bukkit.broadcastMessage(ChatHelper.importantMessage(reason));

    for (AManhuntRole role : session.getAllRoles().values()) {
        role.stop();
    }

    session.clear();
    this.session = null;
    NewManhuntLauncher.getInstance().stop();
}
```

- [ ] **Step 5.5: Update onEntityDamage to use session**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Change onEntityDamage implementation:
```java
@EventHandler
public void onEntityDamage(EntityDamageEvent event)
{
    if (event.getEntityType() != EntityType.PLAYER) return;
    Player player = (Player)event.getEntity();

    AManhuntRole role = session.getRole(player);
    if (role == null) return;

    if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER
            && player.getHealth() - event.getFinalDamage() <= 0) {
        Bukkit.broadcastMessage("Le speedrunner " + player.getDisplayName() + " est mort");
        if (session.decrementSpeedrunners() == 0) {
            Bukkit.getScheduler().runTaskLater(Common.plugin,
                    () -> stopGame("L'équipe SPEEDRUNNER a perdu !!!"),
                    1);
            return;
        }
    }
}
```

- [ ] **Step 5.6: Add null check guard in onEntityDamage**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Add guard at start of onEntityDamage:
```java
@EventHandler
public void onEntityDamage(EntityDamageEvent event)
{
    if (session == null) return; // No active game
    if (event.getEntityType() != EntityType.PLAYER) return;
    // ... rest of method
}
```

- [ ] **Step 5.7: Build to verify compilation**

Run:
```bash
mvn compile
```

Expected: Successful compilation

- [ ] **Step 5.8: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/NewManhuntManager.java
git commit -m "refactor(game): migrate NewManhuntManager to use GameSession

- Accept GameSession parameter in startGame()
- Store session as instance field
- Replace GameData.playerClassList with session.getAllRoles()
- Replace GameData.remainingSpeedrunner with session methods
- Add null check guard for session
- Remove direct GameData dependencies"
```

---

## Task 6: Update NewManhuntLauncher to Create GameSession

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

**Interfaces:**
- Consumes: `GameSession` from Task 2, `GameSessionManager` from Task 3, updated `NewManhuntManager.startGame()` from Task 5
- Produces: Updated `NewManhuntLauncher` that:
  - Creates GameSession via GameSessionManager
  - Populates session with player-role assignments
  - Passes session to NewManhuntManager.startGame()
  - Sets session in GameData adapter for backward compatibility

---

- [ ] **Step 6.1: Read current NewManhuntLauncher implementation**

Read: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Note current Launch() method structure

- [ ] **Step 6.2: Backup original implementation**

Run:
```bash
cp "src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java" "src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java.backup"
```

Expected: Backup created

- [ ] **Step 6.3: Add GameSession field and import statements**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Add imports at top:
```java
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
```

Add field to class:
```java
private GameSession currentSession;
```

- [ ] **Step 6.4: Update Launch() to create GameSession**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Change beginning of Launch() method:
```java
private boolean Launch() {
    int speedrunnerCount = speedrunnerCountParameter.getValue() == 0 ? getSpeedrunnerCount() : speedrunnerCountParameter.getValue();
    int allyCount = allyCountParameter.getValue() == 0 ? getAllyCount() : allyCountParameter.getValue();
    if (!GameRolesManagement.getInstance().setRandomRolesToEmpty(playerRoles, speedrunnerCount, allyCount, specialRolesOnlyParameter.getValue() > 0))
        return false;

    // Create new game session
    GameSessionManager sessionManager = GameSessionManager.getInstance();
    currentSession = sessionManager.createSession();
    
    // Populate session with role assignments
    for (Player player : playerRoles.keySet()) {
        AManhuntRole role = ManhuntRoleFactory.createRole(player, playerRoles.get(player).getSelectedValue());
        currentSession.assignRole(player, role);
    }
    
    // Set in GameData adapter for backward compatibility
    GameData.setCurrentSession(currentSession);

    // Reset player states
    for (Player player : Common.server.getOnlinePlayers()) {
        if (player == null) continue;
        resetPlayerState(player);
        if (resetPlayersStuffParameter.getValue() != 0) player.getInventory().clear();
    }

    // Start game with session
    return NewManhuntManager.getInstance().startGame(
        currentSession,
        minutesBeforeRolesParameter.getValue(),
        surpriseSpeedrunnerParameter.getValue() > 0
    );
}
```

- [ ] **Step 6.5: Update stop() to cleanup session**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Update stop() method:
```java
@Override
public boolean stop() {
    if (!running) {
        return false;
    }

    for (ILaunchablePlugin plugin : optionalPlugin) {
        plugin.stop();
    }
    
    // Cleanup session
    if (currentSession != null) {
        GameSessionManager.getInstance().removeSession(currentSession.getId());
        GameData.setCurrentSession(null);
        currentSession = null;
    }

    running = false;
    return true;
}
```

- [ ] **Step 6.6: Remove old GameData.playerClassList clearing**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Remove these lines from Launch() (no longer needed):
```java
GameData.playerClassList.clear();
for (Player player : playerRoles.keySet()) {
    GameData.playerClassList.put(player, ManhuntRoleFactory.createRole(player, playerRoles.get(player).getSelectedValue()));
}
```

They should now be replaced by the session population code added in Step 6.4.

- [ ] **Step 6.7: Build to verify compilation**

Run:
```bash
mvn compile
```

Expected: Successful compilation

- [ ] **Step 6.8: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java
git commit -m "refactor(game): migrate NewManhuntLauncher to use GameSession

- Create GameSession via GameSessionManager in Launch()
- Populate session with player-role assignments
- Pass session to NewManhuntManager.startGame()
- Set session in GameData adapter for compatibility
- Cleanup session on stop()
- Remove direct GameData.playerClassList usage"
```

---

## Task 7: Update Role Implementations to Use Session Context

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`
- Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

**Interfaces:**
- Consumes: GameData adapter methods from Task 4
- Produces: Updated role implementations that:
  - Use `GameData.recordPortalEntry()` instead of direct map access
  - Use `GameData.getPortalLocation()` instead of direct map access
  - Use `GameData.getRole()` instead of `GameData.playerClassList.get()`

---

- [ ] **Step 7.1: Update SpeedrunnerRole portal tracking**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`

Find doStart() method and update portal initialization:
```java
@Override
protected boolean doStart() {
    winconMet = false;
    onWinConTask = null;
    
    // Use adapter methods
    GameData.recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
    GameData.recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);

    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    trackView = new PlayerSelectionView(GameData.getCurrentSession().getPlayers().stream()
        .filter(p -> p != owner).collect(Collectors.toList()), "Track Selection");
    return true;
}
```

- [ ] **Step 7.2: Update SpeedrunnerRole onEntityPortalEnter**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`

Update onEntityPortalEnter method:
```java
@EventHandler
public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();
    if (player != owner) return;
    
    String worldName = event.getLocation().getWorld().getName();
    if (worldName.equals("world")) {
        GameData.recordPortalEntry(player, event.getLocation(), World.Environment.NORMAL);
    } else if (worldName.equals("world_nether")) {
        GameData.recordPortalEntry(player, event.getLocation(), World.Environment.NETHER);
    }
}
```

- [ ] **Step 7.3: Update SpeedrunnerRole doCompassEffect**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`

Update doCompassEffect method:
```java
protected void doCompassEffect(Player target) {
    Location huntedLocation = target.getLocation();
    String targetName = target.getDisplayName();
    World huntedWorld = huntedLocation.getWorld();
    if (owner.getWorld() != huntedWorld) {
        owner.sendMessage(targetName + " est dans la dimension " + huntedWorld.getName());

        String ownerWorldName = owner.getWorld().getName();
        if (ownerWorldName.equals("world")) {
            huntedLocation = GameData.getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            huntedLocation = GameData.getPortalLocation(target, World.Environment.NETHER);
        }
    }

    if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
        Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
        lodeStoneLocation.getBlock().setType(Material.LODESTONE);

        CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
        compassMeta.setLodestone(lodeStoneLocation);
        compassMeta.setLodestoneTracked(true);
        lastCompassUsed.setItemMeta(compassMeta);
    }
    else {
        CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
        compassMeta.setLodestone(null);
        compassMeta.setLodestoneTracked(false);
        lastCompassUsed.setItemMeta(compassMeta);
        owner.setCompassTarget(huntedLocation);
    }
}
```

- [ ] **Step 7.4: Update HunterRole doStart to use GameData adapter**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

Update doStart() method:
```java
@Override
protected boolean doStart() {
    // Get speedrunners from current session
    GameSession session = GameData.getCurrentSession();
    speedrunnerList = new ArrayList<>();
    for (Player player : session.getPlayers()) {
        AManhuntRole role = session.getRole(player);
        if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            speedrunnerList.add(player);
        }
    }

    ItemStack item = new ItemStack(Material.COMPASS);
    owner.getInventory().addItem(item);

    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    return true;
}
```

- [ ] **Step 7.5: Update HunterRole broadcastPlayerResultMessage**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

Update method to use GameData adapter:
```java
@Override
protected void broadcastPlayerResultMessage() {
    boolean wincon = GameData.getRemainingSpeedrunners() == 0;

    for (IHunterWinConditionModifier modifier : winconModifiers) {
        wincon &= modifier.isHunterWinPossible();
    }

    Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (wincon ? "gagné" : "perdu") + " !"));
}
```

- [ ] **Step 7.6: Update HunterRole onPlayerInteract compass logic**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

Update onPlayerInteract method:
```java
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (owner != event.getPlayer()) return;
    if (!event.hasItem() || event.getItem().getType() != Material.COMPASS) return;
    if (owner.hasCooldown(Material.COMPASS)) return;

    owner.setCooldown(Material.COMPASS, 30 * 20);

    if (++targetIndex >= speedrunnerList.size())
        targetIndex = 0;

    Player target = speedrunnerList.get(targetIndex);
    logCompassUse(owner);
    Location huntedLocation = target.getLocation();
    World huntedWorld = huntedLocation.getWorld();
    if (owner.getWorld() != huntedWorld) {
        owner.sendMessage("Le speedrunner est dans la dimension " + huntedWorld.getName());

        String ownerWorldName = owner.getWorld().getName();
        if (ownerWorldName.equals("world")) {
            huntedLocation = GameData.getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            huntedLocation = GameData.getPortalLocation(target, World.Environment.NETHER);
        }
    }

    if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
        Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
        lodeStoneLocation.getBlock().setType(Material.LODESTONE);

        CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
        compassMeta.setLodestone(lodeStoneLocation);
        compassMeta.setLodestoneTracked(true);
        event.getItem().setItemMeta(compassMeta);
    }
    else {
        CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
        compassMeta.setLodestone(null);
        compassMeta.setLodestoneTracked(false);
        event.getItem().setItemMeta(compassMeta);
        owner.setCompassTarget(huntedLocation);
    }
}
```

- [ ] **Step 7.7: Build to verify compilation**

Run:
```bash
mvn compile
```

Expected: Successful compilation

- [ ] **Step 7.8: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java
git add src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java
git commit -m "refactor(roles): migrate role implementations to use GameData adapter

SpeedrunnerRole changes:
- Use GameData.recordPortalEntry() in doStart and portal event
- Use GameData.getPortalLocation() in compass effect
- Use GameData.getCurrentSession().getPlayers() for track view

HunterRole changes:
- Use GameData.getCurrentSession() to find speedrunners
- Use GameData.getRemainingSpeedrunners() in win check
- Use GameData.getPortalLocation() in compass logic

All changes maintain backward compatibility via adapter"
```

---

## Task 8: Remove GameData Adapter Layer

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/GameData.java`
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`
- Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`
- Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

**Interfaces:**
- Consumes: Existing adapter usage from Tasks 5-7
- Produces: Direct GameSession usage, GameData class deleted

---

- [ ] **Step 8.1: Search for all GameData usage**

Run:
```bash
grep -r "GameData\." src/main/java/me/flamboyant/manhunt/ --include="*.java" | grep -v ".backup"
```

Expected: List of all GameData references to migrate

- [ ] **Step 8.2: Update NewManhuntManager to remove GameData references**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

No changes needed - already uses session directly from Task 5.

Verify by reading the file:
```bash
grep "GameData" src/main/java/me/flamboyant/manhunt/NewManhuntManager.java
```

Expected: No matches

- [ ] **Step 8.3: Update NewManhuntLauncher to remove GameData.setCurrentSession()**

Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Remove these lines from Launch():
```java
// Set in GameData adapter for backward compatibility
GameData.setCurrentSession(currentSession);
```

Remove from stop():
```java
GameData.setCurrentSession(null);
```

Remove import:
```java
import me.flamboyant.manhunt.GameData;
```

- [ ] **Step 8.4: Update SpeedrunnerRole to use session directly**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`

Add session field and update constructor:
```java
private GameSession session;

public SpeedrunnerRole(Player owner) {
    super(owner);
}
```

Update doStart():
```java
@Override
protected boolean doStart() {
    winconMet = false;
    onWinConTask = null;
    
    // Get session from NewManhuntLauncher's current session
    session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
    if (session == null) {
        return false;
    }
    
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NETHER);
    session.recordPortalEntry(owner, owner.getLocation(), World.Environment.NORMAL);

    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    trackView = new PlayerSelectionView(
        session.getPlayers().stream().filter(p -> p != owner).collect(Collectors.toList()),
        "Track Selection"
    );
    return true;
}
```

Update onEntityPortalEnter:
```java
@EventHandler
public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();
    if (player != owner) return;
    
    String worldName = event.getLocation().getWorld().getName();
    if (worldName.equals("world")) {
        session.recordPortalEntry(player, event.getLocation(), World.Environment.NORMAL);
    } else if (worldName.equals("world_nether")) {
        session.recordPortalEntry(player, event.getLocation(), World.Environment.NETHER);
    }
}
```

Update doCompassEffect:
```java
protected void doCompassEffect(Player target) {
    Location huntedLocation = target.getLocation();
    String targetName = target.getDisplayName();
    World huntedWorld = huntedLocation.getWorld();
    if (owner.getWorld() != huntedWorld) {
        owner.sendMessage(targetName + " est dans la dimension " + huntedWorld.getName());

        String ownerWorldName = owner.getWorld().getName();
        if (ownerWorldName.equals("world")) {
            huntedLocation = session.getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            huntedLocation = session.getPortalLocation(target, World.Environment.NETHER);
        }
    }

    if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
        Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
        lodeStoneLocation.getBlock().setType(Material.LODESTONE);

        CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
        compassMeta.setLodestone(lodeStoneLocation);
        compassMeta.setLodestoneTracked(true);
        lastCompassUsed.setItemMeta(compassMeta);
    }
    else {
        CompassMeta compassMeta = (CompassMeta) lastCompassUsed.getItemMeta();
        compassMeta.setLodestone(null);
        compassMeta.setLodestoneTracked(false);
        lastCompassUsed.setItemMeta(compassMeta);
        owner.setCompassTarget(huntedLocation);
    }
}
```

Add import:
```java
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
```

Remove import:
```java
import me.flamboyant.manhunt.GameData;
```

- [ ] **Step 8.5: Update HunterRole to use session directly**

Modify: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`

Add session field:
```java
private GameSession session;
```

Update doStart():
```java
@Override
protected boolean doStart() {
    session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
    if (session == null) {
        return false;
    }
    
    speedrunnerList = new ArrayList<>();
    for (Player player : session.getPlayers()) {
        AManhuntRole role = session.getRole(player);
        if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
            speedrunnerList.add(player);
        }
    }

    ItemStack item = new ItemStack(Material.COMPASS);
    owner.getInventory().addItem(item);

    Common.server.getPluginManager().registerEvents(this, Common.plugin);
    return true;
}
```

Update broadcastPlayerResultMessage():
```java
@Override
protected void broadcastPlayerResultMessage() {
    boolean wincon = session != null && session.getRemainingSpeedrunners() == 0;

    for (IHunterWinConditionModifier modifier : winconModifiers) {
        wincon &= modifier.isHunterWinPossible();
    }

    Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (wincon ? "gagné" : "perdu") + " !"));
}
```

Update onPlayerInteract:
```java
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (owner != event.getPlayer()) return;
    if (!event.hasItem() || event.getItem().getType() != Material.COMPASS) return;
    if (owner.hasCooldown(Material.COMPASS)) return;

    owner.setCooldown(Material.COMPASS, 30 * 20);

    if (++targetIndex >= speedrunnerList.size())
        targetIndex = 0;

    Player target = speedrunnerList.get(targetIndex);
    logCompassUse(owner);
    Location huntedLocation = target.getLocation();
    World huntedWorld = huntedLocation.getWorld();
    if (owner.getWorld() != huntedWorld) {
        owner.sendMessage("Le speedrunner est dans la dimension " + huntedWorld.getName());

        String ownerWorldName = owner.getWorld().getName();
        if (ownerWorldName.equals("world")) {
            huntedLocation = session.getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            huntedLocation = session.getPortalLocation(target, World.Environment.NETHER);
        }
    }

    if (owner.getWorld().getName().equalsIgnoreCase("world_nether")){
        Location lodeStoneLocation = new Location(huntedLocation.getWorld(), huntedLocation.getBlockX(), 0, huntedLocation.getBlockZ());
        lodeStoneLocation.getBlock().setType(Material.LODESTONE);

        CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
        compassMeta.setLodestone(lodeStoneLocation);
        compassMeta.setLodestoneTracked(true);
        event.getItem().setItemMeta(compassMeta);
    }
    else {
        CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
        compassMeta.setLodestone(null);
        compassMeta.setLodestoneTracked(false);
        event.getItem().setItemMeta(compassMeta);
        owner.setCompassTarget(huntedLocation);
    }
}
```

Add imports:
```java
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
```

Remove import:
```java
import me.flamboyant.manhunt.GameData;
```

- [ ] **Step 8.6: Verify no remaining GameData references**

Run:
```bash
grep -r "GameData\." src/main/java/me/flamboyant/manhunt/ --include="*.java" | grep -v ".backup" | grep -v "GameDataTest"
```

Expected: No matches (except in GameData.java itself and its test)

- [ ] **Step 8.7: Delete GameData class**

Run:
```bash
git rm src/main/java/me/flamboyant/manhunt/GameData.java
git rm src/test/java/me/flamboyant/manhunt/GameDataTest.java
```

Expected: Files staged for deletion

- [ ] **Step 8.8: Build to verify compilation**

Run:
```bash
mvn clean compile
```

Expected: Successful compilation with no errors

- [ ] **Step 8.9: Run all tests**

Run:
```bash
mvn test
```

Expected: All tests pass

- [ ] **Step 8.10: Commit**

```bash
git add -A
git commit -m "refactor(domain): remove GameData adapter layer

- Remove GameData.setCurrentSession() calls from launcher
- Update SpeedrunnerRole to get session from GameSessionManager
- Update HunterRole to get session from GameSessionManager
- Roles now get session via getActiveSessionForPlayer()
- Add session field to role implementations
- Delete GameData.java and GameDataTest.java
- All code now uses GameSession directly
- Migration to aggregate root complete"
```

---

## Task 9: Integration Tests and Documentation

**Files:**
- Create: `src/test/java/me/flamboyant/manhunt/GameSessionIntegrationTest.java`
- Create: `docs/architecture/gamesession-aggregate.md`
- Modify: `REFACTORING_PROGRESS.md`

**Interfaces:**
- Consumes: All components from Tasks 1-8
- Produces: Integration tests validating full game lifecycle, architecture documentation

---

- [ ] **Step 9.1: Create integration test**

Create: `src/test/java/me/flamboyant/manhunt/GameSessionIntegrationTest.java`

```java
package me.flamboyant.manhunt;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import me.flamboyant.manhunt.roles.impl.HunterRole;
import me.flamboyant.manhunt.roles.impl.SpeedrunnerRole;
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
        assertEquals(ManhuntRoleType.SPEEDRUNNER, session.getRole(player1).getRoleType());
        
        // Replace role
        session.assignRole(player1, role2);
        assertEquals(role2, session.getRole(player1));
        assertEquals(ManhuntRoleType.HUNTER, session.getRole(player1).getRoleType());
        
        // Query all roles
        assertEquals(1, session.getAllRoles().size());
        assertEquals(role2, session.getAllRoles().get(player1));
    }
}
```

- [ ] **Step 9.2: Run integration tests**

Run:
```bash
mvn test -Dtest=GameSessionIntegrationTest
```

Expected: All 5 integration tests pass

- [ ] **Step 9.3: Run full test suite**

Run:
```bash
mvn test
```

Expected: All unit and integration tests pass

- [ ] **Step 9.4: Create architecture documentation**

Run:
```bash
mkdir -p docs/architecture
```

Create: `docs/architecture/gamesession-aggregate.md`

```markdown
# GameSession Aggregate Root Architecture

## Overview

`GameSession` is the aggregate root for all game state in the Manhunt plugin. It encapsulates player-role mappings, portal location tracking, and speedrunner counting. This document describes the architecture, design decisions, and usage patterns.

## Domain-Driven Design

### Aggregate Root

`GameSession` is an **aggregate root** in DDD terms, meaning:
- It is the single entry point for all game state operations
- All game state modifications go through GameSession methods
- No direct access to internal collections
- Enforces consistency boundaries

### Entity Identity

Each `GameSession` is uniquely identified by a `GameSessionId` value object. Session IDs are generated using UUIDs to ensure global uniqueness.

### Lifecycle

```
CREATE → POPULATE → ACTIVE → CLEANUP → REMOVED
```

1. **CREATE**: `GameSessionManager.createSession()` creates new session
2. **POPULATE**: Roles assigned via `session.assignRole()`
3. **ACTIVE**: Game running, state queried and modified
4. **CLEANUP**: `session.clear()` resets state
5. **REMOVED**: `GameSessionManager.removeSession()` removes from registry

## Components

### GameSession (Aggregate Root)

**Package**: `me.flamboyant.manhunt.domain.game`

**Responsibilities**:
- Encapsulate all game state
- Enforce invariants (e.g., speedrunner count >= 0)
- Provide typed access methods
- Prevent direct state mutation

**Key Methods**:
```java
void assignRole(Player, AManhuntRole)
AManhuntRole getRole(Player)
void recordPortalEntry(Player, Location, World.Environment)
Location getPortalLocation(Player, World.Environment)
int decrementSpeedrunners()
void clear()
```

**Invariants**:
- Session ID cannot be null
- Player and role parameters cannot be null
- Speedrunner count cannot go below zero
- Collections are unmodifiable when exposed

### GameSessionId (Value Object)

**Package**: `me.flamboyant.manhunt.domain.game`

**Responsibilities**:
- Unique session identification
- Value equality semantics
- Immutable identity

**Key Methods**:
```java
static GameSessionId generate()  // Factory method
String getValue()
```

**Characteristics**:
- Immutable
- Validates against null/empty values
- UUID-based generation

### GameSessionManager (Application Service)

**Package**: `me.flamboyant.manhunt.application`

**Responsibilities**:
- Manage session lifecycle
- Track active sessions
- Provide session lookup by ID or player

**Key Methods**:
```java
GameSession createSession()
GameSession getSession(GameSessionId)
GameSession getActiveSessionForPlayer(Player)
void removeSession(GameSessionId)
```

**Pattern**: Singleton (thread-safe lazy initialization)

## Benefits

### Before Refactoring

```java
// Global static state
public class GameData {
    public static HashMap<Player, AManhuntRole> playerClassList = new HashMap<>();
    public static int remainingSpeedrunner;
}

// Direct access anywhere
GameData.playerClassList.put(player, role);
GameData.remainingSpeedrunner--;
```

**Problems**:
- ❌ No encapsulation
- ❌ Cannot run multiple games
- ❌ Hidden dependencies
- ❌ Difficult to test
- ❌ Memory leaks

### After Refactoring

```java
// Encapsulated aggregate
GameSession session = sessionManager.createSession();
session.assignRole(player, role);
session.decrementSpeedrunners();
```

**Benefits**:
- ✅ Full encapsulation
- ✅ Support concurrent games
- ✅ Clear dependencies
- ✅ Testable in isolation
- ✅ Automatic cleanup

## Usage Patterns

### Creating a Game

```java
GameSessionManager manager = GameSessionManager.getInstance();
GameSession session = manager.createSession();

// Populate with players and roles
for (Player player : players) {
    AManhuntRole role = roleFactory.createRole(player, roleId);
    session.assignRole(player, role);
}

// Start game
gameManager.startGame(session, config);
```

### Querying Game State

```java
// Get player's role
AManhuntRole role = session.getRole(player);

// Check speedrunner count
if (session.getRemainingSpeedrunners() == 0) {
    // Hunters win
}

// Get portal location
Location portalLoc = session.getPortalLocation(player, World.Environment.NETHER);
```

### Recording Events

```java
// Player enters portal
session.recordPortalEntry(player, location, environment);

// Speedrunner dies
int remaining = session.decrementSpeedrunners();
if (remaining == 0) {
    endGame();
}
```

### Cleanup

```java
// End game - clear state
session.clear();

// Remove from manager
manager.removeSession(session.getId());
```

## Multiple Concurrent Games

```java
GameSession game1 = manager.createSession();
GameSession game2 = manager.createSession();

// Independent state
game1.setRemainingSpeedrunners(3);
game2.setRemainingSpeedrunners(5);

// Find player's game
GameSession playerSession = manager.getActiveSessionForPlayer(player);
```

## Testing

### Unit Testing

Mock Bukkit types (Player, Location, World):
```java
Player mockPlayer = mock(Player.class);
AManhuntRole mockRole = mock(AManhuntRole.class);

GameSession session = new GameSession(GameSessionId.generate());
session.assignRole(mockPlayer, mockRole);

assertEquals(mockRole, session.getRole(mockPlayer));
```

### Integration Testing

Test full lifecycle without mocks:
```java
GameSessionManager manager = GameSessionManager.getInstance();
GameSession session = manager.createSession();

// Assign real role implementations
session.assignRole(player, new SpeedrunnerRole(player));

// Verify state
assertTrue(session.hasRole(player));
```

## Design Decisions

### Why Aggregate Root?

**Problem**: Global mutable state scattered across codebase

**Solution**: Aggregate root encapsulates all game state

**Benefit**: Single source of truth, clear boundaries

### Why GameSessionManager?

**Problem**: Need to track multiple sessions

**Solution**: Centralized registry with lookup methods

**Benefit**: Easy to find sessions, manage lifecycle

### Why Value Object for ID?

**Problem**: String IDs are error-prone

**Solution**: Type-safe GameSessionId value object

**Benefit**: Type safety, validation, explicit identity

### Why Not Static?

**Problem**: Static state prevents concurrent games

**Solution**: Instance-based sessions

**Benefit**: Multiple games, better testing, cleaner lifecycle

## Future Enhancements

### Phase 2: Rich Domain Model

Currently roles are anemic - they delegate to session. Future: roles contain domain behavior.

### Phase 3: Domain Events

Publish events when state changes:
- `RoleAssignedEvent`
- `SpeedrunnerDecrementedEvent`
- `SessionClearedEvent`

### Phase 4: Persistence

Save/load sessions:
```java
sessionRepository.save(session);
session = sessionRepository.load(sessionId);
```

### Phase 5: State Machine

Add explicit game phases:
```java
session.transitionTo(GamePhase.ROLE_REVEALED);
```

## References

- **PROBLEMS_PRIORITY_SUMMARY.md**: Priority 1 analysis
- **REFACTORING_PROGRESS.md**: Implementation tracking
- **Eric Evans - Domain-Driven Design**: Aggregate pattern
- **Vaughn Vernon - Implementing DDD**: Aggregate design
```

- [ ] **Step 9.5: Update progress tracker**

Modify: `REFACTORING_PROGRESS.md`

Update Priority 1 section to mark as completed:
```markdown
### Priority 1: Create GameSession Aggregate Root ✅
**Status:** Completed  
**Assigned To:** -  
**Started:** 2026-06-18  
**Completed:** 2026-06-18  
**Estimated Effort:** 6-8 hours  
**Actual Effort:** [record actual time spent]
```

Check all task boxes:
```markdown
- [x] 1.1 Create test directory structure
- [x] 1.2 Write failing test for GameSessionId creation
...
```

Update overall progress:
```markdown
### Phase Completion
- [x] Phase 1: Core Domain (1/4 complete)
- [ ] Phase 2: Domain Events & Services (0/3 complete)
- [ ] Phase 3: Tactical Patterns (0/3 complete)
- [ ] Phase 4: Infrastructure & Polish (0/3 complete)

### Total Progress: 1/13 priorities completed (7.7%)
```

- [ ] **Step 9.6: Generate test coverage report**

Run:
```bash
mvn clean test jacoco:report
```

Expected: Coverage report generated

View: `target/site/jacoco/index.html`

- [ ] **Step 9.7: Commit documentation and progress updates**

```bash
git add src/test/java/me/flamboyant/manhunt/GameSessionIntegrationTest.java
git add docs/architecture/gamesession-aggregate.md
git add REFACTORING_PROGRESS.md
git commit -m "docs: add integration tests and architecture documentation

- Add comprehensive GameSessionIntegrationTest
- Test full game lifecycle
- Test multiple concurrent sessions
- Test portal tracking isolation
- Create architecture documentation for GameSession
- Document DDD patterns used
- Document usage patterns and design decisions
- Update REFACTORING_PROGRESS.md to mark Priority 1 complete"
```

- [ ] **Step 9.8: Tag release**

```bash
git tag -a v0.2.0-gamesession -m "Priority 1 Complete: GameSession Aggregate Root

- Implemented GameSession aggregate root
- Removed global static GameData
- Support multiple concurrent games
- Full encapsulation of game state
- Comprehensive test coverage
- Architecture documentation"

git push origin v0.2.0-gamesession
```

---

## Plan Complete

### Summary

This plan implements Priority 1: GameSession Aggregate Root through 9 tasks:

1. ✅ Create GameSessionId value object (foundational identity)
2. ✅ Create GameSession aggregate root (core state encapsulation)
3. ✅ Create GameSessionManager (lifecycle management)
4. ✅ Add GameData adapter layer (backward compatibility bridge)
5. ✅ Update NewManhuntManager (use session instead of static state)
6. ✅ Update NewManhuntLauncher (create and manage sessions)
7. ✅ Update role implementations (use session context)
8. ✅ Remove GameData adapter (complete migration)
9. ✅ Integration tests and documentation (validation)

### Validation Checklist

After completing all tasks, verify:

- [ ] All tests pass (`mvn test`)
- [ ] Build succeeds (`mvn clean package`)
- [ ] No references to `GameData` static fields (except test backups)
- [ ] Can create multiple GameSession instances
- [ ] Sessions are isolated from each other
- [ ] Game state fully encapsulated
- [ ] Documentation updated
- [ ] Progress tracker updated

### Success Criteria Met

- ✅ GameSession encapsulates all game state
- ✅ No direct access to GameData static fields
- ✅ Can create multiple GameSession instances concurrently
- ✅ All tests pass
- ✅ No static mutable state for game data
- ✅ Clear lifecycle management
- ✅ Architecture documented

### Estimated Effort vs Actual

- **Estimated**: 6-8 hours
- **Actual**: [Record after completion]

### Next Priority

After completing this plan, proceed to **Priority 2: Enrich Domain Model** (see REFACTORING_PROGRESS.md).

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-06-18-gamesession-aggregate-root.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
