package me.flamboyant.manhunt.domain.role.ability;

import me.flamboyant.manhunt.domain.services.SessionRepository;
import me.flamboyant.manhunt.domain.services.MessagingPort;
import me.flamboyant.manhunt.domain.services.ItemPort;
import me.flamboyant.manhunt.domain.services.EventRegistrationPort;
import me.flamboyant.manhunt.domain.game.GameSession;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class AbilityContext {
    private final Player owner;
    private final SessionRepository sessionRepository;
    private final MessagingPort messagingPort;
    private final ItemPort itemPort;
    private final EventRegistrationPort eventRegistrationPort;
    private final Server server;
    private final Plugin plugin;
    private final AbilityManager abilityManager;
    private GameSession session;

    public AbilityContext(
        Player owner,
        SessionRepository sessionRepository,
        MessagingPort messagingPort,
        ItemPort itemPort,
        EventRegistrationPort eventRegistrationPort,
        Server server,
        Plugin plugin,
        AbilityManager abilityManager
    ) {
        this.owner = owner;
        this.sessionRepository = sessionRepository;
        this.messagingPort = messagingPort;
        this.itemPort = itemPort;
        this.eventRegistrationPort = eventRegistrationPort;
        this.server = server;
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    public Player getOwner() {
        return owner;
    }

    public GameSession getSession() {
        if (session == null) {
            session = sessionRepository.getActiveSessionForPlayer(owner);
        }
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public SessionRepository getSessionRepository() {
        return sessionRepository;
    }

    public MessagingPort getMessagingPort() {
        return messagingPort;
    }

    public ItemPort getItemPort() {
        return itemPort;
    }

    /**
     * Temporary backward compatibility - abilities still use full ItemService interface.
     * TODO: Refactor abilities to use only ItemPort methods.
     */
    @Deprecated
    public me.flamboyant.manhunt.application.services.ItemService getItemService() {
        // ItemPort is bound to ItemService implementation, so this cast is safe
        return (me.flamboyant.manhunt.application.services.ItemService) itemPort;
    }

    /**
     * Temporary backward compatibility - abilities still use full MessageService interface.
     * TODO: Refactor abilities to use only MessagingPort methods.
     */
    @Deprecated
    public me.flamboyant.manhunt.application.services.MessageService getMessageService() {
        // MessagingPort is bound to MessageService implementation, so this cast is safe
        return (me.flamboyant.manhunt.application.services.MessageService) messagingPort;
    }

    public EventRegistrationPort getEventRegistrationPort() {
        return eventRegistrationPort;
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
        messagingPort.sendMessage(owner, message);
    }

    public <T extends Event> void registerEventHandler(Class<T> eventType, Consumer<T> handler) {
        abilityManager.registerEventHandler(owner, eventType, event -> handler.accept((T) event));
    }

    /**
     * Register a Bukkit Listener for this ability context.
     * Delegates to EventRegistrationPort (application layer).
     */
    public void registerListener(org.bukkit.event.Listener listener) {
        eventRegistrationPort.registerEvents(listener, plugin);
    }
}
