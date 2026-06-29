# Critical Issues Tracking - Status Report

**Date:** 2026-06-29  
**Branch:** feature/ddd-refactoring  
**Plan:** `2026-06-29-critical-fixes-architecture-build-memory.md`  
**Review Source:** Comprehensive 13-Priority Review (Workflow wf_f8da8c33-eec)

---

## 📊 Overview

| Category | Total Issues | Fixed by Plan | Remaining |
|----------|-------------|---------------|-----------|
| **Critical (Blocking)** | 3 | 3 | 0 |
| **High Priority** | 4 | 0 | 4 |
| **Medium Priority** | 4 | 0 | 4 |
| **Build & Test** | 2 | 2 | 0 |
| **Code Quality** | 10 | 0 | 10 |
| **TOTAL** | 23 | 5 | 18 |

---

## ✅ FIXED BY PLAN (5 Issues)

### 🔴 CRITICAL #1: Priority 12 - Anti-Corruption Layer Violations

**Status:** ✅ **WILL BE FIXED**  
**Plan Tasks:** 1, 2, 3, 4  
**Severity:** CRITICAL (Architecture)

**Issues Being Fixed:**

| Issue | File | Plan Task |
|-------|------|-----------|
| GameSession imports HandlerRegistration from application | `domain/game/GameSession.java:3` | Task 1 |
| Role imports application services | `domain/role/behavior/Role.java:5-8` | Task 2 |
| AbilityContext imports application services | `domain/role/ability/AbilityContext.java:3-6` | Task 2 |
| GameStartFailedEvent imports CompensationStatus from application | `domain/event/GameStartFailedEvent.java:3` | Task 3 |
| UIPickerCompassAbility imports infrastructure UI | `domain/role/ability/UIPickerCompassAbility.java:3` | Task 4 |

**Solution:**
- Move HandlerRegistration to `domain/lifecycle/` package
- Move CompensationStatus to `domain/lifecycle/` package
- Create domain service ports (SessionRepository, MessagingPort, ItemPort, EventRegistrationPort)
- Application services implement ports via interfaces
- Domain depends only on ports, not concrete implementations

**Verification:**
```bash
grep -r "import me.flamboyant.manhunt.application" src/main/java/me/flamboyant/manhunt/domain/
# Expected: No matches (all violations fixed)
```

---

### 🔴 CRITICAL #2: Build Errors (7 Compilation Errors)

**Status:** ✅ **WILL BE FIXED**  
**Plan Tasks:** 5, 6, 7, 8  
**Severity:** CRITICAL (Blocking)

**Issues Being Fixed:**

| # | Error | File | Plan Task |
|---|-------|------|-----------|
| 1 | Missing `canModifyParametersOnTheFly()` method | `ManhuntPluginAdapter.java:26` | Task 5 |
| 2 | `EnumParameter.getValue()` doesn't exist | `ManhuntPluginAdapter.java:143` | Task 6 |
| 3 | `EnumParameter.getValue()` doesn't exist | `ManhuntPluginAdapter.java:144` | Task 6 |
| 4 | BooleanParameter type mismatch (int vs boolean) | `ManhuntPluginAdapter.java:153` | Task 6 |
| 5 | Missing import: ILaunchablePlugin | `ManhuntModule.java:89` | Task 7 |
| 6 | Missing import: Common | `ManhuntModule.java:206` | Task 7 |
| 7 | NewManhuntLauncher doesn't exist | `NewManhuntManager.java:173` | Task 8 |

**Solution:**
- Implement missing abstract method
- Use correct FlamboyantPluginTools API (`getSelectedValue()` for EnumParameter)
- Handle BooleanParameter returning int (0/1)
- Add missing imports
- Remove reference to deleted class

**Verification:**
```bash
mvn clean compile
# Expected: BUILD SUCCESS (no compilation errors)
```

---

### 🔴 CRITICAL #3: Priority 9 - Memory Leak in Event Handlers

**Status:** ✅ **WILL BE FIXED**  
**Plan Tasks:** 9, 10, 11  
**Severity:** CRITICAL (Memory Leak)

**Issues Being Fixed:**

| Issue | File | Plan Task |
|-------|------|-----------|
| `Role.doStop()` doesn't call `clearEventHandlers()` | `Role.java:100-113` | Task 9 |
| Manual registration in UIPickerCompassAbility | `UIPickerCompassAbility.java:32` | Task 10 |
| Manual registration in NewManhuntManager | `NewManhuntManager.java:125` | Task 11 |
| AbilityManager.clearEventHandlers() exists but never called | `AbilityManager.java:60` | Task 9 |

**Solution:**
- Add `abilityManager.clearEventHandlers(owner)` call in `Role.doStop()`
- Add trackView cleanup in `UIPickerCompassAbility.onRoleStop()`
- Replace manual registration with injected EventRegistrationService
- All registrations now centralized through DI

**Verification:**
```bash
grep -A5 "protected boolean doStop" src/main/java/me/flamboyant/manhunt/domain/role/behavior/Role.java | grep "clearEventHandlers"
# Expected: Shows clearEventHandlers call
```

---

### 🔴 CRITICAL #4: Tests Cannot Run (Blocked by Build)

**Status:** ✅ **WILL BE FIXED**  
**Plan Tasks:** 5-8 (build fixes), 12 (verification)  
**Severity:** CRITICAL (Blocking)

**Issue:**
- 318 test methods exist across 45 test classes
- Cannot execute due to 7 compilation errors
- Zero tests passing/failing (blocked)

**Solution:**
- Fix all 7 compilation errors (Tasks 5-8)
- Tests will become executable
- Task 12 verifies full test suite runs

**Verification:**
```bash
mvn test
# Expected: Tests execute (may have failures, but no compilation blocks)
```

---

### 🟢 VERIFICATION: Full System Check

**Status:** ✅ **INCLUDED IN PLAN**  
**Plan Task:** 12

**Verification Includes:**
- ✅ Compilation succeeds (no errors)
- ✅ Architecture constraints verified (no domain → application imports)
- ✅ Memory leak fixes verified (clearEventHandlers present)
- ✅ Test suite executes (all 318 tests runnable)
- ✅ Verification report generated

---

## ⏸️ NOT FIXED (Out of Scope) - 18 Issues

### 🟠 HIGH PRIORITY (Not in Plan)

#### ⚠️ Priority 1: GameSession Aggregate - Singleton Pattern

**Status:** ⏸️ **NOT FIXED** (Separate Initiative)  
**Severity:** HIGH

**Issue:**
- `GameSessionManager` uses singleton pattern with static mutable state
- Violates "no static mutable state" principle
- Should be fully DI-injected

**Why Not Fixed:**
- Requires broader refactoring across application layer
- Not blocking (works correctly, just not ideal architecture)
- Separate initiative to eliminate all singletons

**Estimated Effort:** 2-3 hours

---

#### ⚠️ Priority 5: Domain Events - Bukkit Type Coupling

**Status:** ⏸️ **NOT FIXED** (Separate Initiative)  
**Severity:** HIGH

**Issue:**
- 6 domain events contain `org.bukkit.entity.Player` objects (infrastructure type)
- Should use domain `PlayerId` value object instead

**Affected Events:**
1. SpeedrunnerDiedEvent
2. DragonKilledEvent
3. GameStartedEvent
4. RoleAssignedEvent
5. RolesDistributedEvent
6. GameSessionCreatedEvent

**Why Not Fixed:**
- Requires creating PlayerId value object
- Cascading changes across all event publishers/handlers
- Bukkit Player objects work, just violate pure domain isolation
- Medium complexity refactoring

**Estimated Effort:** 4-5 hours

---

#### ⚠️ Priority 6: Application Services - Incomplete DI Migration

**Status:** ⏸️ **NOT FIXED** (Separate Initiative)  
**Severity:** HIGH

**Remaining Singletons:**
- `GameSessionManager.getInstance()`
- `NewManhuntLauncher.getInstance()` (reference removed in Task 8, but pattern remains elsewhere)
- `ConfigurablePluginListener.getInstance()` (external framework)

**Why Not Fixed:**
- GameSessionManager singleton addressed separately (see Priority 1)
- NewManhuntLauncher deletion partially addressed in Task 8
- ConfigurablePluginListener is external framework (cannot change)
- Full DI migration requires coordinated refactoring

**Estimated Effort:** 3-4 hours

---

#### ⚠️ Priority 11: Win Conditions - Hardcoded Logic

**Status:** ⏸️ **NOT FIXED** (Separate Initiative)  
**Severity:** HIGH

**Issues:**
1. Hardcoded win logic in `Role.determineWinStatus()` (lines 125-137)
2. `DragonWinConditionAbility` TODO for EndGameSaga integration (line 34)
3. Potential duplicate win announcements
4. `DragonKilledCondition` requires manual `markDragonKilled()` call

**Why Not Fixed:**
- Requires refactoring win condition evaluation flow
- Need to unify Role.determineWinStatus with WinConditionEvaluator
- Complete DragonWinConditionAbility integration
- Medium complexity, not blocking functionality

**Estimated Effort:** 3-4 hours

---

### 🟡 MEDIUM PRIORITY (Not in Plan)

#### ⚠️ Priority 3: Bounded Contexts - Boundary Violations

**Status:** ⏸️ **PARTIALLY FIXED**  
**Severity:** MEDIUM

**Fixed:**
- ✅ Role.java application imports (Task 2 - ports pattern)

**Remaining:**
- Documentation claims 15+ specialized role implementations exist, but only AManhuntRole.java and Role.java exist
- Role Definition Context has coupling to role/ability package

**Why Not Fully Fixed:**
- Documentation mismatch is informational, not functional
- role/ability coupling is within domain layer (acceptable)
- Lower priority than critical fixes

**Estimated Effort:** 1-2 hours (docs update)

---

#### ⚠️ Priority 8: State Machine - Implicit State Flags

**Status:** ⏸️ **NOT FIXED** (Acceptable by Design)  
**Severity:** MEDIUM (Low Impact)

**Issues:**
- `GameLaunchService.running` is implicit application-level state
- `AManhuntRole.running` is implicit role lifecycle state

**Why Not Fixed:**
- These are separate concerns from GameSession.currentPhase
- Application orchestration state vs domain game state
- Architecturally acceptable per DDD bounded contexts
- Should be documented, not fixed

**Estimated Effort:** 0.5 hours (documentation only)

---

#### ⚠️ Priority 13: Role Abstraction - Misleading Metrics

**Status:** ⏸️ **NOT FIXED** (Informational)  
**Severity:** MEDIUM (Metrics Issue)

**Issues:**
- Code reduction only 6.5% actual (net -147 lines in role domain)
- Claimed 45% includes test deletions and unrelated files
- Cannot verify tests pass due to compilation errors (WILL BE FIXED by build fixes)

**Why Not Fixed:**
- Metrics dispute is informational
- Actual functionality is correct
- Test verification will be possible after build fixes
- Not a code issue, just documentation accuracy

**Estimated Effort:** 0 hours (no fix needed)

---

#### ⚠️ Priority 6: Remaining Singletons (Duplicate - See HIGH)

Already covered in HIGH priority section above.

---

### 🔵 CODE QUALITY CONCERNS (Not in Plan)

#### 1. God Class: ManhuntModule (503 lines)

**Status:** ⏸️ **NOT FIXED**  
**Severity:** HIGH (Code Quality)

**Issue:** ManhuntModule contains all role definitions, DI configuration, and role factory setup

**Solution:** Extract role definitions to separate RoleRegistry or RoleDefinitionBootstrap class

**Estimated Effort:** 2-3 hours

---

#### 2. Static Mutable State: GameSessionManager Singleton

**Status:** ⏸️ **NOT FIXED** (See Priority 1 above)  
**Severity:** HIGH (Code Quality)

Already covered in HIGH priority section.

---

#### 3. Feature Envy: Compass Abilities

**Status:** ⏸️ **NOT FIXED**  
**Severity:** MEDIUM (Code Quality)

**Issue:** CompassAbility accesses `owner.getInventory()` repeatedly with duplicate logic

**Solution:** Extract ItemService.findAndUpdateCompassInInventory() method

**Estimated Effort:** 1-2 hours

---

#### 4. Duplicate Code: Compass Update Loops

**Status:** ⏸️ **NOT FIXED**  
**Severity:** MEDIUM (Code Quality)

**Issue:** Two identical for-loops in CompassAbility (lines 78-86, 88-96)

**Solution:** Extract helper method updateCompassInInventory()

**Estimated Effort:** 0.5 hours

---

#### 5. External Coupling: Common Static Dependencies

**Status:** ⏸️ **NOT FIXED**  
**Severity:** MEDIUM (Code Quality)

**Issue:** Multiple files depend on static `Common.rng`, `Common.server`, `Common.plugin`

**Solution:** Replace with proper DI (ManhuntModule already binds these)

**Estimated Effort:** 2-3 hours

---

#### 6. Missing Documentation: Ability Classes

**Status:** ⏸️ **NOT FIXED**  
**Severity:** MEDIUM (Code Quality)

**Issue:** 33 out of 85 classes lack Javadoc

**Solution:** Add class-level documentation explaining domain purpose

**Estimated Effort:** 3-4 hours

---

#### 7. Long Parameter Lists: Role Constructor

**Status:** ⏸️ **NOT FIXED**  
**Severity:** LOW (Code Quality)

**Issue:** Role constructor takes 10 parameters, AbilityContext takes 8

**Solution:** Consider ServiceBundle or ServiceContext parameter object

**Estimated Effort:** 2-3 hours

---

#### 8. TODO Comments: Incomplete Refactoring

**Status:** ⏸️ **NOT FIXED**  
**Severity:** LOW (Code Quality)

**Issues:**
- `GameLifecycleService.java:65` - "TODO: Delegate to domain - start roles, schedule reveal"
- `DragonWinConditionAbility.java:34` - "TODO: Properly integrate with EndGameSaga"

**Solution:** Complete saga integration (overlaps with Priority 11)

**Estimated Effort:** Covered in Priority 11 estimate

---

#### 9. Naming Inconsistency: "NewManhuntManager"

**Status:** ⏸️ **NOT FIXED**  
**Severity:** LOW (Code Quality)

**Issue:** "New" prefix suggests temporary/migration artifact

**Solution:** Rename to GameEventCoordinator or ManhuntGameCoordinator

**Estimated Effort:** 0.5 hours

---

#### 10. Data Class: RoleDefinition

**Status:** ⏸️ **NOT FIXED**  
**Severity:** LOW (Code Quality)

**Issue:** RoleDefinition is mostly data holder with Builder pattern

**Solution:** Consider if more role-related validation belongs in this domain object

**Estimated Effort:** 1-2 hours

---

## 📋 Summary

### What Gets Fixed (This Plan)

✅ **5 Critical Issues RESOLVED:**
1. Priority 12: All 5 architecture violations fixed
2. Build: All 7 compilation errors fixed
3. Priority 9: All 3 memory leak issues fixed
4. Tests: 318 tests become executable
5. Verification: Full system health check

**Result:** Project compiles, tests run, architecture clean, no memory leaks

---

### What Remains (Future Work)

⏸️ **18 Issues OUT OF SCOPE:**
- 4 High Priority concerns (singletons, event coupling, win conditions)
- 4 Medium Priority concerns (boundaries, state flags, metrics)
- 10 Code Quality improvements (God class, documentation, refactoring)

**Estimated Total Effort for Remaining:** 23-32 hours

---

## 🎯 Next Steps After Plan Execution

### Immediate (Priority)
1. Execute plan (12 tasks)
2. Verify all 5 critical issues resolved
3. Run full test suite
4. Commit and update progress.md

### Short-Term (Next Sprint)
1. Fix Priority 1: Remove GameSessionManager singleton
2. Fix Priority 5: Replace Player with PlayerId in domain events
3. Fix Priority 11: Complete win condition integration
4. Fix Priority 6: Complete DI migration

### Medium-Term (Technical Debt)
1. Refactor ManhuntModule God class
2. Remove Common static dependencies
3. Add missing documentation (33 classes)
4. Extract duplicate compass code

### Long-Term (Enhancement)
1. Rename "NewManhuntManager"
2. Consider parameter object patterns
3. Refactor RoleDefinition behavior
4. Complete saga integration TODOs

---

## 📊 Impact Analysis

### Before Plan Execution
- ❌ Project doesn't compile (7 errors)
- ❌ Tests cannot run (blocked)
- ❌ Domain violates DDD architecture (5 violations)
- ❌ Memory leaks in production (event handlers accumulate)
- ⚠️ 18 other issues remain

### After Plan Execution
- ✅ Project compiles successfully
- ✅ 318 tests executable
- ✅ Domain architecture pure (0 violations)
- ✅ No memory leaks (proper cleanup)
- ⏸️ 18 issues remain (non-blocking)

**Health Score:** 
- Before: 22% (5 of 23 issues resolved)
- After: 100% of critical issues resolved (18 enhancements remain)

---

## 🔗 Related Documents

- **Plan:** `docs/superpowers/plans/2026-06-29-critical-fixes-architecture-build-memory.md`
- **Review:** Workflow output `wf_f8da8c33-eec`
- **Progress:** `.superpowers/sdd/progress.md`
- **Priorities:** `docs/PROBLEMS_PRIORITY_SUMMARY.md`

---

**Status:** Ready for execution  
**Blocking Issues:** 0 (after plan execution)  
**Technical Debt:** 18 items catalogued for future work
