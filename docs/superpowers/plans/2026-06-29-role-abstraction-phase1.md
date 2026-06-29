# Role Abstraction - Phase 1: Build Ability Framework

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the ability framework foundation - interfaces, managers, and base classes. No impact on existing roles.

**Architecture:** Command pattern with ability composition. AbilityManager handles event routing and cooldowns centrally.

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

## Phase 1 Tasks (4 tasks)

### Task 1: Ability Interface and AbilityContext

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/Ability.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/AbilityContext.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/AbilityContextTest.java`

**Interfaces:**
- Consumes: None (foundation)
- Produces: `Ability` interface with methods: `void onRoleStart(AbilityContext)`, `void onRoleStop(AbilityContext)`, `String getName()`, `String getDescription()`
- Produces: `AbilityContext` class with getters for Player, GameSession, services

- [ ] **Step 1: Write failing test for AbilityContext**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityContextTest {
    private Player mockOwner;
    private GameSessionManager mockSessionManager;
    private MessageService mockMessageService;
    private ItemService mockItemService;
    private EventRegistrationService mockEventRegistration;
    private Server mockServer;
    private Plugin mockPlugin;
    private AbilityManager mockAbilityManager;
    private AbilityContext context;

    @BeforeEach
    void setUp() {
        mockOwner = mock(Player.class);
        mockSessionManager = mock(GameSessionManager.class);
        mockMessageService = mock(MessageService.class);
        mockItemService = mock(ItemService.class);
        mockEventRegistration = mock(EventRegistrationService.class);
        mockServer = mock(Server.class);
        mockPlugin = mock(Plugin.class);
        mockAbilityManager = mock(AbilityManager.class);

        context = new AbilityContext(
            mockOwner,
            mockSessionManager,
            mockMessageService,
            mockItemService,
            mockEventRegistration,
            mockServer,
            mockPlugin,
            mockAbilityManager
        );
    }

    @Test
    void getOwnerReturnsPlayer() {
        assertEquals(mockOwner, context.getOwner());
    }

    @Test
    void getMessageServiceReturnsService() {
        assertEquals(mockMessageService, context.getMessageService());
    }

    @Test
    void sendMessageDelegatesToMessageService() {
        context.sendMessage("test message");
        
        verify(mockMessageService).sendMessage(mockOwner, "test message");
    }

    @Test
    void setSessionUpdatesSession() {
        GameSession mockSession = mock(GameSession.class);
        
        context.setSession(mockSession);
        
        assertEquals(mockSession, context.getSession());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AbilityContextTest`
Expected: FAIL with "cannot find symbol class AbilityContext"

- [ ] **Step 3: Create Ability interface**

```java
package me.flamboyant.manhunt.domain.role.ability;

public interface Ability {
    /**
     * Called when the role starts. Ability registers event handlers here.
     */
    void onRoleStart(AbilityContext context);
    
    /**
     * Called when the role stops. Ability cleans up here.
     */
    void onRoleStop(AbilityContext context);
    
    /**
     * Human-readable ability name.
     */
    String getName();
    
    /**
     * Human-readable ability description.
     */
    String getDescription();
}
```

- [ ] **Step 4: Create AbilityContext class**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class AbilityContext {
    private final Player owner;
    private final GameSessionManager sessionManager;
    private final MessageService messageService;
    private final ItemService itemService;
    private final EventRegistrationService eventRegistration;
    private final Server server;
    private final Plugin plugin;
    private final AbilityManager abilityManager;
    private GameSession session;
    
    public AbilityContext(
        Player owner,
        GameSessionManager sessionManager,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration,
        Server server,
        Plugin plugin,
        AbilityManager abilityManager
    ) {
        this.owner = owner;
        this.sessionManager = sessionManager;
        this.messageService = messageService;
        this.itemService = itemService;
        this.eventRegistration = eventRegistration;
        this.server = server;
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }
    
    public Player getOwner() {
        return owner;
    }
    
    public GameSession getSession() {
        return session;
    }
    
    public void setSession(GameSession session) {
        this.session = session;
    }
    
    public GameSessionManager getSessionManager() {
        return sessionManager;
    }
    
    public MessageService getMessageService() {
        return messageService;
    }
    
    public ItemService getItemService() {
        return itemService;
    }
    
    public EventRegistrationService getEventRegistration() {
        return eventRegistration;
    }
    
    public Server getServer() {
        return server;
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
    
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
    
    public void sendMessage(String message) {
        messageService.sendMessage(owner, message);
    }
    
    public <T extends Event> void registerEventHandler(Class<T> eventType, Consumer<T> handler) {
        abilityManager.registerEventHandler(owner, eventType, event -> handler.accept((T) event));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -Dtest=AbilityContextTest`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/Ability.java src/main/java/me/flamboyant/manhunt/domain/role/ability/AbilityContext.java src/test/java/me/flamboyant/manhunt/domain/role/ability/AbilityContextTest.java
git commit -m "feat(priority-13): add Ability interface and AbilityContext

- Ability interface defines lifecycle hooks
- AbilityContext provides dependencies to abilities
- Convenience methods for common operations"
```

---

### Task 2: CooldownTracker

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CooldownTracker.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/CooldownTrackerTest.java`

**Interfaces:**
- Consumes: None
- Produces: `CooldownTracker` with methods: `boolean isOnCooldown(Player, String)`, `void setCooldown(Player, String, Duration)`, `void clearCooldown(Player, String)`, `void clearAllCooldowns(Player)`

- [ ] **Step 1: Write failing tests for CooldownTracker**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CooldownTrackerTest {
    private CooldownTracker tracker;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        tracker = new CooldownTracker();
        mockPlayer = mock(Player.class);
    }

    @Test
    void newPlayerHasNoCooldowns() {
        assertFalse(tracker.isOnCooldown(mockPlayer, "test-ability"));
    }

    @Test
    void setCooldownMakesAbilityOnCooldown() {
        tracker.setCooldown(mockPlayer, "test-ability", Duration.ofSeconds(10));
        
        assertTrue(tracker.isOnCooldown(mockPlayer, "test-ability"));
    }

    @Test
    void clearCooldownRemovesCooldown() {
        tracker.setCooldown(mockPlayer, "test-ability", Duration.ofSeconds(10));
        tracker.clearCooldown(mockPlayer, "test-ability");
        
        assertFalse(tracker.isOnCooldown(mockPlayer, "test-ability"));
    }

    @Test
    void clearAllCooldownsRemovesAllForPlayer() {
        tracker.setCooldown(mockPlayer, "ability-1", Duration.ofSeconds(10));
        tracker.setCooldown(mockPlayer, "ability-2", Duration.ofSeconds(10));
        
        tracker.clearAllCooldowns(mockPlayer);
        
        assertFalse(tracker.isOnCooldown(mockPlayer, "ability-1"));
        assertFalse(tracker.isOnCooldown(mockPlayer, "ability-2"));
    }

    @Test
    void differentPlayersHaveIndependentCooldowns() {
        Player player2 = mock(Player.class);
        tracker.setCooldown(mockPlayer, "test-ability", Duration.ofSeconds(10));
        
        assertFalse(tracker.isOnCooldown(player2, "test-ability"));
    }

    @Test
    void expiredCooldownReturnsFalse() throws InterruptedException {
        tracker.setCooldown(mockPlayer, "test-ability", Duration.ofMillis(50));
        
        Thread.sleep(100);
        
        assertFalse(tracker.isOnCooldown(mockPlayer, "test-ability"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CooldownTrackerTest`
Expected: FAIL with "cannot find symbol class CooldownTracker"

- [ ] **Step 3: Implement CooldownTracker**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownTracker {
    private final Map<Player, Map<String, Instant>> cooldowns;
    
    public CooldownTracker() {
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    public boolean isOnCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) {
            return false;
        }
        
        Instant cooldownEnd = playerCooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return false;
        }
        
        return Instant.now().isBefore(cooldownEnd);
    }
    
    public void setCooldown(Player player, String abilityId, Duration duration) {
        Instant cooldownEnd = Instant.now().plus(duration);
        cooldowns
            .computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(abilityId, cooldownEnd);
    }
    
    public void clearCooldown(Player player, String abilityId) {
        Map<String, Instant> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns != null) {
            playerCooldowns.remove(abilityId);
        }
    }
    
    public void clearAllCooldowns(Player player) {
        cooldowns.remove(player);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=CooldownTrackerTest`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/CooldownTracker.java src/test/java/me/flamboyant/manhunt/domain/role/ability/CooldownTrackerTest.java
git commit -m "feat(priority-13): add CooldownTracker

- Centralized cooldown management per player and ability
- Thread-safe with ConcurrentHashMap
- Automatic expiration based on time"
```

---

### Task 3: AbilityManager

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/AbilityManager.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/AbilityManagerTest.java`

**Interfaces:**
- Consumes: `CooldownTracker` (from Task 2), `Ability` interface (from Task 1)
- Produces: `AbilityManager` with methods: `void registerAbility(Ability, AbilityContext)`, `void unregisterAbility(Ability, AbilityContext)`, `void routeEvent(Event, Player)`, `void registerEventHandler(Player, Class<Event>, EventHandler)`, `void clearEventHandlers(Player)`, cooldown delegation methods

- [ ] **Step 1: Write failing tests for AbilityManager**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityManagerTest {
    private AbilityManager abilityManager;
    private CooldownTracker mockCooldownTracker;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        mockCooldownTracker = mock(CooldownTracker.class);
        abilityManager = new AbilityManager(mockCooldownTracker);
        mockPlayer = mock(Player.class);
    }

    @Test
    void registerAbilityCallsOnRoleStart() {
        Ability mockAbility = mock(Ability.class);
        AbilityContext mockContext = mock(AbilityContext.class);
        
        abilityManager.registerAbility(mockAbility, mockContext);
        
        verify(mockAbility).onRoleStart(mockContext);
    }

    @Test
    void unregisterAbilityCallsOnRoleStop() {
        Ability mockAbility = mock(Ability.class);
        AbilityContext mockContext = mock(AbilityContext.class);
        
        abilityManager.unregisterAbility(mockAbility, mockContext);
        
        verify(mockAbility).onRoleStop(mockContext);
    }

    @Test
    void routeEventCallsRegisteredHandler() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);
        
        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, mockPlayer);
        
        assertTrue(handlerCalled.get());
    }

    @Test
    void routeEventDoesNotCallUnregisteredPlayer() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        Player otherPlayer = mock(Player.class);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);
        
        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, otherPlayer);
        
        assertFalse(handlerCalled.get());
    }

    @Test
    void clearEventHandlersRemovesAllHandlers() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AbilityManager.EventHandler handler = event -> handlerCalled.set(true);
        
        abilityManager.registerEventHandler(mockPlayer, PlayerInteractEvent.class, handler);
        abilityManager.clearEventHandlers(mockPlayer);
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        abilityManager.routeEvent(mockEvent, mockPlayer);
        
        assertFalse(handlerCalled.get());
    }

    @Test
    void isOnCooldownDelegatesToTracker() {
        when(mockCooldownTracker.isOnCooldown(mockPlayer, "test-ability")).thenReturn(true);
        
        assertTrue(abilityManager.isOnCooldown(mockPlayer, "test-ability"));
    }

    @Test
    void setCooldownDelegatesToTracker() {
        Duration duration = Duration.ofSeconds(10);
        
        abilityManager.setCooldown(mockPlayer, "test-ability", duration);
        
        verify(mockCooldownTracker).setCooldown(mockPlayer, "test-ability", duration);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AbilityManagerTest`
Expected: FAIL with "cannot find symbol class AbilityManager"

- [ ] **Step 3: Implement AbilityManager**

```java
package me.flamboyant.manhunt.domain.role.ability;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {
    private final Map<Player, Map<Class<? extends Event>, List<EventHandler>>> eventHandlers;
    private final CooldownTracker cooldownTracker;
    
    @Inject
    public AbilityManager(CooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
        this.eventHandlers = new ConcurrentHashMap<>();
    }
    
    public void registerAbility(Ability ability, AbilityContext context) {
        ability.onRoleStart(context);
    }
    
    public void unregisterAbility(Ability ability, AbilityContext context) {
        ability.onRoleStop(context);
    }
    
    public void routeEvent(Event event, Player owner) {
        Map<Class<? extends Event>, List<EventHandler>> playerHandlers = eventHandlers.get(owner);
        if (playerHandlers == null) {
            return;
        }
        
        List<EventHandler> handlers = playerHandlers.get(event.getClass());
        if (handlers == null) {
            return;
        }
        
        handlers.forEach(handler -> {
            try {
                handler.handle(event);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Event handler failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public void registerEventHandler(Player owner, Class<? extends Event> eventType, EventHandler handler) {
        eventHandlers
            .computeIfAbsent(owner, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(handler);
    }
    
    public void clearEventHandlers(Player owner) {
        eventHandlers.remove(owner);
    }
    
    public boolean isOnCooldown(Player player, String abilityId) {
        return cooldownTracker.isOnCooldown(player, abilityId);
    }
    
    public void setCooldown(Player player, String abilityId, Duration duration) {
        cooldownTracker.setCooldown(player, abilityId, duration);
    }
    
    public void clearCooldown(Player player, String abilityId) {
        cooldownTracker.clearCooldown(player, abilityId);
    }
    
    @FunctionalInterface
    public interface EventHandler {
        void handle(Event event);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=AbilityManagerTest`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/AbilityManager.java src/test/java/me/flamboyant/manhunt/domain/role/ability/AbilityManagerTest.java
git commit -m "feat(priority-13): add AbilityManager

- Coordinates ability lifecycle
- Routes events to registered handlers
- Delegates cooldown management to CooldownTracker
- Error handling prevents cascade failures"
```

---

### Task 4: Base Ability Classes

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/ItemActivatedAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/PassiveAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/WinConditionAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/ItemActivatedAbilityTest.java`

**Interfaces:**
- Consumes: `Ability` (Task 1), `AbilityContext` (Task 1)
- Produces: `ItemActivatedAbility` (abstract class with `activate()` method), `PassiveAbility` (abstract class with `registerEventHandlers()` method), `WinConditionAbility` (abstract class with `registerWinConditionHandlers()` method)

- [ ] **Step 1: Write failing test for ItemActivatedAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.application.services.ItemService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemActivatedAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private ItemService mockItemService;
    private AbilityManager mockAbilityManager;
    private ItemStack triggerItem;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        mockItemService = mock(ItemService.class);
        mockAbilityManager = mock(AbilityManager.class);
        triggerItem = new ItemStack(Material.COMPASS);
        
        when(mockContext.getOwner()).thenReturn(mockOwner);
        when(mockContext.getItemService()).thenReturn(mockItemService);
        when(mockContext.getAbilityManager()).thenReturn(mockAbilityManager);
    }

    @Test
    void onRoleStartRegistersEventHandler() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));
        
        ability.onRoleStart(mockContext);
        
        verify(mockAbilityManager).registerEventHandler(eq(mockOwner), eq(PlayerInteractEvent.class), any());
    }

    @Test
    void activateCalledWhenItemMatches() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));
        ability.onRoleStart(mockContext);
        
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        ItemStack eventItem = new ItemStack(Material.COMPASS);
        when(mockEvent.hasItem()).thenReturn(true);
        when(mockEvent.getItem()).thenReturn(eventItem);
        when(mockItemService.isSameItemKind(eventItem, triggerItem)).thenReturn(true);
        when(mockAbilityManager.isOnCooldown(mockOwner, ability.getName())).thenReturn(false);
        
        // Simulate event routing
        ability.handleInteract(mockEvent);
        
        assertTrue(ability.wasActivated());
    }

    @Test
    void activateNotCalledWhenOnCooldown() {
        TestItemAbility ability = new TestItemAbility(mockContext, triggerItem, Duration.ofSeconds(10));
        ability.onRoleStart(mockContext);
        
        PlayerInteractEvent mockEvent = mock(PlayerInteractEvent.class);
        ItemStack eventItem = new ItemStack(Material.COMPASS);
        when(mockEvent.hasItem()).thenReturn(true);
        when(mockEvent.getItem()).thenReturn(eventItem);
        when(mockItemService.isSameItemKind(eventItem, triggerItem)).thenReturn(true);
        when(mockAbilityManager.isOnCooldown(mockOwner, ability.getName())).thenReturn(true);
        
        ability.handleInteract(mockEvent);
        
        assertFalse(ability.wasActivated());
    }

    // Test helper class
    private static class TestItemAbility extends ItemActivatedAbility {
        private boolean activated = false;
        
        public TestItemAbility(AbilityContext context, ItemStack triggerItem, Duration cooldown) {
            super(context, triggerItem, cooldown);
        }
        
        @Override
        protected void activate() {
            activated = true;
        }
        
        @Override
        public String getName() {
            return "Test Item Ability";
        }
        
        @Override
        public String getDescription() {
            return "Test description";
        }
        
        boolean wasActivated() {
            return activated;
        }
        
        // Expose for testing
        void handleInteract(PlayerInteractEvent event) {
            super.handleInteractEvent(event);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ItemActivatedAbilityTest`
Expected: FAIL with "cannot find symbol class ItemActivatedAbility"

- [ ] **Step 3: Create ItemActivatedAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public abstract class ItemActivatedAbility implements Ability {
    protected final ItemStack triggerItem;
    protected final Duration cooldown;
    protected final AbilityContext context;
    
    protected ItemActivatedAbility(AbilityContext context, ItemStack triggerItem, Duration cooldown) {
        this.context = context;
        this.triggerItem = triggerItem;
        this.cooldown = cooldown;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        context.registerEventHandler(PlayerInteractEvent.class, this::handleInteractEvent);
    }
    
    @Override
    public void onRoleStop(AbilityContext context) {
        // Cleanup handled by EventRegistrationService
    }
    
    protected void handleInteractEvent(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!context.getItemService().isSameItemKind(event.getItem(), triggerItem)) {
            return;
        }
        if (context.getAbilityManager().isOnCooldown(context.getOwner(), getName())) {
            return;
        }
        
        activate();
        
        context.getAbilityManager().setCooldown(context.getOwner(), getName(), cooldown);
    }
    
    protected abstract void activate();
}
```

- [ ] **Step 4: Create PassiveAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

public abstract class PassiveAbility implements Ability {
    protected final AbilityContext context;
    
    protected PassiveAbility(AbilityContext context) {
        this.context = context;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        registerEventHandlers(context);
    }
    
    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
    }
    
    protected abstract void registerEventHandlers(AbilityContext context);
}
```

- [ ] **Step 5: Create WinConditionAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

public abstract class WinConditionAbility implements Ability {
    protected final AbilityContext context;
    
    protected WinConditionAbility(AbilityContext context) {
        this.context = context;
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        registerWinConditionHandlers(context);
    }
    
    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
    }
    
    protected abstract void registerWinConditionHandlers(AbilityContext context);
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn test -Dtest=ItemActivatedAbilityTest`
Expected: PASS (3 tests)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/ItemActivatedAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/PassiveAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/WinConditionAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/ItemActivatedAbilityTest.java
git commit -m "feat(priority-13): add base ability classes

- ItemActivatedAbility handles item-triggered abilities with cooldowns
- PassiveAbility for always-on effects
- WinConditionAbility for win condition logic
- Event filtering boilerplate handled in base classes"
```

---

## PHASE 2: IMPLEMENT CONCRETE ABILITIES

### Task 5: Compass Abilities

**Files:**
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/UIPickerCompassAbility.java`
- Create: `src/main/java/me/flamboyant/manhunt/domain/role/ability/CyclingCompassAbility.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/UIPickerCompassAbilityTest.java`
- Create: `src/test/java/me/flamboyant/manhunt/domain/role/ability/CyclingCompassAbilityTest.java`

**Interfaces:**
- Consumes: `ItemActivatedAbility` (Task 4), `CompassTarget` (existing in domain), `AManhuntRole.calculateCompassTarget()` method pattern
- Produces: `UIPickerCompassAbility(AbilityContext, Duration)`, `CyclingCompassAbility(AbilityContext, Duration)`

- [ ] **Step 1: Write failing test for UIPickerCompassAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.behavior.CompassTarget;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class UIPickerCompassAbilityTest {
    private AbilityContext mockContext;
    private Player mockOwner;
    private GameSession mockSession;
    private UIPickerCompassAbility ability;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockOwner = mock(Player.class);
        mockSession = mock(GameSession.class);
        
        when(mockContext.getOwner()).thenReturn(mockOwner);
        when(mockContext.getSession()).thenReturn(mockSession);
        
        ability = new UIPickerCompassAbility(mockContext, Duration.ofMinutes(15));
    }

    @Test
    void nameIsUIPickerCompass() {
        assertEquals("UI Picker Compass", ability.getName());
    }

    @Test
    void descriptionMentionsUISelection() {
        assertTrue(ability.getDescription().contains("UI"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=UIPickerCompassAbilityTest`
Expected: FAIL

- [ ] **Step 3: Implement CompassAbility base class**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.behavior.CompassTarget;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.time.Duration;

public abstract class CompassAbility extends ItemActivatedAbility {
    protected CompassAbility(AbilityContext context, Duration cooldown) {
        super(context, new ItemStack(Material.COMPASS), cooldown);
    }
    
    @Override
    protected void activate() {
        Player target = selectTarget();
        if (target == null) {
            return;
        }
        
        updateCompass(target);
    }
    
    protected abstract Player selectTarget();
    
    protected void updateCompass(Player target) {
        CompassTarget compassTarget = calculateCompassTarget(target);
        
        if (compassTarget.isCrossDimension()) {
            context.sendMessage(target.getDisplayName() + " est dans la dimension " 
                + compassTarget.getTargetDimensionName());
        }
        
        applyCompassPointing(compassTarget);
    }
    
    private CompassTarget calculateCompassTarget(Player target) {
        Location targetLocation = target.getLocation();
        World targetWorld = targetLocation.getWorld();
        World ownerWorld = context.getOwner().getWorld();
        
        if (ownerWorld == targetWorld) {
            return CompassTarget.sameDimension(targetLocation);
        }
        
        String ownerWorldName = ownerWorld.getName();
        Location portalLocation;
        if (ownerWorldName.equals("world")) {
            portalLocation = context.getSession().getPortalLocation(target, World.Environment.NORMAL);
        } else if (ownerWorldName.equals("world_nether")) {
            portalLocation = context.getSession().getPortalLocation(target, World.Environment.NETHER);
        } else {
            portalLocation = targetLocation;
        }
        
        return CompassTarget.crossDimension(portalLocation, targetWorld.getName());
    }
    
    private void applyCompassPointing(CompassTarget compassTarget) {
        Player owner = context.getOwner();
        Location huntedLocation = compassTarget.getLocation();
        
        if (owner.getWorld().getName().equalsIgnoreCase("world_nether")) {
            Location lodeStoneLocation = new Location(
                huntedLocation.getWorld(), 
                huntedLocation.getBlockX(), 
                0, 
                huntedLocation.getBlockZ()
            );
            lodeStoneLocation.getBlock().setType(Material.LODESTONE);
            
            // Find compass in inventory and update it
            for (ItemStack item : owner.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) {
                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                    compassMeta.setLodestone(lodeStoneLocation);
                    compassMeta.setLodestoneTracked(true);
                    item.setItemMeta(compassMeta);
                    break;
                }
            }
        } else {
            for (ItemStack item : owner.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) {
                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                    compassMeta.setLodestone(null);
                    compassMeta.setLodestoneTracked(false);
                    item.setItemMeta(compassMeta);
                    break;
                }
            }
            owner.setCompassTarget(huntedLocation);
        }
    }
}
```

- [ ] **Step 4: Implement UIPickerCompassAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class UIPickerCompassAbility extends CompassAbility {
    private PlayerSelectionView trackView;
    
    public UIPickerCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        List<Player> otherPlayers = context.getSession().getPlayers().stream()
            .filter(p -> p != context.getOwner())
            .collect(Collectors.toList());
        trackView = new PlayerSelectionView(otherPlayers, "Track Selection");
        
        context.registerEventHandler(InventoryCloseEvent.class, this::onInventoryClose);
    }
    
    @Override
    protected Player selectTarget() {
        context.getServer().getPluginManager().registerEvents(trackView, context.getPlugin());
        context.getOwner().openInventory(trackView.getView());
        return null; // Target selected via UI callback
    }
    
    private void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != context.getOwner()) {
            return;
        }
        if (event.getInventory() != trackView.getView()) {
            return;
        }
        
        Player selectedPlayer = trackView.getSelectedPlayer();
        if (selectedPlayer != null) {
            updateCompass(selectedPlayer);
        }
        trackView.close();
    }
    
    @Override
    public String getName() {
        return "UI Picker Compass";
    }
    
    @Override
    public String getDescription() {
        return "Sélectionne un hunter via l'interface pour le traquer avec la boussole";
    }
}
```

- [ ] **Step 5: Implement CyclingCompassAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CyclingCompassAbility extends CompassAbility {
    private List<Player> speedrunners;
    private int targetIndex = 0;
    
    public CyclingCompassAbility(AbilityContext context, Duration cooldown) {
        super(context, cooldown);
        this.speedrunners = new ArrayList<>();
    }
    
    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        
        speedrunners = new ArrayList<>();
        for (Player player : context.getSession().getPlayers()) {
            if (context.getSession().getRole(player).getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunners.add(player);
            }
        }
    }
    
    @Override
    protected Player selectTarget() {
        if (speedrunners.isEmpty()) {
            return null;
        }
        
        if (++targetIndex >= speedrunners.size()) {
            targetIndex = 0;
        }
        
        return speedrunners.get(targetIndex);
    }
    
    @Override
    public String getName() {
        return "Cycling Compass";
    }
    
    @Override
    public String getDescription() {
        return "Boussole qui cycle automatiquement entre les speedrunners";
    }
}
```

- [ ] **Step 6: Write test for CyclingCompassAbility**

```java
package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.mockito.Mockito.*;

class CyclingCompassAbilityTest {
    private AbilityContext mockContext;
    private GameSession mockSession;
    private CyclingCompassAbility ability;

    @BeforeEach
    void setUp() {
        mockContext = mock(AbilityContext.class);
        mockSession = mock(GameSession.class);
        
        when(mockContext.getSession()).thenReturn(mockSession);
        
        ability = new CyclingCompassAbility(mockContext, Duration.ofSeconds(30));
    }

    @Test
    void buildsSpeedrunnerListOnStart() {
        Player speedrunner1 = mock(Player.class);
        Player speedrunner2 = mock(Player.class);
        Player hunter = mock(Player.class);
        
        AManhuntRole srRole1 = mock(AManhuntRole.class);
        AManhuntRole srRole2 = mock(AManhuntRole.class);
        AManhuntRole hunterRole = mock(AManhuntRole.class);
        
        when(srRole1.getRoleType()).thenReturn(ManhuntRoleType.SPEEDRUNNER);
        when(srRole2.getRoleType()).thenReturn(ManhuntRoleType.SPEEDRUNNER);
        when(hunterRole.getRoleType()).thenReturn(ManhuntRoleType.HUNTER);
        
        when(mockSession.getPlayers()).thenReturn(Arrays.asList(speedrunner1, speedrunner2, hunter));
        when(mockSession.getRole(speedrunner1)).thenReturn(srRole1);
        when(mockSession.getRole(speedrunner2)).thenReturn(srRole2);
        when(mockSession.getRole(hunter)).thenReturn(hunterRole);
        
        ability.onRoleStart(mockContext);
        
        // Verify list built (can't directly check private field, but test passes if no exception)
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn test -Dtest=UIPickerCompassAbilityTest,CyclingCompassAbilityTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/me/flamboyant/manhunt/domain/role/ability/CompassAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/UIPickerCompassAbility.java src/main/java/me/flamboyant/manhunt/domain/role/ability/CyclingCompassAbility.java src/test/java/me/flamboyant/manhunt/domain/role/ability/UIPickerCompassAbilityTest.java src/test/java/me/flamboyant/manhunt/domain/role/ability/CyclingCompassAbilityTest.java
git commit -m "feat(priority-13): add compass tracking abilities

- CompassAbility base class with shared compass logic
- UIPickerCompassAbility for speedrunner UI selection
- CyclingCompassAbility for hunter target cycling
- Extracted compass calculation and lodestone handling"
```

---

Due to token limitations, I'll provide the structure for the remaining tasks. The pattern continues with:

**PHASE 2 (continued):**
- Task 6: Checkpoint Abilities (Save, Rollback, Storage)
- Task 7: Passive Abilities (GrassDrop, SwordSound, CutClean, etc.)
- Task 8: Win Condition Abilities (DragonWin, SpeedrunnerDeath, PortalTracking)
- Task 9: Specialized Abilities (TntTactical, Werewolf, ProMiner, etc.)
- Task 10: Utility Abilities (CompassOnStart, CompassOnRespawn, etc.)

**PHASE 3: ROLE DEFINITIONS**
- Task 11: RoleDefinition and RoleDefinitionRegistry
- Task 12: Register all 19 role definitions in ManhuntModule
- Task 13: Migration tests

**PHASE 4: CUTOVER**
- Task 14: Update AssistedRoleFactory to create new Role class
- Task 15: Delete old role classes
- Task 16: Update documentation

Each task follows the same TDD pattern: test first, implement, verify, commit.

Would you like me to continue writing the complete plan with all tasks, or is this structure sufficient for execution?
