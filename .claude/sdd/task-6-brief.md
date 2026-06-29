# Task 6: GameSession Event Publisher Integration

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`
- Test: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java` (existing)

**Interfaces:**
- Consumes: `DomainEventPublisher`, `InMemoryEventPublisher` from Task 2
- Produces: `GameSession` with `eventPublisher` field, updated constructors, `getEventPublisher()` method

---

**Step 1: Write failing test for event publisher in GameSession**

Add to existing `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java`:

```java
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

@Test(expected = IllegalArgumentException.class)
public void testSessionConstructorRejectsNullEventPublisher() {
    GameSessionId id = GameSessionId.generate();
    
    new GameSession(id, new InMemoryPortalTracker(), null);
}
```

Note: MockEventPublisher will be created in Task 11, but for now the test will fail at compilation.

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GameSessionTest#testSessionHasEventPublisher`

Expected output:
```
[ERROR] Compilation failure: method getEventPublisher() is undefined for type GameSession
```

**Step 3: Add eventPublisher field and update GameSession constructors**

Modify `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`:

Find the existing field declarations:
```java
public class GameSession {
    private final GameSessionId id;
    private final Map<Player, AManhuntRole> playerRoles;
    private final PortalTracker portalTracker;
    private int remainingSpeedrunners;
```

Add the new field:
```java
public class GameSession {
    private final GameSessionId id;
    private final Map<Player, AManhuntRole> playerRoles;
    private final PortalTracker portalTracker;
    private final DomainEventPublisher eventPublisher;
    private int remainingSpeedrunners;
```

Add import at top of file:
```java
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
```

Find the existing constructors:
```java
    // Constructor with default PortalTracker (for backward compatibility)
    public GameSession(GameSessionId id) {
        this(id, new InMemoryPortalTracker());
    }

    // Constructor with dependency injection
    public GameSession(GameSessionId id, PortalTracker portalTracker) {
        if (id == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (portalTracker == null) {
            throw new IllegalArgumentException("PortalTracker cannot be null");
        }
        this.id = id;
        this.playerRoles = new HashMap<>();
        this.portalTracker = portalTracker;
        this.remainingSpeedrunners = 0;
    }
```

Replace with:
```java
    // Constructor with defaults (for backward compatibility)
    public GameSession(GameSessionId id) {
        this(id, new InMemoryPortalTracker(), new InMemoryEventPublisher());
    }

    // Constructor with dependency injection (for testing)
    public GameSession(GameSessionId id, PortalTracker portalTracker, DomainEventPublisher eventPublisher) {
        if (id == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (portalTracker == null) {
            throw new IllegalArgumentException("PortalTracker cannot be null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("EventPublisher cannot be null");
        }
        this.id = id;
        this.playerRoles = new HashMap<>();
        this.portalTracker = portalTracker;
        this.eventPublisher = eventPublisher;
        this.remainingSpeedrunners = 0;
    }
```

Add getter method after `getId()`:
```java
    public DomainEventPublisher getEventPublisher() {
        return eventPublisher;
    }
```

**Step 4: Update end() method to cleanup handlers**

Find the existing `end()` method in GameSession.java:
```java
    public void end() {
        clear();
    }
```

Replace with:
```java
    public void end() {
        eventPublisher.unsubscribeAll(); // Cleanup handlers to prevent leaks
        clear();
    }
```

**Step 5: Verify compilation**

Run: `mvn compile`

Expected output:
```
[INFO] BUILD SUCCESS
```

**Step 6: Run existing GameSession tests to verify no regressions**

Run: `mvn test -Dtest=GameSessionTest`

Expected output:
```
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
```

(Test count should be 3 higher than before due to new tests added in Step 1)

**Step 7: Commit GameSession event publisher integration**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java
git add src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java
git commit -m "feat(domain): integrate event publisher into GameSession

- Add eventPublisher field to GameSession
- Update constructors with dependency injection support
- Add getEventPublisher() accessor method
- Update end() to call unsubscribeAll() for cleanup
- Add tests for event publisher integration"
```
