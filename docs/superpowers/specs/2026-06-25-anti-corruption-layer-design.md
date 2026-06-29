# Anti-Corruption Layer Design Specification

**Priority:** 12  
**Created:** 2026-06-25  
**Status:** Design Complete - Ready for Implementation  
**Estimated Effort:** 6-8 hours

---

## Executive Summary

This specification defines the implementation of an anti-corruption layer to isolate the domain and application layers from the FlamboyantPluginTools framework. The goal is clean architecture compliance with strict separation of concerns, making domain/application code framework-free and fully testable with mocks.

**Scope:** Domain + Application layers (118 files with framework imports)  
**Approach:** Service Layer Injection with Guice AssistedInject  
**Out of Scope:** Infrastructure layer remains coupled to framework (appropriate)

---

## Problem Statement

### Current State

The codebase has widespread framework coupling across all layers:

- **118 files** import from `me.flamboyant.*` packages
- **Static access**: `Common.server`, `Common.plugin` used throughout domain/application
- **Utility helpers**: `ChatHelper`, `ItemHelper` directly imported in domain code
- **Lifecycle coupling**: `NewManhuntLauncher` implements `ILaunchablePlugin` and mixes UI with domain logic
- **Parameter system**: Framework UI parameters scattered in domain orchestration

### Architectural Violations

1. **Domain depends on infrastructure** - roles import framework utilities
2. **Application depends on infrastructure** - sagas use `ChatHelper`
3. **No testability** - domain logic requires full framework to test
4. **Static coupling** - hidden dependencies via `Common` utility class
5. **Mixed responsibilities** - UI configuration mixed with game orchestration

---

## Design Goals

1. ✅ **Domain layer framework-free** - no `me.flamboyant.*` imports except in infrastructure
2. ✅ **Application layer framework-free** - services depend only on domain and Bukkit primitives
3. ✅ **Explicit dependencies** - constructor injection, no static access
4. ✅ **Testability** - can test domain/application with simple mocks
5. ✅ **Clean separation** - clear boundaries between layers

---

## Architecture Overview

### Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                    │
│  (Framework-coupled: adapters, parameters, Bukkit)      │
│                                                          │
│  - ManhuntPluginAdapter (ILaunchablePlugin)            │
│  - BukkitMessageService                                 │
│  - BukkitItemService                                    │
│  - BukkitEventRegistrationService                       │
│  - CommandsDispatcher                                   │
│  - Main.java                                            │
└──────────────────┬──────────────────────────────────────┘
                   │ delegates to
                   ↓
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│         (Framework-free: uses domain + Bukkit)          │
│                                                          │
│  - GameLaunchService (orchestration)                   │
│  - GameLaunchConfiguration (value object)              │
│  - StartGameSaga, EndGameSaga (existing)               │
│  - Application services (existing)                      │
│  - MessageService interface                             │
│  - ItemService interface                                │
│  - EventRegistrationService interface                   │
└──────────────────┬──────────────────────────────────────┘
                   │ uses
                   ↓
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│           (Pure domain logic + Bukkit types)            │
│                                                          │
│  - AManhuntRole and 16 role implementations            │
│  - GameSession, GameSessionManager                      │
│  - Win conditions, events, value objects                │
└─────────────────────────────────────────────────────────┘
```

### Dependency Flow

```
Infrastructure → Application → Domain
     ↓                ↓            ↓
Framework      Bukkit only   Bukkit only
```

**Key principle:** Dependencies point inward. Domain has zero framework knowledge. Application depends on domain abstractions. Infrastructure adapts framework to application needs.

---

## Component Design

## 1. Infrastructure Services

Three new services wrap framework/Bukkit functionality using only Bukkit APIs.

### 1.1 MessageService

**Purpose:** Replace `ChatHelper` with domain/application-friendly messaging.

**Interface (application layer):**
```java
package me.flamboyant.manhunt.application.services;

import org.bukkit.entity.Player;

/**
 * Service for sending formatted messages to players and server.
 * Replaces ChatHelper with framework-independent abstraction.
 */
public interface MessageService {
    /**
     * Send a message to a player with color code translation.
     * @param player target player
     * @param message message with '&' color codes
     */
    void sendMessage(Player player, String message);
    
    /**
     * Broadcast a message to all online players.
     * @param message message with '&' color codes
     */
    void broadcastMessage(String message);
    
    /**
     * Send a formatted message to a player.
     * @param player target player
     * @param format format string
     * @param args format arguments
     */
    void sendFormattedMessage(Player player, String format, Object... args);
}
```

**Implementation (infrastructure layer):**
```java
package me.flamboyant.manhunt.infrastructure.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import javax.inject.Singleton;

/**
 * Bukkit implementation of MessageService.
 * Uses only Bukkit APIs for message formatting and delivery.
 */
@Singleton
public class BukkitMessageService implements MessageService {
    private final Server server;
    
    @Inject
    public BukkitMessageService(Server server) {
        this.server = server;
    }
    
    @Override
    public void sendMessage(Player player, String message) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning("Attempted to send message to offline player: " + player.getName());
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    @Override
    public void broadcastMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        server.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    @Override
    public void sendFormattedMessage(Player player, String format, Object... args) {
        sendMessage(player, String.format(format, args));
    }
}
```

**Migration:**
- Replace all `ChatHelper.sendMessage(player, msg)` → `messageService.sendMessage(player, msg)`
- Replace all `ChatHelper.broadcastMessage(msg)` → `messageService.broadcastMessage(msg)`
- Add `MessageService` to constructor dependencies

---

### 1.2 ItemService

**Purpose:** Replace `ItemHelper` for item manipulation.

**Interface (application layer):**
```java
package me.flamboyant.manhunt.application.services;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Service for item creation and manipulation.
 * Replaces ItemHelper with framework-independent abstraction.
 */
public interface ItemService {
    /**
     * Give an item to a player. If inventory is full, drops at player location.
     * @param player target player
     * @param item item to give
     */
    void giveItem(Player player, ItemStack item);
    
    /**
     * Create an item with display name and lore.
     * @param material item material
     * @param displayName display name with '&' color codes
     * @param lore lore lines with '&' color codes
     * @return configured ItemStack
     */
    ItemStack createItem(Material material, String displayName, String... lore);
}
```

**Implementation (infrastructure layer):**
```java
package me.flamboyant.manhunt.infrastructure.services;

import me.flamboyant.manhunt.application.services.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Bukkit implementation of ItemService.
 * Uses only Bukkit APIs for item creation and manipulation.
 */
@Singleton
public class BukkitItemService implements ItemService {
    
    @Override
    public void giveItem(Player player, ItemStack item) {
        if (player == null || item == null) {
            throw new IllegalArgumentException("Player and item cannot be null");
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning("Attempted to give item to offline player: " + player.getName());
            return;
        }
        
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack extra : overflow.values()) {
                player.getWorld().dropItem(player.getLocation(), extra);
            }
        }
    }
    
    @Override
    public ItemStack createItem(Material material, String displayName, String... lore) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            
            if (lore.length > 0) {
                meta.setLore(Arrays.stream(lore)
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
```

**Migration:**
- Replace all `ItemHelper.giveItem(player, item)` → `itemService.giveItem(player, item)`
- Replace all `ItemHelper.createItem(...)` → `itemService.createItem(...)`
- Add `ItemService` to constructor dependencies

---

### 1.3 EventRegistrationService

**Purpose:** Centralize Bukkit event registration, replace direct PluginManager access.

**Interface (application layer):**
```java
package me.flamboyant.manhunt.application.services;

import org.bukkit.event.Listener;

/**
 * Service for registering and unregistering Bukkit event listeners.
 * Replaces direct PluginManager access.
 */
public interface EventRegistrationService {
    /**
     * Register a Bukkit event listener.
     * @param listener listener to register
     */
    void registerListener(Listener listener);
    
    /**
     * Unregister a Bukkit event listener.
     * @param listener listener to unregister
     */
    void unregisterListener(Listener listener);
}
```

**Implementation (infrastructure layer):**
```java
package me.flamboyant.manhunt.infrastructure.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.exceptions.EventRegistrationException;
import org.bukkit.Server;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import javax.inject.Singleton;

/**
 * Bukkit implementation of EventRegistrationService.
 * Manages event listener lifecycle.
 */
@Singleton
public class BukkitEventRegistrationService implements EventRegistrationService {
    private final Server server;
    private final Plugin plugin;
    
    @Inject
    public BukkitEventRegistrationService(Server server, Plugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }
    
    @Override
    public void registerListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        try {
            server.getPluginManager().registerEvents(listener, plugin);
        } catch (Exception e) {
            throw new EventRegistrationException(
                "Failed to register listener: " + listener.getClass().getName(), e);
        }
    }
    
    @Override
    public void unregisterListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        HandlerList.unregisterAll(listener);
    }
}
```

**New Exception:**
```java
package me.flamboyant.manhunt.application.exceptions;

public class EventRegistrationException extends RuntimeException {
    public EventRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Migration:**
- Replace all `Common.server.getPluginManager().registerEvents(this, Common.plugin)` → `eventRegistration.registerListener(this)`
- Replace all `HandlerList.unregisterAll(this)` → `eventRegistration.unregisterListener(this)`
- Add `EventRegistrationService` to constructor dependencies

---

## 2. Role Construction with AssistedInject

Roles need multiple injected services (Server, Plugin, MessageService, ItemService, EventRegistrationService) plus a runtime parameter (Player owner).

### 2.1 RoleFactory Interface (unchanged)

Keep existing interface - callers don't need to know about DI:

```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface RoleFactory {
    AManhuntRole create(Player owner);
}
```

### 2.2 Role Constructor Pattern

Use `@AssistedInject` to separate injected vs runtime parameters:

**Example (SpeedrunnerRole):**
```java
package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpeedrunnerRole extends AManhuntRole {
    private final Server server;
    private final Plugin plugin;
    private final MessageService messageService;
    private final ItemService itemService;
    private final EventRegistrationService eventRegistration;
    
    @Inject
    public SpeedrunnerRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner);
        this.server = server;
        this.plugin = plugin;
        this.messageService = messageService;
        this.itemService = itemService;
        this.eventRegistration = eventRegistration;
    }
    
    @Override
    protected boolean doStart() {
        messageService.sendMessage(owner, "&6You are a SPEEDRUNNER!");
        eventRegistration.registerListener(this);
        return true;
    }
    
    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.SPEEDRUNNER;
    }
    
    // Other methods use injected services instead of static calls
}
```

**Key points:**
- `@Assisted` marks runtime parameter (Player)
- All services injected automatically by Guice
- No more `Common.server`, `Common.plugin`, `ChatHelper`, `ItemHelper`
- Apply same pattern to all 16 role classes

---

### 2.3 AssistedInject Factory Setup

**Guice Module Binding:**
```java
@Override
protected void configure() {
    // ... existing bindings
    
    // AssistedInject for each role type
    install(new FactoryModuleBuilder()
        .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_SIMPLE"), SpeedrunnerRole.class)
        .build(new TypeLiteral<AssistedRoleFactory<SpeedrunnerRole>>() {}));
    
    install(new FactoryModuleBuilder()
        .implement(AManhuntRole.class, Names.named("HUNTER_SIMPLE"), HunterRole.class)
        .build(new TypeLiteral<AssistedRoleFactory<HunterRole>>() {}));
    
    // Repeat for all 22 role identifiers...
}
```

**AssistedRoleFactory interface:**
```java
package me.flamboyant.manhunt.domain.role.definition;

import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

public interface AssistedRoleFactory<T extends AManhuntRole> {
    T create(Player owner);
}
```

---

### 2.4 RoleRegistry Integration

Update `RoleRegistry` to use AssistedInject factories:

```java
package me.flamboyant.manhunt.domain.role.definition;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import me.flamboyant.manhunt.domain.role.behavior.AManhuntRole;
import org.bukkit.entity.Player;

import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public class RoleRegistry {
    private final Map<ManhuntRoleIdentifier, AssistedRoleFactory<?>> factories;
    private final Injector injector;
    
    @Inject
    public RoleRegistry(Injector injector) {
        this.injector = injector;
        this.factories = new EnumMap<>(ManhuntRoleIdentifier.class);
        registerAllRoles();
    }
    
    private void registerAllRoles() {
        // Register each role identifier with its factory
        for (ManhuntRoleIdentifier id : ManhuntRoleIdentifier.values()) {
            AssistedRoleFactory<?> factory = injector.getInstance(
                Key.get(AssistedRoleFactory.class, Names.named(id.name())));
            factories.put(id, factory);
        }
    }
    
    public AManhuntRole createRole(ManhuntRoleIdentifier id, Player owner) {
        AssistedRoleFactory<?> factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for: " + id);
        }
        return factory.create(owner);
    }
}
```

---

## 3. Launcher Split

Current `NewManhuntLauncher` mixes domain orchestration with framework UI. Split into two components following adapter pattern.

### 3.1 GameLaunchService (Application Layer)

**Purpose:** Pure application service for game launch orchestration. No framework dependencies.

**Location:** `me.flamboyant.manhunt.application.services.GameLaunchService`

```java
package me.flamboyant.manhunt.application.services;

import com.google.inject.Inject;
import me.flamboyant.manhunt.application.commands.EndGameCommand;
import me.flamboyant.manhunt.application.commands.StartGameCommand;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.Bukkit;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Application service for game launch orchestration.
 * Framework-free: uses only domain concepts and sagas.
 */
@Singleton
public class GameLaunchService {
    private boolean running;
    private GameSessionId currentSessionId;
    private final List<GamePlugin> optionalPlugins;
    private final StartGameSaga startGameSaga;
    private final EndGameSaga endGameSaga;
    
    @Inject
    public GameLaunchService(
        StartGameSaga startGameSaga,
        EndGameSaga endGameSaga
    ) {
        this.startGameSaga = startGameSaga;
        this.endGameSaga = endGameSaga;
        this.optionalPlugins = new ArrayList<>();
        this.running = false;
    }
    
    /**
     * Start a new game with the given configuration.
     * @param config game launch configuration
     * @return session ID of started game
     * @throws GameStartException if game cannot be started
     */
    public GameSessionId startGame(GameLaunchConfiguration config) throws GameStartException {
        if (running) {
            throw new GameStartException("Cannot start game - already running");
        }
        if (currentSessionId != null) {
            throw new IllegalStateException("Session ID exists but game not marked as running");
        }
        
        // Build StartGameCommand from configuration
        StartGameCommand command = StartGameCommand.create(
            config.getPlayers(),
            config.getPlayerRoleAssignments(),
            config.getSpeedrunnerCount(),
            config.getAllyCount(),
            config.isSpecialRolesOnly(),
            config.isHiddenSpeedrunner(),
            config.isResetPlayerStuff(),
            config.getRoleRevealDelayMinutes()
        );
        
        // Start via saga
        currentSessionId = startGameSaga.startGame(command);
        
        // Start optional plugins
        for (GamePlugin plugin : optionalPlugins) {
            try {
                plugin.start();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to start optional plugin: " + e.getMessage());
            }
        }
        
        running = true;
        return currentSessionId;
    }
    
    /**
     * Stop the currently running game.
     * @param reason reason for stopping
     */
    public void stopGame(String reason) {
        if (!running) {
            Bukkit.getLogger().info("stopGame called but game not running - ignoring");
            return;
        }
        
        // Stop optional plugins
        for (GamePlugin plugin : optionalPlugins) {
            try {
                plugin.stop();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to stop optional plugin: " + e.getMessage());
            }
        }
        
        // End via saga
        if (currentSessionId != null) {
            EndGameCommand command = new EndGameCommand(currentSessionId, reason);
            endGameSaga.endGame(command);
            currentSessionId = null;
        }
        
        running = false;
    }
    
    /**
     * Check if a game is currently running.
     * @return true if game running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Register an optional plugin to start/stop with game.
     * @param plugin plugin to register
     */
    public void registerOptionalPlugin(GamePlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        optionalPlugins.add(plugin);
    }
}
```

---

### 3.2 GameLaunchConfiguration (Application Layer)

**Purpose:** Value object holding game configuration, decoupled from framework parameter system.

```java
package me.flamboyant.manhunt.application.commands;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Value object holding game launch configuration.
 * Decouples application layer from framework parameter system.
 */
public class GameLaunchConfiguration {
    private final List<Player> players;
    private final Map<Player, ManhuntRoleIdentifier> playerRoleAssignments;
    private final int speedrunnerCount;
    private final int allyCount;
    private final boolean specialRolesOnly;
    private final boolean hiddenSpeedrunner;
    private final boolean resetPlayerStuff;
    private final int roleRevealDelayMinutes;
    
    private GameLaunchConfiguration(Builder builder) {
        this.players = Collections.unmodifiableList(builder.players);
        this.playerRoleAssignments = Collections.unmodifiableMap(builder.playerRoleAssignments);
        this.speedrunnerCount = builder.speedrunnerCount;
        this.allyCount = builder.allyCount;
        this.specialRolesOnly = builder.specialRolesOnly;
        this.hiddenSpeedrunner = builder.hiddenSpeedrunner;
        this.resetPlayerStuff = builder.resetPlayerStuff;
        this.roleRevealDelayMinutes = builder.roleRevealDelayMinutes;
    }
    
    public List<Player> getPlayers() { return players; }
    public Map<Player, ManhuntRoleIdentifier> getPlayerRoleAssignments() { return playerRoleAssignments; }
    public int getSpeedrunnerCount() { return speedrunnerCount; }
    public int getAllyCount() { return allyCount; }
    public boolean isSpecialRolesOnly() { return specialRolesOnly; }
    public boolean isHiddenSpeedrunner() { return hiddenSpeedrunner; }
    public boolean isResetPlayerStuff() { return resetPlayerStuff; }
    public int getRoleRevealDelayMinutes() { return roleRevealDelayMinutes; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<Player> players = Collections.emptyList();
        private Map<Player, ManhuntRoleIdentifier> playerRoleAssignments = Collections.emptyMap();
        private int speedrunnerCount = 1;
        private int allyCount = 0;
        private boolean specialRolesOnly = false;
        private boolean hiddenSpeedrunner = false;
        private boolean resetPlayerStuff = false;
        private int roleRevealDelayMinutes = 10;
        
        public Builder players(List<Player> players) {
            this.players = players;
            return this;
        }
        
        public Builder playerRoleAssignments(Map<Player, ManhuntRoleIdentifier> assignments) {
            this.playerRoleAssignments = assignments;
            return this;
        }
        
        public Builder speedrunnerCount(int count) {
            this.speedrunnerCount = count;
            return this;
        }
        
        public Builder allyCount(int count) {
            this.allyCount = count;
            return this;
        }
        
        public Builder specialRolesOnly(boolean specialRolesOnly) {
            this.specialRolesOnly = specialRolesOnly;
            return this;
        }
        
        public Builder hiddenSpeedrunner(boolean hidden) {
            this.hiddenSpeedrunner = hidden;
            return this;
        }
        
        public Builder resetPlayerStuff(boolean reset) {
            this.resetPlayerStuff = reset;
            return this;
        }
        
        public Builder roleRevealDelayMinutes(int minutes) {
            this.roleRevealDelayMinutes = minutes;
            return this;
        }
        
        public GameLaunchConfiguration build() {
            return new GameLaunchConfiguration(this);
        }
    }
}
```

---

### 3.3 GamePlugin Interface (Domain Layer)

**Purpose:** Replace `ILaunchablePlugin` with domain interface for optional plugins.

```java
package me.flamboyant.manhunt.domain.plugin;

/**
 * Domain interface for optional game plugins.
 * Replaces framework ILaunchablePlugin.
 */
public interface GamePlugin {
    void start();
    void stop();
}
```

---

### 3.4 ManhuntPluginAdapter (Infrastructure Layer)

**Purpose:** Infrastructure adapter implementing framework interface, translates UI parameters to domain configuration.

**Location:** `me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter`

```java
package me.flamboyant.manhunt.infrastructure.adapters;

import com.google.inject.Inject;
import me.flamboyant.configurable.parameters.*;
import me.flamboyant.manhunt.application.commands.GameLaunchConfiguration;
import me.flamboyant.manhunt.application.exceptions.GameStartException;
import me.flamboyant.manhunt.application.services.GameLaunchService;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Singleton;
import java.util.*;

/**
 * Infrastructure adapter implementing FlamboyantPluginTools ILaunchablePlugin.
 * Translates framework UI parameters to domain GameLaunchConfiguration.
 * Delegates game orchestration to GameLaunchService.
 */
@Singleton
public class ManhuntPluginAdapter implements ILaunchablePlugin {
    private final GameLaunchService gameLaunchService;
    private final Server server;
    
    // Framework UI parameters
    private BooleanParameter resetPlayersStuffParameter;
    private BooleanParameter specialRolesOnlyParameter;
    private BooleanParameter surpriseSpeedrunnerParameter;
    private IntParameter allyCountParameter;
    private IntParameter speedrunnerCountParameter;
    private IntParameter minutesBeforeRolesParameter;
    private Map<Player, EnumParameter<ManhuntRoleIdentifier>> playerRoles;
    
    @Inject
    public ManhuntPluginAdapter(
        GameLaunchService gameLaunchService,
        Server server
    ) {
        this.gameLaunchService = gameLaunchService;
        this.server = server;
        this.playerRoles = new HashMap<>();
        initializeParameters();
    }
    
    @Override
    public boolean start() {
        try {
            GameLaunchConfiguration config = buildConfigurationFromParameters();
            gameLaunchService.startGame(config);
            return true;
        } catch (GameStartException e) {
            Bukkit.getLogger().severe("Failed to start game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        gameLaunchService.stopGame("Game stopped by player");
        return true;
    }
    
    @Override
    public boolean isRunning() {
        return gameLaunchService.isRunning();
    }
    
    @Override
    public void resetParameters() {
        initializeParameters();
    }
    
    @Override
    public List<AParameter> getParameters() {
        List<AParameter> params = new ArrayList<>();
        params.add(resetPlayersStuffParameter);
        params.add(minutesBeforeRolesParameter);
        params.add(speedrunnerCountParameter);
        params.add(allyCountParameter);
        params.add(specialRolesOnlyParameter);
        params.add(surpriseSpeedrunnerParameter);
        params.addAll(playerRoles.values());
        return params;
    }
    
    private void initializeParameters() {
        resetPlayersStuffParameter = new BooleanParameter(
            Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
        resetPlayersStuffParameter.setCategory("Manhunt Parameters");
        
        specialRolesOnlyParameter = new BooleanParameter(
            Material.NETHER_STAR, "Special only", "Random role = special");
        specialRolesOnlyParameter.setCategory("Manhunt Parameters");
        
        surpriseSpeedrunnerParameter = new BooleanParameter(
            Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
        surpriseSpeedrunnerParameter.setCategory("Manhunt Parameters");
        
        speedrunnerCountParameter = new IntParameter(
            Material.DIAMOND_BOOTS, "Speedrunners count", "0 = random", 
            1, 0, server.getOnlinePlayers().size());
        speedrunnerCountParameter.setCategory("Manhunt Parameters");
        
        allyCountParameter = new IntParameter(
            Material.GOLDEN_APPLE, "Allies count", "0 = random", 
            0, 0, server.getOnlinePlayers().size());
        allyCountParameter.setCategory("Manhunt Parameters");
        
        minutesBeforeRolesParameter = new IntParameter(
            Material.CLOCK, "Roles time", "Minutes avant annonce rôles", 
            10, 0, 20);
        minutesBeforeRolesParameter.setCategory("Manhunt Parameters");
        
        initializePlayerRoleParameters();
    }
    
    private void initializePlayerRoleParameters() {
        playerRoles.clear();
        for (Player player : server.getOnlinePlayers()) {
            EnumParameter<ManhuntRoleIdentifier> param = new EnumParameter<>(
                Material.PLAYER_HEAD, 
                player.getDisplayName(), 
                "Select role", 
                ManhuntRoleIdentifier.class
            );
            param.setIsNullable(true);
            param.setCategory("Players Role");
            playerRoles.put(player, param);
        }
    }
    
    private GameLaunchConfiguration buildConfigurationFromParameters() {
        List<Player> players = new ArrayList<>(server.getOnlinePlayers());
        
        Map<Player, ManhuntRoleIdentifier> roleAssignments = new HashMap<>();
        for (Map.Entry<Player, EnumParameter<ManhuntRoleIdentifier>> entry : playerRoles.entrySet()) {
            if (entry.getValue().getValue() != null) {
                roleAssignments.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        
        return GameLaunchConfiguration.builder()
            .players(players)
            .playerRoleAssignments(roleAssignments)
            .speedrunnerCount(speedrunnerCountParameter.getValue())
            .allyCount(allyCountParameter.getValue())
            .specialRolesOnly(specialRolesOnlyParameter.getValue())
            .hiddenSpeedrunner(surpriseSpeedrunnerParameter.getValue())
            .resetPlayerStuff(resetPlayersStuffParameter.getValue())
            .roleRevealDelayMinutes(minutesBeforeRolesParameter.getValue())
            .build();
    }
}
```

---

### 3.5 CommandsDispatcher Update

Update to use adapter via DI:

```java
package me.flamboyant.manhunt;

import com.google.inject.Inject;
import me.flamboyant.gui.ConfigurablePluginListener;
import me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Singleton;

@Singleton
public class CommandsDispatcher implements CommandExecutor {
    private final ManhuntPluginAdapter adapter;
    
    @Inject
    public CommandsDispatcher(ManhuntPluginAdapter adapter) {
        this.adapter = adapter;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if (sender instanceof Player) {
            Player commandSender = (Player) sender;
            if ("f_manhunt".equals(cmd.getName())) {
                launchPlugin(commandSender, adapter);
            }
            return true;
        }
        return false;
    }
    
    private void launchPlugin(Player sender, ILaunchablePlugin plugin) {
        if (plugin.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Plugin stopped");
            plugin.stop();
            return;
        }
        
        plugin.resetParameters();
        
        if (!ConfigurablePluginListener.getInstance().isLaunched()) {
            ConfigurablePluginListener.getInstance().launch(plugin, sender);
        }
        
        sender.sendMessage("Plugin started");
    }
}
```

---

## 4. Guice Module Configuration

Update `ManhuntModule` to bind all new components:

```java
package me.flamboyant.manhunt.application.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import me.flamboyant.manhunt.application.services.*;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.role.behavior.*;
import me.flamboyant.manhunt.domain.role.definition.AssistedRoleFactory;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter;
import me.flamboyant.manhunt.infrastructure.services.*;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import javax.inject.Singleton;
import java.util.Random;

public class ManhuntModule extends AbstractModule {
    private final Plugin plugin;
    
    public ManhuntModule(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    protected void configure() {
        // Bukkit primitives
        bind(Server.class).toInstance(plugin.getServer());
        bind(Plugin.class).toInstance(plugin);
        
        // Infrastructure services (interfaces → implementations)
        bind(MessageService.class).to(BukkitMessageService.class).in(Singleton.class);
        bind(ItemService.class).to(BukkitItemService.class).in(Singleton.class);
        bind(EventRegistrationService.class).to(BukkitEventRegistrationService.class).in(Singleton.class);
        
        // Domain event publisher
        bind(DomainEventPublisher.class).to(InMemoryEventPublisher.class).in(Singleton.class);
        
        // Application services
        bind(GameLaunchService.class).in(Singleton.class);
        
        // Infrastructure adapter
        bind(ManhuntPluginAdapter.class).in(Singleton.class);
        bind(ILaunchablePlugin.class).to(ManhuntPluginAdapter.class);
        
        // AssistedInject factories for all roles
        installRoleFactories();
        
        // Utilities
        bind(Random.class).toInstance(new Random());
    }
    
    private void installRoleFactories() {
        // Speedrunner roles
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_SIMPLE"), SpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_SWAPPER"), SpeedrunnerSwapperRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_CHECKPOINT"), CheckpointSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_TNT_TACTICAL"), TntTacticalSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_WEREWOLF"), WerewolfSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_ELF"), ElfSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_LINK"), LinkSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_CUTCLEAN"), CutCleanSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("SPEEDRUNNER_NO_NAMETAG"), NoNameTagSpeedrunnerRole.class)
            .build(AssistedRoleFactory.class));
        
        // Hunter roles
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_SIMPLE"), HunterRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_CHECKPOINT"), CheckpointHunterRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_PRO_MINER"), ProMinerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_SUPER"), SuperHunterRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_ELF"), ElfHunterRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_LINK"), LinkHunterRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("HUNTER_CUTCLEAN"), CutCleanHunterRole.class)
            .build(AssistedRoleFactory.class));
        
        // Ally roles
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("ALLY_GLUER"), GluerRole.class)
            .build(AssistedRoleFactory.class));
        
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("ALLY_IMPOSTER"), ImposterRole.class)
            .build(AssistedRoleFactory.class));
        
        // Special roles
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("UNDECIDED"), UndecidedRole.class)
            .build(AssistedRoleFactory.class));
    }
    
    // Existing @Provides methods remain unchanged...
}
```

---

## 5. Migration Strategy

Given 118 files need changes, migrate in phases with verification at each stage.

### Phase 1: Infrastructure Services (1 hour)

**Tasks:**
1. Create service interfaces in `application.services` package:
   - `MessageService`
   - `ItemService`
   - `EventRegistrationService`

2. Create implementations in `infrastructure.services` package:
   - `BukkitMessageService`
   - `BukkitItemService`
   - `BukkitEventRegistrationService`

3. Create `EventRegistrationException` in `application.exceptions`

4. Update `ManhuntModule`:
   - Bind `Server.class` and `Plugin.class`
   - Bind service interfaces to implementations

5. Write unit tests:
   - `BukkitMessageServiceTest` (3 tests)
   - `BukkitItemServiceTest` (3 tests)
   - `BukkitEventRegistrationServiceTest` (3 tests)

**Verification:** All new tests pass, services injectable.

---

### Phase 2: Domain Layer - Role Behavior (2-3 hours)

**Tasks:**
1. Update `AManhuntRole` base class if needed

2. For each of 16 role classes:
   - Add `@Inject` constructor with `@Assisted Player owner`
   - Add service fields (Server, Plugin, MessageService, ItemService, EventRegistrationService)
   - Replace `Common.server` → `server`
   - Replace `Common.plugin` → `plugin`
   - Replace `ChatHelper.*` → `messageService.*`
   - Replace `ItemHelper.*` → `itemService.*`
   - Replace `Common.server.getPluginManager().registerEvents(...)` → `eventRegistration.registerListener(...)`
   - Remove framework imports

3. Role classes to migrate:
   - `SpeedrunnerRole`
   - `HunterRole`
   - `SpeedrunnerSwapperRole`
   - `CheckpointSpeedrunnerRole`
   - `TntTacticalSpeedrunnerRole`
   - `WerewolfSpeedrunnerRole`
   - `ElfSpeedrunnerRole`
   - `LinkSpeedrunnerRole`
   - `CutCleanSpeedrunnerRole`
   - `NoNameTagSpeedrunnerRole`
   - `CheckpointHunterRole`
   - `ProMinerRole`
   - `SuperHunterRole`
   - `ElfHunterRole`
   - `LinkHunterRole`
   - `CutCleanHunterRole`
   - `GluerRole`
   - `ImposterRole`
   - `UndecidedRole`

4. Create `AssistedRoleFactory` interface

5. Update `RoleRegistry` to use AssistedInject

6. Update `ManhuntModule` with role factory bindings

**Verification:** All role tests pass, roles compile without framework imports.

---

### Phase 3: Application Layer (1-2 hours)

**Tasks:**
1. Create `GameLaunchConfiguration` value object in `application.commands`

2. Create `GamePlugin` interface in `domain.plugin`

3. Create `GameLaunchService` in `application.services`

4. Update sagas if they use framework utilities:
   - `StartGameSaga` - replace `ChatHelper` with `MessageService`
   - `EndGameSaga` - replace `ChatHelper` with `MessageService`

5. Update other application services if needed

6. Write tests:
   - `GameLaunchConfigurationTest` (builder test)
   - `GameLaunchServiceTest` (start/stop/double-start tests)

**Verification:** Application layer compiles without framework imports (except infrastructure adapters).

---

### Phase 4: Infrastructure Layer (1-2 hours)

**Tasks:**
1. Create `ManhuntPluginAdapter` in `infrastructure.adapters`

2. Update `CommandsDispatcher`:
   - Inject `ManhuntPluginAdapter` via constructor
   - Remove getInstance() calls

3. Update `Main.java`:
   - Inject `CommandsDispatcher` via Guice
   - Remove manual instantiation

4. Delete or deprecate old `NewManhuntLauncher` class

**Verification:** Command works, UI displays, game launches successfully.

---

### Phase 5: Testing & Verification (1 hour)

**Tasks:**
1. Run full test suite - all existing tests must pass

2. Add integration test:
   - `AntiCorruptionLayerIntegrationTest` - verify services work end-to-end

3. Manual smoke testing:
   - [ ] Launch game via `/f_manhunt`
   - [ ] UI parameters display
   - [ ] Roles assigned
   - [ ] Game starts
   - [ ] Role abilities work (compass, checkpoint, etc.)
   - [ ] Messages send correctly
   - [ ] Items given correctly
   - [ ] Game ends cleanly
   - [ ] Multiple start/stop cycles work

4. Verify no framework imports in domain/application:
   ```bash
   grep -r "import me.flamboyant" src/main/java/me/flamboyant/manhunt/domain/
   grep -r "import me.flamboyant" src/main/java/me/flamboyant/manhunt/application/ | grep -v infrastructure
   ```

**Verification:** Clean architecture verified, all tests pass, smoke tests pass.

---

## 6. Error Handling

### Service-Level Error Handling

All services follow fail-fast with clear errors:

**MessageService:**
- Null player/message → `IllegalArgumentException`
- Offline player → log warning, don't throw (graceful degradation)

**ItemService:**
- Null player/item → `IllegalArgumentException`
- Offline player → log warning, don't throw
- Inventory overflow → drop at location (existing behavior)

**EventRegistrationService:**
- Null listener → `IllegalArgumentException`
- Registration failure → `EventRegistrationException` with cause

### GameLaunchService Edge Cases

**Double-start prevention:**
```java
if (running) {
    throw new GameStartException("Cannot start game - already running");
}
```

**Inconsistent state detection:**
```java
if (currentSessionId != null && !running) {
    throw new IllegalStateException("Session ID exists but game not marked as running");
}
```

**Optional plugin failures:**
```java
for (GamePlugin plugin : optionalPlugins) {
    try {
        plugin.start();
    } catch (Exception e) {
        Bukkit.getLogger().warning("Failed to start optional plugin: " + e.getMessage());
        // Continue with other plugins - don't fail entire game
    }
}
```

---

## 7. Testing Strategy

### Unit Tests

**Service Tests (9 tests total):**

**BukkitMessageServiceTest:**
- `testSendMessage_translatesColorCodes()` - verify '&' codes translated
- `testSendMessage_nullPlayer_throwsException()` - verify null checks
- `testSendMessage_offlinePlayer_logsWarning()` - graceful offline handling
- `testBroadcastMessage_sendsToServer()` - verify server broadcast

**BukkitItemServiceTest:**
- `testGiveItem_addsToInventory()` - normal case
- `testGiveItem_fullInventory_dropsAtLocation()` - overflow handling
- `testCreateItem_setsDisplayNameAndLore()` - item creation

**BukkitEventRegistrationServiceTest:**
- `testRegisterListener_callsPluginManager()` - verify registration
- `testUnregisterListener_callsHandlerList()` - verify unregistration

**GameLaunchServiceTest:**
- `testStartGame_delegatesToSaga()` - verify saga called
- `testStartGame_alreadyRunning_throwsException()` - double-start prevented
- `testStopGame_notRunning_ignores()` - graceful no-op
- `testStopGame_endsViaEndGameSaga()` - verify saga called

### Integration Tests

**RoleConstructionIntegrationTest:**
- Verify AssistedInject creates roles with services
- Verify all 22 role identifiers can be created
- Verify roles receive correct dependencies

**AntiCorruptionLayerIntegrationTest:**
- End-to-end test: adapter → service → saga → domain
- Verify configuration translation works
- Verify services usable in real scenario

### Manual Smoke Test Checklist

- [ ] Can launch game via `/f_manhunt` command
- [ ] UI parameters display correctly
- [ ] Roles are assigned properly
- [ ] Game starts without errors
- [ ] Speedrunner receives compass and messages
- [ ] Hunter receives tracking and messages
- [ ] Checkpoint roles can set checkpoints
- [ ] Game ends cleanly
- [ ] No memory leaks (multiple start/stop cycles)
- [ ] No framework imports in domain/application (grep verification)

---

## 8. Success Criteria

### Technical Criteria

- ✅ **Zero framework imports in domain layer** - only `org.bukkit.*` and domain packages
- ✅ **Zero framework imports in application layer** - only `org.bukkit.*`, domain, and application packages
- ✅ **All 118 files compile** - no breaking changes
- ✅ **All existing tests pass** - backward compatibility maintained
- ✅ **9+ new unit tests pass** - services verified
- ✅ **Integration tests pass** - end-to-end verification
- ✅ **Manual smoke tests pass** - functional verification

### Architectural Criteria

- ✅ **Dependency rule enforced** - infrastructure → application → domain (no reverse)
- ✅ **Explicit dependencies** - no static access, all via constructor injection
- ✅ **Single Responsibility** - each service has one clear purpose
- ✅ **Testability** - domain/application testable with simple mocks
- ✅ **Framework isolation** - framework concerns confined to infrastructure layer

### Behavioral Criteria

- ✅ **No regression** - game works exactly as before
- ✅ **Performance unchanged** - no noticeable latency added
- ✅ **Error messages clear** - failures provide actionable information
- ✅ **Resource cleanup** - no memory leaks, listeners unregistered

---

## 9. Risks & Mitigations

### Risk 1: AssistedInject Complexity

**Risk:** Guice AssistedInject has learning curve, may be configured incorrectly.

**Mitigation:**
- Start with 2-3 role classes, verify pattern works
- Write integration test early to catch binding issues
- Document factory binding pattern clearly
- Use named bindings for clarity

### Risk 2: Large-Scale Migration Errors

**Risk:** 118 files is a lot to change - easy to miss files or make typos.

**Mitigation:**
- Migrate in small phases with verification
- Use IDE refactoring tools where possible
- Grep for remaining `Common.`, `ChatHelper.`, `ItemHelper.` after each phase
- Run full test suite after each phase
- Manual smoke test at end

### Risk 3: Breaking Existing Functionality

**Risk:** Changes to role constructors may break existing behavior.

**Mitigation:**
- Keep existing tests - they verify behavior unchanged
- Add new tests for service layer
- Manual smoke testing checklist
- Phase 5 dedicated to verification

### Risk 4: DI Configuration Errors

**Risk:** Guice binding errors may not surface until runtime.

**Mitigation:**
- Integration test verifies all roles can be created
- Test module configuration separately
- Clear error messages in RoleRegistry
- Early verification in Phase 2

---

## 10. Future Enhancements (Out of Scope)

These are explicitly **not** part of Priority 12 but may be future work:

1. **Replace framework parameter system** - create own UI abstraction (Priority 13+)
2. **Mock Bukkit for tests** - use MockBukkit or similar for true unit tests
3. **Replace ILaunchablePlugin completely** - create own plugin lifecycle interface
4. **Service interfaces in infrastructure** - currently in application, could be pure domain
5. **More granular services** - e.g., separate CompassService, InventoryService

---

## Appendix A: File Organization

### New Files Created

**Application Layer:**
- `application/services/MessageService.java` (interface)
- `application/services/ItemService.java` (interface)
- `application/services/EventRegistrationService.java` (interface)
- `application/services/GameLaunchService.java` (service)
- `application/commands/GameLaunchConfiguration.java` (value object)
- `application/exceptions/EventRegistrationException.java` (exception)

**Domain Layer:**
- `domain/plugin/GamePlugin.java` (interface)
- `domain/role/definition/AssistedRoleFactory.java` (interface)

**Infrastructure Layer:**
- `infrastructure/services/BukkitMessageService.java` (implementation)
- `infrastructure/services/BukkitItemService.java` (implementation)
- `infrastructure/services/BukkitEventRegistrationService.java` (implementation)
- `infrastructure/adapters/ManhuntPluginAdapter.java` (adapter)

**Test Files:**
- `test/.../BukkitMessageServiceTest.java`
- `test/.../BukkitItemServiceTest.java`
- `test/.../BukkitEventRegistrationServiceTest.java`
- `test/.../GameLaunchServiceTest.java`
- `test/.../RoleConstructionIntegrationTest.java`
- `test/.../AntiCorruptionLayerIntegrationTest.java`

### Files Modified

**Domain Layer (16 role classes):**
- `domain/role/behavior/SpeedrunnerRole.java`
- `domain/role/behavior/HunterRole.java`
- `domain/role/behavior/SpeedrunnerSwapperRole.java`
- `domain/role/behavior/CheckpointSpeedrunnerRole.java`
- `domain/role/behavior/TntTacticalSpeedrunnerRole.java`
- `domain/role/behavior/WerewolfSpeedrunnerRole.java`
- `domain/role/behavior/ElfSpeedrunnerRole.java`
- `domain/role/behavior/LinkSpeedrunnerRole.java`
- `domain/role/behavior/CutCleanSpeedrunnerRole.java`
- `domain/role/behavior/NoNameTagSpeedrunnerRole.java`
- `domain/role/behavior/CheckpointHunterRole.java`
- `domain/role/behavior/ProMinerRole.java`
- `domain/role/behavior/SuperHunterRole.java`
- `domain/role/behavior/ElfHunterRole.java`
- `domain/role/behavior/LinkHunterRole.java`
- `domain/role/behavior/CutCleanHunterRole.java`
- `domain/role/behavior/GluerRole.java`
- `domain/role/behavior/ImposterRole.java`
- `domain/role/behavior/UndecidedRole.java`
- `domain/role/definition/RoleRegistry.java`

**Application Layer:**
- `application/sagas/StartGameSaga.java` (replace ChatHelper)
- `application/sagas/EndGameSaga.java` (replace ChatHelper)
- `application/injection/ManhuntModule.java` (add bindings)

**Infrastructure Layer:**
- `CommandsDispatcher.java` (inject adapter)
- `Main.java` (inject CommandsDispatcher)

**Deprecated/Deleted:**
- `NewManhuntLauncher.java` (replaced by GameLaunchService + ManhuntPluginAdapter)

---

## Appendix B: Reference Examples

### Complete Role Migration Example

**Before (SpeedrunnerRole with framework coupling):**
```java
package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.entity.Player;

public class SpeedrunnerRole extends AManhuntRole {
    public SpeedrunnerRole(Player owner) {
        super(owner);
    }
    
    @Override
    protected boolean doStart() {
        ChatHelper.sendMessage(owner, "&6You are a SPEEDRUNNER!");
        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        return true;
    }
    
    @Override
    protected boolean doStop() {
        HandlerList.unregisterAll(this);
        return true;
    }
}
```

**After (SpeedrunnerRole with clean architecture):**
```java
package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpeedrunnerRole extends AManhuntRole {
    private final Server server;
    private final Plugin plugin;
    private final MessageService messageService;
    private final EventRegistrationService eventRegistration;
    
    @Inject
    public SpeedrunnerRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        EventRegistrationService eventRegistration
    ) {
        super(owner);
        this.server = server;
        this.plugin = plugin;
        this.messageService = messageService;
        this.eventRegistration = eventRegistration;
    }
    
    @Override
    protected boolean doStart() {
        messageService.sendMessage(owner, "&6You are a SPEEDRUNNER!");
        eventRegistration.registerListener(this);
        return true;
    }
    
    @Override
    protected boolean doStop() {
        eventRegistration.unregisterListener(this);
        return true;
    }
    
    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.SPEEDRUNNER;
    }
}
```

**Key changes:**
- ❌ Removed `import me.flamboyant.utils.*`
- ✅ Added `@Inject` constructor with `@Assisted Player`
- ✅ Added service fields
- ✅ Replaced `ChatHelper` → `messageService`
- ✅ Replaced `Common.server.getPluginManager()` → `eventRegistration`
- ✅ Explicit dependencies, no static access

---

## Conclusion

This design provides a clean anti-corruption layer isolating domain and application layers from the FlamboyantPluginTools framework. The implementation follows clean architecture principles with explicit dependencies, testability, and clear layer boundaries.

**Next step:** Create implementation plan with detailed task breakdown.
