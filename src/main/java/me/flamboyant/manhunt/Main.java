package me.flamboyant.manhunt;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.flamboyant.FlamboyantPlugin;
import me.flamboyant.manhunt.application.injection.ManhuntModule;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.GameEndedEvent;
import me.flamboyant.manhunt.domain.event.GameSessionCreatedEvent;
import me.flamboyant.manhunt.domain.event.HandlersRegisteredEvent;
import me.flamboyant.manhunt.domain.event.RolesAssignedEvent;
import me.flamboyant.manhunt.domain.event.RolesDistributedEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends FlamboyantPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        super.onEnable();

        // Initialize Guice DI
        injector = Guice.createInjector(new ManhuntModule(this));

        // Register saga event handlers
        registerSagaEventHandlers();

        // Initialize infrastructure singletons with DI instances
        initializeInfrastructure();

        CommandsDispatcher commandDispatcher = new CommandsDispatcher();

        getCommand("f_manhunt").setExecutor(commandDispatcher);
    }

    /**
     * Initializes infrastructure components with DI instances.
     * Bridges the singleton pattern with dependency injection during migration.
     */
    private void initializeInfrastructure() {
        // Get DI-managed instance and set it as the singleton
        NewManhuntLauncher launcher = injector.getInstance(NewManhuntLauncher.class);
        NewManhuntLauncher.setInstance(launcher);
    }

    /**
     * Registers domain event handlers for sagas.
     * Wires saga methods to domain events via the event publisher.
     */
    private void registerSagaEventHandlers() {
        DomainEventPublisher eventPublisher = injector.getInstance(DomainEventPublisher.class);
        StartGameSaga startGameSaga = injector.getInstance(StartGameSaga.class);
        EndGameSaga endGameSaga = injector.getInstance(EndGameSaga.class);

        // StartGameSaga handlers
        eventPublisher.subscribe(GameSessionCreatedEvent.class, startGameSaga::onSessionCreated);
        eventPublisher.subscribe(RolesDistributedEvent.class, startGameSaga::onRolesDistributed);
        eventPublisher.subscribe(RolesAssignedEvent.class, startGameSaga::onRolesAssigned);
        eventPublisher.subscribe(HandlersRegisteredEvent.class, startGameSaga::onHandlersRegistered);

        // EndGameSaga handlers
        eventPublisher.subscribe(GameEndedEvent.class, endGameSaga::onGameEnded);
    }

    /**
     * Gets the Guice injector instance.
     * Used by other infrastructure components for dependency resolution.
     *
     * @return Guice injector
     */
    public Injector getInjector() {
        return injector;
    }
}
