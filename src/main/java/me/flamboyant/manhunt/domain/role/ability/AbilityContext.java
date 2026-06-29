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
