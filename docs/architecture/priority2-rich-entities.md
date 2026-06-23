# Priority 2: Rich Entities - Architecture Documentation

## Overview
Priority 2 transformed anemic domain entities into rich entities that contain behavior, following Domain-Driven Design (DDD) principles. Business logic was moved from managers and event handlers into the domain entities themselves, implementing the "Tell, Don't Ask" pattern.

## Changes Summary

### 1. Value Objects Created

#### DamageOutcome
**Purpose:** Encapsulates the result of taking damage.

**Location:** `src/main/java/me/flamboyant/manhunt/domain/roles/DamageOutcome.java`

**Properties:**
- `died: boolean` - Whether the entity died from the damage
- `remainingHealth: double` - Health remaining after damage

**Factory Method:**
```java
DamageOutcome.of(double currentHealth, double damageAmount)
```

**Benefits:**
- Immutable value object
- Encapsulates damage calculation logic
- Type-safe (no primitive obsession)
- Self-documenting return type

#### CompassTarget
**Purpose:** Represents a compass tracking target with dimension awareness.

**Location:** `src/main/java/me/flamboyant/manhunt/domain/roles/CompassTarget.java`

**Properties:**
- `location: Location` - Target location for compass
- `crossDimension: boolean` - Whether target is in different dimension
- `targetDimensionName: String` - Name of target's dimension (if cross-dimension)

**Factory Methods:**
```java
CompassTarget.sameDimension(Location location)
CompassTarget.crossDimension(Location portalLocation, String targetDimensionName)
```

**Benefits:**
- Encapsulates cross-dimension tracking logic
- Explicit factory methods for different scenarios
- Validates non-null location
- Removes dimension-handling complexity from event handlers

### 2. Rich Entity Behaviors

#### SpeedrunnerRole.handleDamage()
**Before:** NewManhuntManager asked role type, calculated death, implemented logic
**After:** Role handles its own damage and returns outcome

```java
public DamageOutcome handleDamage(double damageAmount) {
    return DamageOutcome.of(owner.getHealth(), damageAmount);
}
```

**Benefits:**
- Role encapsulates death logic
- Manager only orchestrates, doesn't decide
- Follows Single Responsibility Principle
- Testable in isolation

#### AManhuntRole.calculateCompassTarget()
**Before:** Logic scattered in HunterRole and SpeedrunnerRole event handlers
**After:** Shared method in base class, reused by both roles

```java
protected CompassTarget calculateCompassTarget(Player target, GameSession session)
```

**Benefits:**
- DRY (Don't Repeat Yourself) - logic in one place
- Cross-dimension handling encapsulated
- Portal location retrieval abstracted
- Both roles use same logic consistently

### 3. Tell, Don't Ask Refactoring

#### NewManhuntManager.onEntityDamage()

**Before (Asking):**
```java
if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER
        && player.getHealth() - event.getFinalDamage() <= 0) {
    // Manager implements death logic
}
```

**After (Telling):**
```java
if (role instanceof SpeedrunnerRole) {
    DamageOutcome outcome = speedrunnerRole.handleDamage(event.getFinalDamage());
    if (outcome.isDied()) {
        // Manager orchestrates game-level response
    }
}
```

**Benefits:**
- Manager tells role to handle damage
- Role decides what happens
- Business logic in domain entity, not infrastructure
- Easier to add role-specific death behaviors

#### SpeedrunnerRole.doCompassEffect()

**Before:**
```java
Location huntedLocation = target.getLocation();
if (owner.getWorld() != huntedWorld) {
    owner.sendMessage(...);
    if (ownerWorldName.equals("world")) {
        huntedLocation = session.getPortalLocation(target, World.Environment.NORMAL);
    } else if (ownerWorldName.equals("world_nether")) {
        huntedLocation = session.getPortalLocation(target, World.Environment.NETHER);
    }
}
// More dimension logic...
```

**After:**
```java
CompassTarget compassTarget = calculateCompassTarget(target, session);
if (compassTarget.isCrossDimension()) {
    owner.sendMessage(target.getDisplayName() + " est dans la dimension " + compassTarget.getTargetDimensionName());
}
Location huntedLocation = compassTarget.getLocation();
// Simplified compass application
```

**Benefits:**
- Dimension logic encapsulated in value object
- Event handler only applies the result
- Cross-dimension handling is declarative, not imperative
- Easier to understand at a glance

### 4. Test-Driven Development (TDD)

All changes followed strict TDD discipline:

1. **RED:** Write failing test
2. **Verify RED:** Watch it fail for expected reason
3. **GREEN:** Write minimal code to pass
4. **Verify GREEN:** Watch it pass
5. **REFACTOR:** Clean up while keeping tests green

#### SpeedrunnerRoleTest
**Location:** `src/test/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRoleTest.java`

**Test Coverage:**
- Death handling when health drops to zero
- Survival when damage doesn't kill
- Overkill damage (negative remaining health)
- Compass targeting in same dimension
- Compass targeting across dimensions with portal lookup

**Tests:** 5 tests covering death and compass scenarios

## Architecture Benefits

### 1. Rich Entities
- Entities now contain behavior, not just data
- Business logic lives where it belongs (in the domain)
- Roles are "tell-able" objects, not data structures

### 2. Value Objects
- Primitives replaced with domain concepts
- Immutable, self-validating
- Make code self-documenting

### 3. Single Responsibility
- Roles handle role logic
- Managers orchestrate game flow
- Clear separation of concerns

### 4. Testability
- Domain logic testable in isolation
- Mock Bukkit dependencies
- Fast unit tests without Minecraft server

### 5. Maintainability
- Logic in one place (DRY)
- Easy to find where behavior is implemented
- Changes localized to appropriate entity

## Files Changed

### Created
- `src/main/java/me/flamboyant/manhunt/domain/roles/DamageOutcome.java`
- `src/main/java/me/flamboyant/manhunt/domain/roles/CompassTarget.java`
- `src/test/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRoleTest.java`

### Modified
- `src/main/java/me/flamboyant/manhunt/roles/AManhuntRole.java`
  - Added calculateCompassTarget() method
  - Added imports for value objects
- `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java`
  - Added handleDamage() method
  - Refactored doCompassEffect() to use calculateCompassTarget()
  - Added DamageOutcome import
- `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java`
  - Refactored onPlayerInteract() to use calculateCompassTarget()
  - Added CompassTarget import
- `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`
  - Refactored onEntityDamage() to use Tell, Don't Ask
  - Calls role.handleDamage() instead of checking role type
  - Added DamageOutcome import

## Next Steps (Priority 3)
- Define Bounded Contexts
- Establish context boundaries
- Implement anti-corruption layers between contexts
- Define ubiquitous language per context

## References
- [Priority 1: GameSession Aggregate Root](gamesession-aggregate.md)
- [Refactoring Progress Tracker](../REFACTORING_PROGRESS.md)
- [Implementation Plan](../superpowers/plans/2026-06-18-gamesession-aggregate-root.md)
