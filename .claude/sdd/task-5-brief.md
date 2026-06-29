# Task 5: Win Condition Events

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/DragonKilledEvent.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/WinConditionMetEvent.java`

**Interfaces:**
- Consumes: `DomainEvent` from Task 1, `WinOutcome` from `me.flamboyant.manhunt.domain.wincondition.WinOutcome`
- Produces:
  - `DragonKilledEvent(sessionId, killer)` where killer may be null
  - `WinConditionMetEvent(sessionId, outcome)`

---

**Step 1: Create DragonKilledEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/DragonKilledEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

/**
 * Published when the Ender Dragon is killed.
 * This is a critical business event that triggers win condition evaluation.
 */
public class DragonKilledEvent extends DomainEvent {
    private final Player killer; // May be null if environmental kill
    
    public DragonKilledEvent(GameSessionId sessionId, Player killer) {
        super(sessionId);
        this.killer = killer; // Nullable
    }
    
    public Player getKiller() {
        return killer;
    }
    
    public boolean hasKiller() {
        return killer != null;
    }
}
```

**Step 2: Create WinConditionMetEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/WinConditionMetEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.wincondition.WinOutcome;

/**
 * Published when any win condition evaluates to true.
 * Indicates that the game should end with the specified outcome.
 */
public class WinConditionMetEvent extends DomainEvent {
    private final WinOutcome outcome;
    
    public WinConditionMetEvent(GameSessionId sessionId, WinOutcome outcome) {
        super(sessionId);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        this.outcome = outcome;
    }
    
    public WinOutcome getOutcome() {
        return outcome;
    }
}
```

**Step 3: Verify compilation**

Run: `mvn compile`

Expected output:
```
[INFO] BUILD SUCCESS
```

**Step 4: Commit win condition events**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/event/DragonKilledEvent.java
git add src/main/java/me/flamboyant/manhunt/domain/event/WinConditionMetEvent.java
git commit -m "feat(domain): add win condition events

- Add DragonKilledEvent with optional killer field
- Add WinConditionMetEvent for win detection
- Both events used for win condition evaluation"
```
