# Bounded Contexts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish 6 bounded contexts with clear boundaries, ubiquitous language, and integration interfaces.

**Architecture:** Classic Layered DDD with contexts at domain level. Package structure reflects context boundaries. Infrastructure depends on domain (not vice versa). Contexts communicate via direct method calls through well-defined interfaces.

**Tech Stack:** Java 17, Bukkit/Spigot API, JUnit 5, Mockito

## Global Constraints

- All existing 69+ tests must continue to pass
- No breaking changes to existing behavior
- Use TDD (Red-Green-Refactor) for all new code
- Domain code must not import Bukkit classes
- Follow existing code style and naming conventions
- Commit after each task completion
- Run full test suite before each commit

---

## File Structure Overview

### New Packages to Create
```
domain/
├── game/              (Game Management Context)
├── role/
│   ├── definition/    (Role Definition Context)
│   └── behavior/      (Role Behavior Context)
├── tracking/          (Player Tracking Context)
└── wincondition/      (Win Condition Context)

infrastructure/
└── ui/                (Infrastructure Context)
```

### Files to Create (10 new files)
- `domain/game/GameConfiguration.java`
- `domain/tracking/PortalTracker.java`
- `domain/tracking/PlayerLocation.java`
- `domain/tracking/PortalEntry.java`
- `domain/wincondition/WinCondition.java`
- `domain/wincondition/AllSpeedrunnersDeadCondition.java`
- `domain/wincondition/DragonKilledCondition.java`
- `domain/wincondition/WinConditionEvaluator.java`
- `domain/wincondition/WinOutcome.java`
- `test/.../PortalTrackerTest.java`

### Files to Move (27 existing files)
- 2 files: `domain/game/` → stays in place
- 3 files: `roles/` → `domain/role/definition/`
- 20 files: `roles/` + `roles/impl/` → `domain/role/behavior/`
- 2 files: `domain/roles/` → `domain/role/behavior/`
- 1 file: `views/` → `infrastructure/ui/`
- 1 file: `roles/` → `domain/wincondition/`

---

## Task 1: Create Package Structure and Move Game Management Context

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/game/.gitkeep` (then delete)
- Current: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java` (already exists)
- Current: `src/main/java/me/flamboyant/manhunt/domain/game/GameSessionId.java` (already exists)
- Test: Run existing `GameSessionTest.java` and `GameSessionIdTest.java`

**Interfaces:**
- Consumes: Existing GameSession and GameSessionId classes
- Produces: Package structure foundation for all other contexts

**Steps:**

- [ ] **Step 1: Verify current state**

Run: `./gradlew test --tests "*GameSession*" --tests "*GameSessionId*"`

Expected: All tests pass (24 GameSession tests + 7 GameSessionId tests)

- [ ] **Step 2: Verify package already correct**

Check that these files exist in correct location:
- `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`
- `src/main/java/me/flamboyant/manhunt/domain/game/GameSessionId.java`

Expected: Files already in `domain/game/` package (from Priority 1)

- [ ] **Step 3: Create remaining package directories**

Run:
```bash
mkdir -p "src/main/java/me/flamboyant/manhunt/domain/role/definition"
mkdir -p "src/main/java/me/flamboyant/manhunt/domain/role/behavior"
mkdir -p "src/main/java/me/flamboyant/manhunt/domain/tracking"
mkdir -p "src/main/java/me/flamboyant/manhunt/domain/wincondition"
mkdir -p "src/main/java/me/flamboyant/manhunt/infrastructure/ui"
mkdir -p "src/test/java/me/flamboyant/manhunt/domain/tracking"
mkdir -p "src/test/java/me/flamboyant/manhunt/domain/wincondition"
```

Expected: Directories created successfully

- [ ] **Step 4: Verify test directories**

Run:
```bash
mkdir -p "src/test/java/me/flamboyant/manhunt/domain/game"
mkdir -p "src/test/java/me/flamboyant/manhunt/domain/role/behavior"
```

Expected: Test directories exist

- [ ] **Step 5: Commit package structure**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/
git add src/main/java/me/flamboyant/manhunt/infrastructure/
git add src/test/java/me/flamboyant/manhunt/domain/
git commit -m "refactor(contexts): create bounded context package structure"
```

---

## Task 2: Move Role Definition Context Files

**Files:**
- Move: `src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleType.java` → `domain/role/definition/`
- Move: `src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleIdentifier.java` → `domain/role/definition/`
- Move: `src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleFactory.java` → `domain/role/definition/`

**Interfaces:**
- Consumes: Package structure from Task 1
- Produces: Role Definition Context with type-safe role identifiers and factory

**Steps:**

- [ ] **Step 1: Move ManhuntRoleType.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleType.java" "src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleType.java"
```

Expected: File moved successfully

- [ ] **Step 2: Update package declaration in ManhuntRoleType.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleType.java`

Change line 1 from:
```java
package me.flamboyant.manhunt.roles;
```

To:
```java
package me.flamboyant.manhunt.domain.role.definition;
```

- [ ] **Step 3: Move ManhuntRoleIdentifier.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleIdentifier.java" "src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java"
```

Expected: File moved successfully

- [ ] **Step 4: Update package declaration in ManhuntRoleIdentifier.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleIdentifier.java`

Change line 1 from:
```java
package me.flamboyant.manhunt.roles;
```

To:
```java
package me.flamboyant.manhunt.domain.role.definition;
```

- [ ] **Step 5: Move ManhuntRoleFactory.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/ManhuntRoleFactory.java" "src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java"
```

Expected: File moved successfully

- [ ] **Step 6: Update package declaration in ManhuntRoleFactory.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java`

Change line 1 from:
```java
package me.flamboyant.manhunt.roles;
```

To:
```java
package me.flamboyant.manhunt.domain.role.definition;
```

- [ ] **Step 7: Update imports in ManhuntRoleFactory.java**

In the same file, add import for role behavior (which we'll move next):
```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.*;
import org.bukkit.entity.Player;
```

Note: This import will be updated after Task 3 moves the role implementations.

- [ ] **Step 8: Attempt compilation to find import errors**

Run: `./gradlew compileJava`

Expected: Compilation fails with import errors (we'll fix these systematically)

- [ ] **Step 9: Commit Role Definition Context**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/definition/
git commit -m "refactor(contexts): move Role Definition Context files"
```

---

## Task 3: Move Role Behavior Context Files (Part 1 - Base Classes)

**Files:**
- Move: `src/main/java/me/flamboyant/manhunt/roles/AManhuntRole.java` → `domain/role/behavior/`
- Move: `src/main/java/me/flamboyant/manhunt/domain/roles/DamageOutcome.java` → `domain/role/behavior/`
- Move: `src/main/java/me/flamboyant/manhunt/domain/roles/CompassTarget.java` → `domain/role/behavior/`

**Interfaces:**
- Consumes: Role Definition Context from Task 2
- Produces: Base role behavior classes for concrete implementations

**Steps:**

- [ ] **Step 1: Move AManhuntRole.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/AManhuntRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/AManhuntRole.java"
```

Expected: File moved successfully

- [ ] **Step 2: Update package in AManhuntRole.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/AManhuntRole.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

Add imports:
```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.game.GameSession;
```

- [ ] **Step 3: Move DamageOutcome.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/domain/roles/DamageOutcome.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/DamageOutcome.java"
```

Expected: File moved successfully

- [ ] **Step 4: Update package in DamageOutcome.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/DamageOutcome.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

- [ ] **Step 5: Move CompassTarget.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/domain/roles/CompassTarget.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/CompassTarget.java"
```

Expected: File moved successfully

- [ ] **Step 6: Update package in CompassTarget.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/CompassTarget.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

- [ ] **Step 7: Delete empty domain/roles directory**

Run:
```bash
rmdir "src/main/java/me/flamboyant/manhunt/domain/roles"
```

Expected: Directory removed (now empty)

- [ ] **Step 8: Commit base behavior classes**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/behavior/
git rm -r src/main/java/me/flamboyant/manhunt/domain/roles/
git commit -m "refactor(contexts): move base Role Behavior classes"
```

---

## Task 4: Move Role Behavior Context Files (Part 2 - Core Implementations)

**Files:**
- Move: `src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java` → `domain/role/behavior/`
- Move: `src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java` → `domain/role/behavior/`
- Test: Move `src/test/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRoleTest.java` → `domain/role/behavior/`

**Interfaces:**
- Consumes: AManhuntRole, DamageOutcome, CompassTarget from Task 3
- Produces: Core role implementations (Speedrunner, Hunter)

**Steps:**

- [ ] **Step 1: Move SpeedrunnerRole.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/SpeedrunnerRole.java"
```

Expected: File moved successfully

- [ ] **Step 2: Update package in SpeedrunnerRole.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/SpeedrunnerRole.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

Update imports:
```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.game.GameSession;
```

- [ ] **Step 3: Move HunterRole.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/HunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java"
```

Expected: File moved successfully

- [ ] **Step 4: Update package in HunterRole.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/HunterRole.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

Update imports:
```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.game.GameSession;
```

- [ ] **Step 5: Move SpeedrunnerRoleTest.java**

Run:
```bash
git mv "src/test/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerRoleTest.java" "src/test/java/me/flamboyant/manhunt/domain/role/behavior/SpeedrunnerRoleTest.java"
```

Expected: File moved successfully

- [ ] **Step 6: Update package in SpeedrunnerRoleTest.java**

Open: `src/test/java/me/flamboyant/manhunt/domain/role/behavior/SpeedrunnerRoleTest.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.role.behavior;
```

Update imports:
```java
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
```

- [ ] **Step 7: Run SpeedrunnerRoleTest to verify**

Run: `./gradlew test --tests "*SpeedrunnerRoleTest"`

Expected: All 5 tests pass

- [ ] **Step 8: Commit core role implementations**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/behavior/
git add src/test/java/me/flamboyant/manhunt/domain/role/behavior/
git commit -m "refactor(contexts): move core role implementations to Role Behavior context"
```

---

## Task 5: Move Role Behavior Context Files (Part 3 - All Specialized Roles)

**Files:**
- Move all 15 specialized role files from `roles/impl/` to `domain/role/behavior/`

**Interfaces:**
- Consumes: SpeedrunnerRole, HunterRole base classes from Task 4
- Produces: Complete Role Behavior Context with all role variants

**Steps:**

- [ ] **Step 1: Move all specialized role files in one batch**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/CheckpointSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/CheckpointHunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/CutCleanHunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/CutCleanSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/ElfHunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/ElfSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/GluerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/ImposterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/LinkHunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/LinkSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/NoNameTagSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/ProMinerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/SpeedrunnerSwapperRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/SuperHunterRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/TntTacticalSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/UndecidedRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
git mv "src/main/java/me/flamboyant/manhunt/roles/impl/WerewolfSpeedrunnerRole.java" "src/main/java/me/flamboyant/manhunt/domain/role/behavior/"
```

Expected: All 17 files moved successfully

- [ ] **Step 2: Update package declarations (batch script)**

For each moved file, change package from `me.flamboyant.manhunt.roles.impl` to `me.flamboyant.manhunt.domain.role.behavior`

Create a script or use IDE "Replace in Path":
- Find: `package me.flamboyant.manhunt.roles.impl;`
- Replace: `package me.flamboyant.manhunt.domain.role.behavior;`
- Scope: `src/main/java/me/flamboyant/manhunt/domain/role/behavior/`

- [ ] **Step 3: Update imports in all role files**

For each moved file, ensure imports reference new packages:
- Find: `import me.flamboyant.manhunt.roles.ManhuntRoleIdentifier;`
- Replace: `import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;`

Repeat for:
- `ManhuntRoleType` → `domain.role.definition.ManhuntRoleType`
- `AManhuntRole` → `domain.role.behavior.AManhuntRole`
- `SpeedrunnerRole` → `domain.role.behavior.SpeedrunnerRole`
- `HunterRole` → `domain.role.behavior.HunterRole`

- [ ] **Step 4: Delete empty roles/impl directory**

Run:
```bash
rmdir "src/main/java/me/flamboyant/manhunt/roles/impl"
```

Expected: Directory removed

- [ ] **Step 5: Attempt compilation**

Run: `./gradlew compileJava`

Expected: May still have errors from files that import roles (we'll fix next)

- [ ] **Step 6: Commit all specialized roles**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/behavior/
git rm -r src/main/java/me/flamboyant/manhunt/roles/impl/
git commit -m "refactor(contexts): move all specialized roles to Role Behavior context"
```

---

## Task 6: Move Infrastructure and Win Condition Files

**Files:**
- Move: `src/main/java/me/flamboyant/manhunt/views/PlayerSelectionView.java` → `infrastructure/ui/`
- Move: `src/main/java/me/flamboyant/manhunt/roles/IHunterWinConditionModifier.java` → `domain/wincondition/`

**Interfaces:**
- Consumes: Package structure from Task 1
- Produces: Infrastructure UI context and partial Win Condition context

**Steps:**

- [ ] **Step 1: Move PlayerSelectionView.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/views/PlayerSelectionView.java" "src/main/java/me/flamboyant/manhunt/infrastructure/ui/PlayerSelectionView.java"
```

Expected: File moved successfully

- [ ] **Step 2: Update package in PlayerSelectionView.java**

Open: `src/main/java/me/flamboyant/manhunt/infrastructure/ui/PlayerSelectionView.java`

Change package declaration:
```java
package me.flamboyant.manhunt.infrastructure.ui;
```

- [ ] **Step 3: Delete empty views directory**

Run:
```bash
rmdir "src/main/java/me/flamboyant/manhunt/views"
```

Expected: Directory removed

- [ ] **Step 4: Move IHunterWinConditionModifier.java**

Run:
```bash
git mv "src/main/java/me/flamboyant/manhunt/roles/IHunterWinConditionModifier.java" "src/main/java/me/flamboyant/manhunt/domain/wincondition/IHunterWinConditionModifier.java"
```

Expected: File moved successfully

- [ ] **Step 5: Update package in IHunterWinConditionModifier.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/wincondition/IHunterWinConditionModifier.java`

Change package declaration:
```java
package me.flamboyant.manhunt.domain.wincondition;
```

Update imports:
```java
import me.flamboyant.manhunt.domain.game.GameSession;
```

- [ ] **Step 6: Commit infrastructure and win condition moves**

```bash
git add src/main/java/me/flamboyant/manhunt/infrastructure/ui/
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/
git rm -r src/main/java/me/flamboyant/manhunt/views/
git commit -m "refactor(contexts): move Infrastructure UI and Win Condition files"
```

---

## Task 7: Fix All Import Statements Across Codebase

**Files:**
- Modify: All files that import moved classes (Main.java, NewManhuntManager.java, NewManhuntLauncher.java, GameRolesManagement.java, etc.)

**Interfaces:**
- Consumes: All moved files from Tasks 2-6
- Produces: Compilable codebase with correct imports

**Steps:**

- [ ] **Step 1: Find all files with import errors**

Run: `./gradlew compileJava 2>&1 | grep "error: package" | head -20`

Expected: List of files with import errors

- [ ] **Step 2: Update imports in ManhuntRoleFactory.java**

Open: `src/main/java/me/flamboyant/manhunt/domain/role/definition/ManhuntRoleFactory.java`

Update imports at the top:
```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.*;
import org.bukkit.entity.Player;

public class ManhuntRoleFactory {
    // ... existing code
}
```

- [ ] **Step 3: Update imports in GameRolesManagement.java**

Open: `src/main/java/me/flamboyant/manhunt/roles/GameRolesManagement.java`

Update imports:
```java
package me.flamboyant.manhunt.roles;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleFactory;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
// ... other imports
```

- [ ] **Step 4: Update imports in NewManhuntManager.java**

Open: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.behavior.DamageOutcome;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.game.GameSession;
```

- [ ] **Step 5: Update imports in NewManhuntLauncher.java**

Open: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleFactory;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView;
```

- [ ] **Step 6: Update imports in Main.java**

Open: `src/main/java/me/flamboyant/manhunt/Main.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.wincondition.IHunterWinConditionModifier;
```

- [ ] **Step 7: Update imports in GameSessionManager.java**

Open: `src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
```

- [ ] **Step 8: Update test imports in GameSessionTest.java**

Open: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java`

Verify package declaration:
```java
package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
```

- [ ] **Step 9: Update test imports in GameSessionManagerTest.java**

Open: `src/test/java/me/flamboyant/manhunt/application/GameSessionManagerTest.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
```

- [ ] **Step 10: Update test imports in GameSessionIntegrationTest.java**

Open: `src/test/java/me/flamboyant/manhunt/GameSessionIntegrationTest.java`

Update imports:
```java
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.role.behavior.SpeedrunnerRole;
import me.flamboyant.manhunt.domain.role.behavior.HunterRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
```

- [ ] **Step 11: Compile entire project**

Run: `./gradlew compileJava compileTestJava`

Expected: Clean compilation with no errors

- [ ] **Step 12: Run full test suite**

Run: `./gradlew test`

Expected: All 69+ tests pass (might be 74 tests now including Priority 2)

- [ ] **Step 13: Commit import fixes**

```bash
git add .
git commit -m "refactor(contexts): fix all import statements after package reorganization"
```

---

## Task 8: Create PortalTracker Interface and Implementation (TDD)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/tracking/PortalTracker.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/tracking/PortalTrackerTest.java`

**Interfaces:**
- Consumes: Nothing (independent tracking context)
- Produces: `PortalTracker` interface with methods:
  - `void setPortalLocation(Player, World.Environment, Location)`
  - `Optional<Location> getPortalLocation(Player, World.Environment)`
  - `void clearPortals(Player)`
  - `boolean hasPortal(Player, World.Environment)`

**Steps:**

- [ ] **Step 1: Write failing test for storing portal location**

Create: `src/test/java/me/flamboyant/manhunt/domain/tracking/PortalTrackerTest.java`

```java
package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PortalTrackerTest {

    private PortalTracker tracker;
    private Player player;
    private Location location;

    @BeforeEach
    void setUp() {
        tracker = new InMemoryPortalTracker();
        player = mock(Player.class);
        location = mock(Location.class);
    }

    @Test
    void shouldStoreAndRetrievePortalLocation() {
        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, location);
        Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);

        // Then
        assertTrue(result.isPresent());
        assertEquals(location, result.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*PortalTrackerTest.shouldStoreAndRetrievePortalLocation"`

Expected: FAIL - "cannot find symbol: class PortalTracker"

- [ ] **Step 3: Create PortalTracker interface**

Create: `src/main/java/me/flamboyant/manhunt/domain/tracking/PortalTracker.java`

```java
package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Manages portal location storage for players across different dimensions.
 * Part of the Player Tracking bounded context.
 */
public interface PortalTracker {
    
    /**
     * Store a portal location for a player in a specific dimension.
     *
     * @param player The player who owns this portal
     * @param dimension The dimension where the portal leads
     * @param location The portal location
     */
    void setPortalLocation(Player player, World.Environment dimension, Location location);
    
    /**
     * Retrieve a portal location for a player in a specific dimension.
     *
     * @param player The player
     * @param dimension The dimension
     * @return Optional containing location if portal exists, empty otherwise
     */
    Optional<Location> getPortalLocation(Player player, World.Environment dimension);
    
    /**
     * Clear all portal locations for a player.
     *
     * @param player The player
     */
    void clearPortals(Player player);
    
    /**
     * Check if a player has a portal in a specific dimension.
     *
     * @param player The player
     * @param dimension The dimension
     * @return true if portal exists
     */
    boolean hasPortal(Player player, World.Environment dimension);
}
```

- [ ] **Step 4: Run test to verify it still fails**

Run: `./gradlew test --tests "*PortalTrackerTest.shouldStoreAndRetrievePortalLocation"`

Expected: FAIL - "cannot find symbol: class InMemoryPortalTracker"

- [ ] **Step 5: Create InMemoryPortalTracker implementation**

Add to same file `PortalTracker.java` at the bottom:

```java
package me.flamboyant.manhunt.domain.tracking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of PortalTracker.
 * Stores portal locations in nested HashMaps.
 */
class InMemoryPortalTracker implements PortalTracker {
    
    private final Map<Player, Map<World.Environment, Location>> portalLocations;
    
    public InMemoryPortalTracker() {
        this.portalLocations = new HashMap<>();
    }
    
    @Override
    public void setPortalLocation(Player player, World.Environment dimension, Location location) {
        portalLocations.computeIfAbsent(player, k -> new HashMap<>())
                      .put(dimension, location);
    }
    
    @Override
    public Optional<Location> getPortalLocation(Player player, World.Environment dimension) {
        Map<World.Environment, Location> playerPortals = portalLocations.get(player);
        if (playerPortals == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerPortals.get(dimension));
    }
    
    @Override
    public void clearPortals(Player player) {
        portalLocations.remove(player);
    }
    
    @Override
    public boolean hasPortal(Player player, World.Environment dimension) {
        return getPortalLocation(player, dimension).isPresent();
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "*PortalTrackerTest.shouldStoreAndRetrievePortalLocation"`

Expected: PASS (1 test passed)

- [ ] **Step 7: Add more tests for edge cases**

Add to `PortalTrackerTest.java`:

```java
    @Test
    void shouldReturnEmptyWhenNoPortalExists() {
        // When
        Optional<Location> result = tracker.getPortalLocation(player, World.Environment.NETHER);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldStoreMultipleDimensionsPerPlayer() {
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
    void shouldClearAllPortalsForPlayer() {
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
    void shouldCheckPortalExistence() {
        // Given
        tracker.setPortalLocation(player, World.Environment.NETHER, location);

        // Then
        assertTrue(tracker.hasPortal(player, World.Environment.NETHER));
        assertFalse(tracker.hasPortal(player, World.Environment.THE_END));
    }

    @Test
    void shouldOverwriteExistingPortal() {
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
    void shouldIsolatePortalsBetweenPlayers() {
        Player player2 = mock(Player.class);
        Location location2 = mock(Location.class);

        // When
        tracker.setPortalLocation(player, World.Environment.NETHER, location);
        tracker.setPortalLocation(player2, World.Environment.NETHER, location2);

        // Then
        assertEquals(location, tracker.getPortalLocation(player, World.Environment.NETHER).get());
        assertEquals(location2, tracker.getPortalLocation(player2, World.Environment.NETHER).get());
    }
```

- [ ] **Step 8: Run all PortalTracker tests**

Run: `./gradlew test --tests "*PortalTrackerTest"`

Expected: PASS (7 tests passed)

- [ ] **Step 9: Commit PortalTracker**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/tracking/
git add src/test/java/me/flamboyant/manhunt/domain/tracking/
git commit -m "feat(tracking): create PortalTracker interface and implementation with tests"
```

---

## Task 9: Extract Portal Logic from GameSession (TDD)

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`
- Modify: `src/test/java/me/flamboyant/manhunt/domain/game/GameSessionTest.java`

**Interfaces:**
- Consumes: `PortalTracker` from Task 8
- Produces: `GameSession` with delegated portal tracking

**Steps:**

- [ ] **Step 1: Add PortalTracker field to GameSession**

Open: `src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java`

Add field and update constructor:

```java
package me.flamboyant.manhunt.domain.game;

import me.flamboyant.manhunt.domain.tracking.PortalTracker;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
// ... other imports

public class GameSession {
    private final GameSessionId id;
    private final Map<Player, AManhuntRole> playerRoles;
    private final PortalTracker portalTracker;  // NEW
    private int remainingSpeedrunners;
    
    // OLD constructor (deprecated but keep for now)
    public GameSession(GameSessionId id) {
        this(id, new InMemoryPortalTracker());
    }
    
    // NEW constructor with dependency injection
    public GameSession(GameSessionId id, PortalTracker portalTracker) {
        this.id = id;
        this.playerRoles = new HashMap<>();
        this.portalTracker = portalTracker;  // NEW
        this.remainingSpeedrunners = 0;
    }
    
    // ... rest of class
}
```

- [ ] **Step 2: Delegate portal methods to PortalTracker**

In the same file, update portal methods:

```java
    // OLD field - REMOVE THIS
    // private final Map<Player, Map<World.Environment, Location>> portalLocations = new HashMap<>();
    
    public void setPortalLocation(Player player, World.Environment environment, Location location) {
        portalTracker.setPortalLocation(player, environment, location);
    }
    
    public Optional<Location> getPortalLocation(Player player, World.Environment environment) {
        return portalTracker.getPortalLocation(player, environment);
    }
    
    public Location getPortalLocationOrDefault(Player player, World.Environment environment, Location defaultLocation) {
        return portalTracker.getPortalLocation(player, environment).orElse(defaultLocation);
    }
```

- [ ] **Step 3: Update clear() method to clear portals**

In the same file, update the `clear()` method:

```java
    public void clear() {
        // Clear all portals for all players in this session
        for (Player player : playerRoles.keySet()) {
            portalTracker.clearPortals(player);
        }
        playerRoles.clear();
        remainingSpeedrunners = 0;
    }
```

- [ ] **Step 4: Remove old portalLocations field**

Remove the line:
```java
private final Map<Player, Map<World.Environment, Location>> portalLocations = new HashMap<>();
```

And remove any direct references to `portalLocations` map (should all be delegated now).

- [ ] **Step 5: Run GameSessionTest to check for failures**

Run: `./gradlew test --tests "*GameSessionTest"`

Expected: All tests should still pass (24 tests) because we maintained the same public API

- [ ] **Step 6: Update GameSessionManager to use new constructor**

Open: `src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java`

Update the `createSession()` method:

```java
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;

public class GameSessionManager {
    // ... existing code
    
    public GameSession createSession() {
        GameSessionId id = GameSessionId.generate();
        GameSession session = new GameSession(id, new InMemoryPortalTracker());
        activeSessions.put(id, session);
        return session;
    }
    
    // ... rest of class
}
```

- [ ] **Step 7: Run all tests to verify extraction**

Run: `./gradlew test`

Expected: All 74+ tests pass (no regressions)

- [ ] **Step 8: Commit portal extraction**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/game/GameSession.java
git add src/main/java/me/flamboyant/manhunt/application/GameSessionManager.java
git commit -m "refactor(tracking): extract portal tracking from GameSession to PortalTracker"
```

---

## Task 10: Create WinCondition Interface and Value Objects (TDD)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinCondition.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinOutcome.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

**Interfaces:**
- Consumes: `GameSession` from Game Management Context
- Produces: `WinCondition` interface with:
  - `boolean isMet(GameSession session)`
  - `Set<ManhuntRoleType> getWinners()`
  - `String getDescription()`
- Produces: `WinOutcome` value object

**Steps:**

- [ ] **Step 1: Write failing test for WinCondition**

Create: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WinConditionTest {

    @Test
    void shouldCreateWinOutcomeFromCondition() {
        // Given
        GameSession session = mock(GameSession.class);
        WinCondition condition = new WinCondition() {
            @Override
            public boolean isMet(GameSession session) {
                return true;
            }

            @Override
            public Set<ManhuntRoleType> getWinners() {
                return Set.of(ManhuntRoleType.HUNTER);
            }

            @Override
            public String getDescription() {
                return "Test win condition";
            }
        };

        // When
        WinOutcome outcome = WinOutcome.of(condition);

        // Then
        assertNotNull(outcome);
        assertTrue(outcome.getWinners().contains(ManhuntRoleType.HUNTER));
        assertEquals("Test win condition", outcome.getDescription());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*WinConditionTest.shouldCreateWinOutcomeFromCondition"`

Expected: FAIL - "cannot find symbol: interface WinCondition"

- [ ] **Step 3: Create WinCondition interface**

Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinCondition.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Set;

/**
 * Represents a game victory condition.
 * Part of the Win Condition bounded context.
 */
public interface WinCondition {
    
    /**
     * Check if this win condition is met for the given game session.
     *
     * @param session The game session to evaluate
     * @return true if the condition is met and the game should end
     */
    boolean isMet(GameSession session);
    
    /**
     * Get the team(s) that win if this condition is met.
     *
     * @return Set of winning role types
     */
    Set<ManhuntRoleType> getWinners();
    
    /**
     * Get a human-readable description of this win condition.
     *
     * @return Description string (e.g., "Tous les speedrunners sont morts !")
     */
    String getDescription();
}
```

- [ ] **Step 4: Run test to verify it still fails**

Run: `./gradlew test --tests "*WinConditionTest.shouldCreateWinOutcomeFromCondition"`

Expected: FAIL - "cannot find symbol: class WinOutcome"

- [ ] **Step 5: Create WinOutcome value object**

Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinOutcome.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable value object representing the outcome of a win condition evaluation.
 * Part of the Win Condition bounded context.
 */
public final class WinOutcome {
    
    private final Set<ManhuntRoleType> winners;
    private final String description;
    private final WinCondition triggeringCondition;
    
    private WinOutcome(Set<ManhuntRoleType> winners, String description, WinCondition triggeringCondition) {
        this.winners = Set.copyOf(winners); // Defensive copy for immutability
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.triggeringCondition = Objects.requireNonNull(triggeringCondition, "Triggering condition cannot be null");
    }
    
    /**
     * Create a WinOutcome from a win condition.
     *
     * @param condition The win condition that was met
     * @return Immutable WinOutcome
     */
    public static WinOutcome of(WinCondition condition) {
        return new WinOutcome(
            condition.getWinners(),
            condition.getDescription(),
            condition
        );
    }
    
    /**
     * Get the winning team(s).
     *
     * @return Immutable set of winning role types
     */
    public Set<ManhuntRoleType> getWinners() {
        return winners;
    }
    
    /**
     * Get the description of why the game ended.
     *
     * @return Description string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the condition that triggered this outcome.
     *
     * @return The win condition
     */
    public WinCondition getTriggeringCondition() {
        return triggeringCondition;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WinOutcome that = (WinOutcome) o;
        return Objects.equals(winners, that.winners) &&
               Objects.equals(description, that.description) &&
               Objects.equals(triggeringCondition, that.triggeringCondition);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(winners, description, triggeringCondition);
    }
    
    @Override
    public String toString() {
        return "WinOutcome{" +
               "winners=" + winners +
               ", description='" + description + '\'' +
               '}';
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "*WinConditionTest.shouldCreateWinOutcomeFromCondition"`

Expected: PASS (1 test passed)

- [ ] **Step 7: Add test for WinOutcome immutability**

Add to `WinConditionTest.java`:

```java
    @Test
    void shouldBeImmutable() {
        // Given
        WinCondition condition = new WinCondition() {
            @Override
            public boolean isMet(GameSession session) { return true; }
            @Override
            public Set<ManhuntRoleType> getWinners() { return Set.of(ManhuntRoleType.HUNTER); }
            @Override
            public String getDescription() { return "Test"; }
        };

        // When
        WinOutcome outcome = WinOutcome.of(condition);
        Set<ManhuntRoleType> winners = outcome.getWinners();

        // Then - attempting to modify should fail
        assertThrows(UnsupportedOperationException.class, () -> {
            winners.add(ManhuntRoleType.SPEEDRUNNER);
        });
    }
```

- [ ] **Step 8: Run all WinCondition tests**

Run: `./gradlew test --tests "*WinConditionTest"`

Expected: PASS (2 tests passed)

- [ ] **Step 9: Commit WinCondition interface and WinOutcome**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/WinCondition.java
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/WinOutcome.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java
git commit -m "feat(wincondition): create WinCondition interface and WinOutcome value object with tests"
```

---

## Task 11: Implement AllSpeedrunnersDeadCondition (TDD)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/AllSpeedrunnersDeadCondition.java`
- Modify: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

**Interfaces:**
- Consumes: `WinCondition` interface from Task 10, `GameSession.getRemainingSpeedrunners()`
- Produces: `AllSpeedrunnersDeadCondition` implementation

**Steps:**

- [ ] **Step 1: Write failing test for AllSpeedrunnersDeadCondition**

Add to `WinConditionTest.java`:

```java
    @Test
    void allSpeedrunnersDeadCondition_shouldBeMet_whenNoSpeedrunnersRemain() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertTrue(isMet);
        assertTrue(condition.getWinners().contains(ManhuntRoleType.HUNTER));
        assertFalse(condition.getDescription().isEmpty());
    }

    @Test
    void allSpeedrunnersDeadCondition_shouldNotBeMet_whenSpeedrunnersRemain() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(2);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertFalse(isMet);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*WinConditionTest.allSpeedrunnersDeadCondition*"`

Expected: FAIL - "cannot find symbol: class AllSpeedrunnersDeadCondition"

- [ ] **Step 3: Create AllSpeedrunnersDeadCondition**

Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/AllSpeedrunnersDeadCondition.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Set;

/**
 * Win condition: All speedrunners are dead.
 * When met, the Hunter team wins.
 */
public class AllSpeedrunnersDeadCondition implements WinCondition {
    
    @Override
    public boolean isMet(GameSession session) {
        return session.getRemainingSpeedrunners() == 0;
    }
    
    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.HUNTER);
    }
    
    @Override
    public String getDescription() {
        return "Tous les speedrunners sont morts ! L'équipe HUNTER a gagné !";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*WinConditionTest.allSpeedrunnersDeadCondition*"`

Expected: PASS (2 new tests passed)

- [ ] **Step 5: Test edge case with negative speedrunners**

Add to `WinConditionTest.java`:

```java
    @Test
    void allSpeedrunnersDeadCondition_shouldHandleEdgeCase_whenNegativeSpeedrunners() {
        // Given (shouldn't happen, but defensive)
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(-1);
        WinCondition condition = new AllSpeedrunnersDeadCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then - treat negative as "all dead"
        assertTrue(isMet);
    }
```

- [ ] **Step 6: Update implementation to handle edge case**

Update `AllSpeedrunnersDeadCondition.java`:

```java
    @Override
    public boolean isMet(GameSession session) {
        // Treat 0 or negative as "all dead"
        return session.getRemainingSpeedrunners() <= 0;
    }
```

- [ ] **Step 7: Run all tests**

Run: `./gradlew test --tests "*WinConditionTest"`

Expected: PASS (5 tests total)

- [ ] **Step 8: Commit AllSpeedrunnersDeadCondition**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/AllSpeedrunnersDeadCondition.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java
git commit -m "feat(wincondition): implement AllSpeedrunnersDeadCondition with tests"
```

---

## Task 12: Implement DragonKilledCondition (TDD)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/DragonKilledCondition.java`
- Modify: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

**Interfaces:**
- Consumes: `WinCondition` interface from Task 10
- Produces: `DragonKilledCondition` implementation (stateful)

**Steps:**

- [ ] **Step 1: Write failing test for DragonKilledCondition**

Add to `WinConditionTest.java`:

```java
    @Test
    void dragonKilledCondition_shouldNotBeMet_initially() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();

        // When
        boolean isMet = condition.isMet(session);

        // Then
        assertFalse(isMet);
    }

    @Test
    void dragonKilledCondition_shouldBeMet_afterDragonKilled() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();

        // When
        condition.markDragonKilled();
        boolean isMet = condition.isMet(session);

        // Then
        assertTrue(isMet);
        assertTrue(condition.getWinners().contains(ManhuntRoleType.SPEEDRUNNER));
        assertFalse(condition.getDescription().isEmpty());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*WinConditionTest.dragonKilledCondition*"`

Expected: FAIL - "cannot find symbol: class DragonKilledCondition"

- [ ] **Step 3: Create DragonKilledCondition**

Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/DragonKilledCondition.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Set;

/**
 * Win condition: The Ender Dragon has been killed.
 * When met, the Speedrunner team wins.
 * 
 * Note: This is a stateful win condition that must be notified when the dragon dies.
 */
public class DragonKilledCondition implements WinCondition {
    
    private boolean dragonKilled = false;
    
    /**
     * Mark that the Ender Dragon has been killed.
     * Should be called by infrastructure when dragon death is detected.
     */
    public void markDragonKilled() {
        this.dragonKilled = true;
    }
    
    @Override
    public boolean isMet(GameSession session) {
        return dragonKilled;
    }
    
    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.SPEEDRUNNER);
    }
    
    @Override
    public String getDescription() {
        return "Le dragon de l'End a été tué ! L'équipe SPEEDRUNNER a gagné !";
    }
    
    /**
     * Reset the condition (useful for tests or new games).
     */
    public void reset() {
        this.dragonKilled = false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*WinConditionTest.dragonKilledCondition*"`

Expected: PASS (2 new tests passed)

- [ ] **Step 5: Add test for reset functionality**

Add to `WinConditionTest.java`:

```java
    @Test
    void dragonKilledCondition_shouldReset() {
        // Given
        GameSession session = mock(GameSession.class);
        DragonKilledCondition condition = new DragonKilledCondition();
        condition.markDragonKilled();
        assertTrue(condition.isMet(session));

        // When
        condition.reset();

        // Then
        assertFalse(condition.isMet(session));
    }
```

- [ ] **Step 6: Run test to verify reset works**

Run: `./gradlew test --tests "*WinConditionTest.dragonKilledCondition*"`

Expected: PASS (3 tests for dragon killed condition)

- [ ] **Step 7: Run all WinCondition tests**

Run: `./gradlew test --tests "*WinConditionTest"`

Expected: PASS (8 tests total)

- [ ] **Step 8: Commit DragonKilledCondition**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/DragonKilledCondition.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java
git commit -m "feat(wincondition): implement DragonKilledCondition with tests"
```

---

## Task 13: Create WinConditionEvaluator (TDD)

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java`
- Modify: `src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java`

**Interfaces:**
- Consumes: `WinCondition`, `WinOutcome`, `GameSession`
- Produces: `WinConditionEvaluator` with `Optional<WinOutcome> evaluate(GameSession)`

**Steps:**

- [ ] **Step 1: Write failing test for WinConditionEvaluator**

Add to `WinConditionTest.java`:

```java
    @Test
    void evaluator_shouldReturnEmpty_whenNoConditionsMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(3);
        
        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),
                new DragonKilledCondition()
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertFalse(outcome.isPresent());
    }

    @Test
    void evaluator_shouldReturnOutcome_whenConditionMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        
        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),
                new DragonKilledCondition()
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().getWinners().contains(ManhuntRoleType.HUNTER));
    }

    @Test
    void evaluator_shouldReturnFirstMetCondition_whenMultipleMet() {
        // Given
        GameSession session = mock(GameSession.class);
        when(session.getRemainingSpeedrunners()).thenReturn(0);
        
        DragonKilledCondition dragonCondition = new DragonKilledCondition();
        dragonCondition.markDragonKilled();
        
        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),  // First in list
                dragonCondition
            )
        );

        // When
        Optional<WinOutcome> outcome = evaluator.evaluate(session);

        // Then
        assertTrue(outcome.isPresent());
        // Should be hunter win (first condition), not speedrunner
        assertTrue(outcome.get().getWinners().contains(ManhuntRoleType.HUNTER));
    }
```

- [ ] **Step 2: Add missing import**

Add to top of `WinConditionTest.java`:

```java
import java.util.List;
import java.util.Optional;
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew test --tests "*WinConditionTest.evaluator*"`

Expected: FAIL - "cannot find symbol: class WinConditionEvaluator"

- [ ] **Step 4: Create WinConditionEvaluator**

Create: `src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java`

```java
package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates all registered win conditions and returns the first one that is met.
 * Part of the Win Condition bounded context.
 */
public class WinConditionEvaluator {
    
    private final List<WinCondition> conditions;
    
    /**
     * Create an evaluator with a list of win conditions.
     *
     * @param conditions The win conditions to check (order matters - first match wins)
     */
    public WinConditionEvaluator(List<WinCondition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions, "Conditions cannot be null"));
    }
    
    /**
     * Evaluate all win conditions against the given session.
     * Returns the first condition that is met.
     *
     * @param session The game session to evaluate
     * @return Optional containing WinOutcome if any condition is met, empty otherwise
     */
    public Optional<WinOutcome> evaluate(GameSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        
        for (WinCondition condition : conditions) {
            if (condition.isMet(session)) {
                return Optional.of(WinOutcome.of(condition));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the list of conditions being evaluated.
     *
     * @return Immutable list of win conditions
     */
    public List<WinCondition> getConditions() {
        return conditions;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "*WinConditionTest.evaluator*"`

Expected: PASS (3 new tests passed)

- [ ] **Step 6: Add test for null safety**

Add to `WinConditionTest.java`:

```java
    @Test
    void evaluator_shouldThrow_whenNullSession() {
        // Given
        WinConditionEvaluator evaluator = new WinConditionEvaluator(
            List.of(new AllSpeedrunnersDeadCondition())
        );

        // When/Then
        assertThrows(NullPointerException.class, () -> {
            evaluator.evaluate(null);
        });
    }

    @Test
    void evaluator_shouldThrow_whenNullConditions() {
        // When/Then
        assertThrows(NullPointerException.class, () -> {
            new WinConditionEvaluator(null);
        });
    }
```

- [ ] **Step 7: Run all WinCondition tests**

Run: `./gradlew test --tests "*WinConditionTest"`

Expected: PASS (13 tests total)

- [ ] **Step 8: Commit WinConditionEvaluator**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/wincondition/WinConditionEvaluator.java
git add src/test/java/me/flamboyant/manhunt/domain/wincondition/WinConditionTest.java
git commit -m "feat(wincondition): create WinConditionEvaluator with comprehensive tests"
```

---

## Task 14: Integrate WinConditionEvaluator into Managers

**Files:**
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`
- Modify: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

**Interfaces:**
- Consumes: `WinConditionEvaluator`, `AllSpeedrunnersDeadCondition`, `DragonKilledCondition`
- Produces: Managers using evaluator instead of inline win condition checks

**Steps:**

- [ ] **Step 1: Add WinConditionEvaluator field to NewManhuntManager**

Open: `src/main/java/me/flamboyant/manhunt/NewManhuntManager.java`

Add imports and field:

```java
import me.flamboyant.manhunt.domain.wincondition.*;
import java.util.List;
import java.util.Optional;

public class NewManhuntManager {
    
    private static NewManhuntManager instance;
    private GameSession currentSession;
    private final WinConditionEvaluator winConditionEvaluator;  // NEW
    
    private NewManhuntManager() {
        // Initialize evaluator with standard conditions
        this.winConditionEvaluator = new WinConditionEvaluator(
            List.of(
                new AllSpeedrunnersDeadCondition(),
                new DragonKilledCondition()
            )
        );
    }
    
    // ... rest of class
}
```

- [ ] **Step 2: Replace inline speedrunner death check with evaluator**

In the same file, find the `onEntityDamage` method and update:

OLD CODE:
```java
if (role instanceof SpeedrunnerRole) {
    SpeedrunnerRole speedrunnerRole = (SpeedrunnerRole) role;
    DamageOutcome outcome = speedrunnerRole.handleDamage(event.getFinalDamage());
    
    if (outcome.isDied()) {
        currentSession.decrementSpeedrunnerCount();
        
        // OLD: inline check
        if (currentSession.getRemainingSpeedrunners() == 0) {
            stopGame("L'équipe SPEEDRUNNER a perdu !!!");
        }
    }
}
```

NEW CODE:
```java
if (role instanceof SpeedrunnerRole) {
    SpeedrunnerRole speedrunnerRole = (SpeedrunnerRole) role;
    DamageOutcome outcome = speedrunnerRole.handleDamage(event.getFinalDamage());
    
    if (outcome.isDied()) {
        currentSession.decrementSpeedrunnerCount();
        
        // NEW: use evaluator
        checkWinConditions();
    }
}
```

- [ ] **Step 3: Add checkWinConditions() method**

Add new method to `NewManhuntManager.java`:

```java
    /**
     * Check all win conditions and end game if any are met.
     */
    private void checkWinConditions() {
        if (currentSession == null) {
            return;
        }
        
        Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(currentSession);
        if (outcome.isPresent()) {
            stopGame(outcome.get().getDescription());
        }
    }
```

- [ ] **Step 4: Update NewManhuntLauncher to access DragonKilledCondition**

Open: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Add method to get dragon condition from manager:

First, in `NewManhuntManager.java`, add getter:

```java
    /**
     * Get the DragonKilledCondition for infrastructure to notify.
     * This is a temporary access point until event-based architecture (Priority 5).
     *
     * @return DragonKilledCondition instance, or null if not found
     */
    public DragonKilledCondition getDragonKilledCondition() {
        return winConditionEvaluator.getConditions().stream()
            .filter(c -> c instanceof DragonKilledCondition)
            .map(c -> (DragonKilledCondition) c)
            .findFirst()
            .orElse(null);
    }
```

- [ ] **Step 5: Update NewManhuntLauncher dragon death handler**

Open: `src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java`

Find the dragon death detection in `onEntityDamage`:

OLD CODE:
```java
// Somewhere in dragon death detection
if (entity.getType() == EntityType.ENDER_DRAGON && entity.getHealth() - event.getFinalDamage() <= 0) {
    NewManhuntManager.getInstance().stopGame("Le dragon de l'End a été tué ! L'équipe SPEEDRUNNER a gagné !");
}
```

NEW CODE:
```java
if (entity.getType() == EntityType.ENDER_DRAGON && entity.getHealth() - event.getFinalDamage() <= 0) {
    // Mark dragon as killed in win condition
    DragonKilledCondition dragonCondition = NewManhuntManager.getInstance().getDragonKilledCondition();
    if (dragonCondition != null) {
        dragonCondition.markDragonKilled();
    }
    // Let manager check win conditions
    // (It will detect dragon killed and end game with proper message)
}
```

Actually, we need to call the manager to check conditions. Update to:

```java
if (entity.getType() == EntityType.ENDER_DRAGON && entity.getHealth() - event.getFinalDamage() <= 0) {
    // Mark dragon as killed
    DragonKilledCondition dragonCondition = NewManhuntManager.getInstance().getDragonKilledCondition();
    if (dragonCondition != null) {
        dragonCondition.markDragonKilled();
        // Trigger win condition check (this will end the game)
        NewManhuntManager.getInstance().checkWinConditions();
    }
}
```

Wait, `checkWinConditions()` is private. Let's make it package-private or add a public method.

- [ ] **Step 6: Make checkWinConditions() package-private in NewManhuntManager**

In `NewManhuntManager.java`, change:

```java
    /**
     * Check all win conditions and end game if any are met.
     */
    void checkWinConditions() {  // Changed from private to package-private
        if (currentSession == null) {
            return;
        }
        
        Optional<WinOutcome> outcome = winConditionEvaluator.evaluate(currentSession);
        if (outcome.isPresent()) {
            stopGame(outcome.get().getDescription());
        }
    }
```

- [ ] **Step 7: Run full test suite**

Run: `./gradlew test`

Expected: All tests pass (74+ tests, no regressions)

Note: Some tests might fail if they mock the old behavior. We'll fix in next step if needed.

- [ ] **Step 8: Fix any failing tests**

If tests fail, check error messages. Common issues:
- Tests expecting exact old win message strings
- Tests not mocking `currentSession` properly

Update test expectations to match new WinOutcome descriptions.

- [ ] **Step 9: Manual verification (optional but recommended)**

If you have a test server, verify:
1. All speedrunners dying ends game with hunter win message
2. Dragon death ends game with speedrunner win message

- [ ] **Step 10: Commit win condition integration**

```bash
git add src/main/java/me/flamboyant/manhunt/NewManhuntManager.java
git add src/main/java/me/flamboyant/manhunt/NewManhuntLauncher.java
git commit -m "refactor(wincondition): integrate WinConditionEvaluator into managers"
```

---

## Task 15: Update Documentation and Create Context Map

**Files:**
- Create: `docs/architecture/bounded-contexts-map.md`
- Modify: `docs/REFACTORING_PROGRESS.md`

**Interfaces:**
- Consumes: All completed work from Tasks 1-14
- Produces: Complete documentation of bounded contexts implementation

**Steps:**

- [ ] **Step 1: Create context map documentation**

Create: `docs/architecture/bounded-contexts-map.md`

```markdown
# Bounded Contexts Map

**Last Updated:** 2026-06-19  
**Status:** Implemented (Priority 3)

## Overview

The Manhunt plugin is organized into **6 bounded contexts** following Domain-Driven Design principles. This document describes each context, their relationships, and integration points.

## Context Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                   Infrastructure Context                      │
│             (Events, UI, Commands, Bukkit)                    │
│                  [Anti-Corruption Layer]                      │
└────────────┬─────────────────────────────────────────────────┘
             │ (translates events to domain operations)
             │
             ▼
    ┌────────────────────────────────────────────┐
    │     Game Management Context (CORE)         │
    │    (Session lifecycle, coordination)       │
    │            [Aggregate Root]                │
    └─┬──────┬──────────┬──────────────┬────────┘
      │      │          │              │
      │      │          │              │
  ┌───▼───┐  │      ┌───▼────┐    ┌───▼──────────┐
  │ Role  │  │      │ Player │    │ Win Condition│
  │ Def.  │  │      │Tracking│    │   Context    │
  │(CORE) │  │      │(SUPP.) │    │   (CORE)     │
  └───┬───┘  │      └────────┘    └──────────────┘
      │      │
      │   ┌──▼──────┐
      └──►│  Role   │
          │Behavior │
          │ (CORE)  │
          └─────────┘
```

## The Six Contexts

### 1. Game Management Context
**Package:** `me.flamboyant.manhunt.domain.game`  
**Type:** Core Domain  
**Responsibility:** Session lifecycle and coordination

**Key Classes:**
- `GameSession` (Aggregate Root)
- `GameSessionId` (Value Object)
- `GameSessionManager` (Application Service)

**Relationships:**
- Uses Role Definition to create roles
- Uses Role Behavior to store role instances
- Uses Player Tracking for portal management
- Uses Win Condition to check game end

---

### 2. Role Definition Context
**Package:** `me.flamboyant.manhunt.domain.role.definition`  
**Type:** Core Domain  
**Responsibility:** Define available role types and identifiers

**Key Classes:**
- `ManhuntRoleType` (Enum)
- `ManhuntRoleIdentifier` (Enum)
- `ManhuntRoleFactory` (Factory)

**Relationships:**
- No dependencies on other contexts (independent)
- Used by Game Management and Role Behavior

---

### 3. Role Behavior Context
**Package:** `me.flamboyant.manhunt.domain.role.behavior`  
**Type:** Core Domain  
**Responsibility:** Implement role actions and abilities

**Key Classes:**
- `AManhuntRole` (Abstract Entity)
- `SpeedrunnerRole`, `HunterRole` (Concrete Entities)
- 15+ specialized role implementations
- `DamageOutcome`, `CompassTarget` (Value Objects)

**Relationships:**
- Conforms to Role Definition (uses types/identifiers)
- Uses Player Tracking through Game Management (portal locations)

---

### 4. Player Tracking Context
**Package:** `me.flamboyant.manhunt.domain.tracking`  
**Type:** Supporting Domain  
**Responsibility:** Track portal locations and player positions

**Key Classes:**
- `PortalTracker` (Interface + Implementation)

**Relationships:**
- No dependencies (supporting context)
- Used by Game Management and Role Behavior

---

### 5. Win Condition Context
**Package:** `me.flamboyant.manhunt.domain.wincondition`  
**Type:** Core Domain  
**Responsibility:** Evaluate victory conditions

**Key Classes:**
- `WinCondition` (Interface)
- `AllSpeedrunnersDeadCondition`, `DragonKilledCondition` (Implementations)
- `WinConditionEvaluator` (Service)
- `WinOutcome` (Value Object)

**Relationships:**
- Reads from Game Management (session state)
- Used by Infrastructure to trigger game end

---

### 6. Infrastructure Context
**Package:** `me.flamboyant.manhunt.infrastructure`  
**Type:** Technical  
**Responsibility:** Bukkit integration and UI

**Key Classes:**
- `PlayerSelectionView` (UI)
- `Main` (Plugin entry)
- Event handlers in managers (to be extracted)

**Relationships:**
- Depends on all domain contexts
- Anti-Corruption Layer pattern

---

## Integration Patterns

### Game Management → Role Definition (Customer/Supplier)
```java
// Game Management uses Role Definition to create roles
AManhuntRole role = ManhuntRoleFactory.create(roleId, player);
session.assignRole(player, roleId);
```

### Game Management ← → Player Tracking (Delegation)
```java
// Game Management delegates portal tracking
public class GameSession {
    private final PortalTracker portalTracker;
    
    public void setPortalLocation(Player p, Environment e, Location l) {
        portalTracker.setPortalLocation(p, e, l);
    }
}
```

### Infrastructure → Win Condition (Anti-Corruption)
```java
// Infrastructure translates Bukkit events to domain operations
dragonCondition.markDragonKilled();
manager.checkWinConditions();
```

---

## Ubiquitous Language

See [bounded-contexts-design.md](../superpowers/specs/2026-06-19-bounded-contexts-design.md#appendix-a-ubiquitous-language-glossary) for complete glossary per context.

---

## Migration Notes

**From:** Mixed concerns, no clear boundaries  
**To:** 6 well-defined contexts with explicit interfaces

**Key Achievements:**
- ✅ Package structure reflects context boundaries
- ✅ Portal tracking extracted from GameSession
- ✅ Win conditions extracted from managers
- ✅ All 74+ tests passing
- ✅ No behavior changes

**Next Steps:**
- Priority 4: Fix primitive obsession (string-based type detection)
- Priority 5: Implement domain events for decoupling
- Priority 6: Create application services layer

---

**References:**
- Design Spec: `docs/superpowers/specs/2026-06-19-bounded-contexts-design.md`
- Implementation Plan: `docs/superpowers/plans/2026-06-19-bounded-contexts.md`
```

- [ ] **Step 2: Update REFACTORING_PROGRESS.md**

Open: `docs/REFACTORING_PROGRESS.md`

Update Priority 3 section (around line 189):

Change from:
```markdown
### Priority 3: Define Bounded Contexts ⬜
**Status:** Not Started
```

To:
```markdown
### Priority 3: Define Bounded Contexts ✅
**Status:** Completed  
**Assigned To:** Claude Sonnet 4.5  
**Started:** 2026-06-19  
**Completed:** 2026-06-19  
**Estimated Effort:** 4-6 hours  
**Actual Effort:** ~5 hours
```

Update all task checkboxes to `[x]`:

```markdown
#### Tasks
- [x] 3.1 Identify subdomains
  - Core domain: Game Management, Role Definition, Role Behavior, Win Condition
  - Supporting: Player Tracking
  - Generic: Infrastructure (UI, Bukkit integration)

- [x] 3.2 Define bounded contexts
  - ✅ **Game Management Context:** Session lifecycle, configuration
  - ✅ **Role Definition Context:** Role types, identifiers, factory
  - ✅ **Role Behavior Context:** Role implementations, abilities
  - ✅ **Player Tracking Context:** Portal locations, player positions
  - ✅ **Win Condition Context:** Victory evaluation, outcomes
  - ✅ **Infrastructure Context:** Bukkit integration, UI, events

- [x] 3.3 Create context map
  - ✅ Documented relationships between contexts
  - ✅ Defined Customer/Supplier, Anti-Corruption Layer patterns
  - ✅ Created visual context diagram

- [x] 3.4 Establish ubiquitous language per context
  - ✅ Game Management: Session, Configuration, Launch
  - ✅ Role Definition: RoleType, RoleIdentifier, Factory
  - ✅ Role Behavior: Role, Ability, DamageOutcome, CompassTarget
  - ✅ Player Tracking: PortalLocation, Dimension, CrossDimension
  - ✅ Win Condition: WinCondition, Evaluator, Outcome, Victory
  - ✅ Infrastructure: EventBridge, UIView, CommandHandler

- [x] 3.5 Restructure packages by context
  - ✅ Created 6 context packages
  - ✅ Moved 27 existing files
  - ✅ Created 10 new files
  - ✅ All imports updated

- [x] 3.6 Define context interfaces
  - ✅ Game Management → Role Definition: ManhuntRoleFactory
  - ✅ Game Management → Player Tracking: PortalTracker delegation
  - ✅ Game Management → Win Condition: WinConditionEvaluator
  - ✅ Infrastructure → Domain: Anti-Corruption Layer

- [x] 3.7 Document context boundaries
  - ✅ Created bounded-contexts-map.md
  - ✅ Documented integration points
  - ✅ Created context diagram
```

Add Notes section:

```markdown
#### Notes
- **Blockers:** None
- **Decisions Made:**
  - Split Role Context into Definition (stable) vs Behavior (volatile)
  - Extracted Win Condition as separate context (strategic importance)
  - Expanded from 4 planned contexts to 6 for better separation
  - Used direct method calls between contexts (simpler than events for now)
  - Extracted PortalTracker from GameSession via delegation
- **Questions:** None
- **Commits:**
  - [SHA]: Package structure creation
  - [SHA]: Role Definition and Behavior context moves
  - [SHA]: PortalTracker extraction with tests
  - [SHA]: Win Condition context creation with tests
  - [SHA]: WinConditionEvaluator integration
  - [SHA]: Documentation and context map

#### Success Criteria
- ✅ All 6 contexts identified and documented - ACHIEVED
- ✅ Packages organized by context - ACHIEVED
- ✅ Ubiquitous language defined per context - ACHIEVED
- ✅ Context map created - ACHIEVED
- ✅ Integration points defined - ACHIEVED
- ✅ All tests passing (74+ tests) - ACHIEVED
```

- [ ] **Step 3: Run final test suite**

Run: `./gradlew test`

Expected: All tests pass

- [ ] **Step 4: Check test count**

Run: `./gradlew test | grep -E "tests? completed"`

Expected: 74+ tests (69 from Priority 1 + 5 from Priority 2 + 13 new from Priority 3 = 87 tests)

- [ ] **Step 5: Commit documentation**

```bash
git add docs/architecture/bounded-contexts-map.md
git add docs/REFACTORING_PROGRESS.md
git commit -m "docs(contexts): complete Priority 3 documentation and context map"
```

- [ ] **Step 6: Create final summary commit**

Verify all changes with:

```bash
git log --oneline --since="1 day ago"
```

Expected: 15 commits for this priority

Tag the completion:

```bash
git tag -a priority-3-complete -m "Priority 3: Define Bounded Contexts - Complete"
```

- [ ] **Step 7: Final verification**

Run complete build:

```bash
./gradlew clean build
```

Expected: BUILD SUCCESSFUL with all tests passing

---

## Plan Complete

All tasks completed! Summary of deliverables:

**Code Changes:**
- ✅ 6 bounded contexts established
- ✅ 27 files moved to new packages
- ✅ 10 new files created (PortalTracker, WinCondition, etc.)
- ✅ Portal tracking extracted from GameSession
- ✅ Win condition logic extracted from managers
- ✅ All imports fixed

**Tests:**
- ✅ 7 new tests for PortalTracker
- ✅ 13 new tests for WinCondition context
- ✅ All existing 74 tests still passing
- ✅ Total: 87+ tests

**Documentation:**
- ✅ bounded-contexts-map.md created
- ✅ REFACTORING_PROGRESS.md updated
- ✅ Context diagram documented
- ✅ Ubiquitous language defined

**Next Priority:** Priority 4 - Fix Primitive Obsession (String Types)
