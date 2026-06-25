# Task 3: Game Lifecycle Events

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/GameStartedEvent.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/GameEndedEvent.java`
- Test: Covered by `DomainEventTest.java` from Task 1 (base class validation)

**Interfaces:**
- Consumes: `DomainEvent` from Task 1, `WinOutcome` from `me.flamboyant.manhunt.domain.wincondition.WinOutcome`
- Produces:
  - `GameStartedEvent(sessionId, players, totalSpeedrunners)` 
  - `GameEndedEvent(sessionId, outcome, reason)`

---

**Step 1: Create GameStartedEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/GameStartedEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Published when a game session starts and all roles have been assigned.
 */
public class GameStartedEvent extends DomainEvent {
    private final Set<Player> players;
    private final int totalSpeedrunners;
    
    public GameStartedEvent(GameSessionId sessionId, Set<Player> players, int totalSpeedrunners) {
        super(sessionId);
        if (players == null) {
            throw new IllegalArgumentException("Players cannot be null");
        }
        this.players = Set.copyOf(players); // Defensive copy
        this.totalSpeedrunners = totalSpeedrunners;
    }
    
    public Set<Player> getPlayers() {
        return players;
    }
    
    public int getTotalSpeedrunners() {
        return totalSpeedrunners;
    }
}
```

**Step 2: Create GameEndedEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/GameEndedEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;

/**
 * Published when a game session ends, either due to win condition being met
 * or manual termination.
 */
public class GameEndedEvent extends DomainEvent {
    private final WinOutcome outcome;
    private final String reason;
    
    public GameEndedEvent(GameSessionId sessionId, WinOutcome outcome, String reason) {
        super(sessionId);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reason cannot be null");
        }
        this.outcome = outcome;
        this.reason = reason;
    }
    
    public WinOutcome getOutcome() {
        return outcome;
    }
    
    public String getReason() {
        return reason;
    }
}
```

**Step 3: Verify compilation**

Run: `mvn compile`

Expected output:
```
[INFO] BUILD SUCCESS
```

**Step 4: Commit game lifecycle events**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/event/GameStartedEvent.java
git add src/main/java/me/flamboyant/manhunt/domain/event/GameEndedEvent.java
git commit -m "feat(domain): add game lifecycle events

- Add GameStartedEvent with player list and speedrunner count
- Add GameEndedEvent with win outcome and reason
- Both events validate null parameters and use defensive copying"
```
