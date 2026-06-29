# Task 4: Role and Player Events

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/RoleAssignedEvent.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/RolesRevealedEvent.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/event/SpeedrunnerDiedEvent.java`

**Interfaces:**
- Consumes: `DomainEvent` from Task 1, `ManhuntRoleIdentifier` from `me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier`
- Produces:
  - `RoleAssignedEvent(sessionId, player, roleIdentifier)`
  - `RolesRevealedEvent(sessionId)`
  - `SpeedrunnerDiedEvent(sessionId, player, remainingSpeedrunners)`

---

**Step 1: Create RoleAssignedEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/RoleAssignedEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

/**
 * Published when a role is assigned to a player during game setup.
 */
public class RoleAssignedEvent extends DomainEvent {
    private final Player player;
    private final ManhuntRoleIdentifier roleIdentifier;
    
    public RoleAssignedEvent(GameSessionId sessionId, Player player, ManhuntRoleIdentifier roleIdentifier) {
        super(sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (roleIdentifier == null) {
            throw new IllegalArgumentException("Role identifier cannot be null");
        }
        this.player = player;
        this.roleIdentifier = roleIdentifier;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return roleIdentifier;
    }
}
```

**Step 2: Create RolesRevealedEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/RolesRevealedEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;

/**
 * Published when surprise mode ends and roles are revealed to players.
 */
public class RolesRevealedEvent extends DomainEvent {
    public RolesRevealedEvent(GameSessionId sessionId) {
        super(sessionId);
    }
}
```

**Step 3: Create SpeedrunnerDiedEvent**

Create `src/main/java/me/flamboyant/manhunt/domain/event/SpeedrunnerDiedEvent.java`:

```java
package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

/**
 * Published when a speedrunner takes fatal damage and dies.
 * This is a critical business event that triggers win condition evaluation.
 */
public class SpeedrunnerDiedEvent extends DomainEvent {
    private final Player player;
    private final int remainingSpeedrunners;
    
    public SpeedrunnerDiedEvent(GameSessionId sessionId, Player player, int remainingSpeedrunners) {
        super(sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
        this.remainingSpeedrunners = remainingSpeedrunners;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getRemainingSpeedrunners() {
        return remainingSpeedrunners;
    }
}
```

**Step 4: Verify compilation**

Run: `mvn compile`

Expected output:
```
[INFO] BUILD SUCCESS
```

**Step 5: Commit role and player events**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/event/RoleAssignedEvent.java
git add src/main/java/me/flamboyant/manhunt/domain/event/RolesRevealedEvent.java
git add src/main/java/me/flamboyant/manhunt/domain/event/SpeedrunnerDiedEvent.java
git commit -m "feat(domain): add role and player events

- Add RoleAssignedEvent for role distribution tracking
- Add RolesRevealedEvent for surprise mode end
- Add SpeedrunnerDiedEvent for win condition evaluation
- All events validate required parameters"
```
