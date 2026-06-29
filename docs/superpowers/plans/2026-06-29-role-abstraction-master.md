# Role Abstraction Improvement - Master Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace 19 role classes with a composition-based ability system to eliminate duplication, separate concerns, and improve testability.

**Architecture:** Command pattern with ability composition. Roles become containers that delegate to reusable Ability instances. AbilityManager handles event routing and cooldowns centrally.

**Tech Stack:** Java 8, Bukkit API, Google Guice, JUnit 5, Mockito

## Global Constraints

- Java 8 compatibility (no `var`, no records, streams OK)
- All ability classes in `me.flamboyant.manhunt.domain.role.ability` package
- Event filtering boilerplate handled once in base classes, never in concrete abilities
- All tests use JUnit 5 + Mockito
- TDD: tests written before implementation for every ability
- No changes to game behavior - roles behave identically to current implementation
- Commit after every completed task

---

## Implementation Phases

This plan is split into 4 phases, each with its own detailed sub-plan:

### Phase 1: Build Ability Framework (3-4 hours)
**Plan:** `2026-06-29-role-abstraction-phase1.md`

**Tasks:**
1. Ability Interface and AbilityContext
2. CooldownTracker
3. AbilityManager
4. Base Ability Classes (ItemActivatedAbility, PassiveAbility, WinConditionAbility)

**Deliverable:** Framework ready, no impact on existing roles

**Success Criteria:**
- All interface and base class tests pass
- AbilityManager routes events correctly
- CooldownTracker manages cooldowns per player
- Framework documented and committed

---

### Phase 2: Implement Concrete Abilities (4-5 hours)
**Plan:** `2026-06-29-role-abstraction-phase2.md`

**Tasks:**
5. Compass Abilities (UIPickerCompass, CyclingCompass)
6. Checkpoint Abilities (Save, Rollback, Storage)
7. Passive Abilities (GrassDrop, SwordSound, CutClean)
8. Win Condition Abilities (DragonWin, SpeedrunnerDeath, PortalTracking)
9. Specialized Abilities (TntTactical, Werewolf, ProMiner, Elf, Gluer, Imposter)
10. Utility Abilities (CompassOnStart, CompassOnRespawn, NoNameTag, Swapper, Undecided)

**Deliverable:** ~19 tested ability classes, no impact on existing roles

**Success Criteria:**
- Each ability has 3-5 unit tests
- All abilities tested in isolation
- No framework dependencies in ability logic
- All tests pass

---

### Phase 3: Register Role Definitions (2-3 hours)
**Plan:** `2026-06-29-role-abstraction-phase3.md`

**Tasks:**
11. RoleDefinition and RoleDefinitionRegistry
12. Register all 19 role definitions
13. Migration tests

**Deliverable:** Parallel system ready, old classes still present

**Success Criteria:**
- All 19 roles have definitions
- Registry validated with tests
- Migration tests verify equivalence
- No changes to existing role classes yet

---

### Phase 4: Cutover & Delete (2-3 hours)
**Plan:** `2026-06-29-role-abstraction-phase4.md`

**Tasks:**
14. Create new Role class
15. Update AssistedRoleFactory and ManhuntModule
16. Delete old role classes and update documentation

**Deliverable:** Clean cutover, ~1000 lines of code deleted

**Success Criteria:**
- All integration tests pass
- All 19 roles work identically to before
- Old role classes deleted
- Documentation updated
- ~33% code reduction achieved

---

## Execution Order

Execute phases sequentially:

1. **Start with Phase 1** - Complete all 4 tasks in `2026-06-29-role-abstraction-phase1.md`
2. **Then Phase 2** - Complete all 6 tasks in `2026-06-29-role-abstraction-phase2.md`
3. **Then Phase 3** - Complete all 3 tasks in `2026-06-29-role-abstraction-phase3.md`
4. **Finally Phase 4** - Complete all 3 tasks in `2026-06-29-role-abstraction-phase4.md`

Each phase builds on the previous. Do not skip ahead.

---

## Rollback Strategy

If issues arise during Phase 4:
1. Revert the cutover commit
2. Old role classes still exist in git history
3. Switch AssistedRoleFactory back to old implementation
4. System returns to pre-migration state

---

## Success Criteria (Overall)

### Functional Requirements
- ✅ All 19 roles work exactly as before
- ✅ Win conditions unchanged
- ✅ Cooldowns work correctly
- ✅ Event handling preserved
- ✅ No regressions in game behavior

### Code Quality Requirements
- ✅ No event filtering boilerplate in ability classes
- ✅ Cooldowns managed centrally, not scattered
- ✅ No manual event registration/cleanup in roles
- ✅ Abilities testable without Bukkit mocks
- ✅ ~33% code reduction (3000 lines → 2000 lines)

### Testing Requirements
- ✅ 50-60 unit tests for abilities
- ✅ 20 integration tests for role composition
- ✅ 19 migration tests (one per role)
- ✅ All tests pass
- ✅ Test coverage >80% for ability classes

---

**Ready to begin:** Start with Phase 1 plan: `2026-06-29-role-abstraction-phase1.md`
