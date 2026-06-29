package me.flamboyant.manhunt.application.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import me.flamboyant.manhunt.NewManhuntManager;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.application.services.EventHandlerRegistrationService;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.RoleAssignmentService;
import me.flamboyant.manhunt.application.services.RoleDistributionService;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.role.definition.AssistedRoleFactory;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
import me.flamboyant.manhunt.domain.role.behavior.*;
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.DragonKilledCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionEvaluator;
import me.flamboyant.manhunt.infrastructure.services.BukkitEventRegistrationService;
import me.flamboyant.manhunt.infrastructure.services.BukkitItemService;
import me.flamboyant.manhunt.infrastructure.services.BukkitMessageService;
import me.flamboyant.manhunt.application.services.GameLaunchService;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

/**
 * Guice dependency injection module for Manhunt plugin.
 * Configures bindings for all application services, sagas, and domain services.
 */
public class ManhuntModule extends AbstractModule {
    private final Plugin plugin;

    public ManhuntModule(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Bind plugin instance
        bind(Plugin.class).annotatedWith(Names.named("plugin")).toInstance(plugin);

        // Application services (all @Singleton scope)
        bind(GameLifecycleService.class);
        bind(RoleDistributionService.class);
        bind(RoleAssignmentService.class);
        bind(EventHandlerRegistrationService.class);
        bind(GameLaunchService.class).in(Singleton.class);

        // Sagas
        bind(StartGameSaga.class);
        bind(EndGameSaga.class);

        // Domain services
        bind(WinConditionEvaluator.class).toProvider(WinConditionEvaluatorProvider.class);

        // Infrastructure (transition from singleton)
        bind(GameSessionManager.class).toProvider(GameSessionManagerProvider.class);

        // Infrastructure adapter
        bind(ManhuntPluginAdapter.class).in(Singleton.class);
        bind(ILaunchablePlugin.class).to(ManhuntPluginAdapter.class);

        // Event publisher
        bind(DomainEventPublisher.class).toProvider(EventPublisherProvider.class).in(Singleton.class);

        // Bukkit primitives
        bind(Server.class).toInstance(plugin.getServer());
        bind(Plugin.class).toInstance(plugin);

        // Infrastructure services
        bind(MessageService.class).to(BukkitMessageService.class).in(Singleton.class);
        bind(ItemService.class).to(BukkitItemService.class).in(Singleton.class);
        bind(EventRegistrationService.class).to(BukkitEventRegistrationService.class).in(Singleton.class);

        // AssistedInject factories for all roles
        installRoleFactories();
    }

    /**
     * Install AssistedInject factories for all role types.
     * Each role gets a named factory that Guice implements automatically.
     * Named bindings match ManhuntRoleIdentifier enum values.
     */
    private void installRoleFactories() {
        // Speedrunner roles (8 variants)
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

        // Hunter roles (7 variants)
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
            .implement(AManhuntRole.class, Names.named("SUPER_HUNTER"), SuperHunterRole.class)
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

        // Ally and neutral roles (3 variants)
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("NEUTRAL_GLUER"), GluerRole.class)
            .build(AssistedRoleFactory.class));

        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("ALLY_IMPOSTER"), ImposterRole.class)
            .build(AssistedRoleFactory.class));

        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Names.named("NEUTRAL_UNDECIDED"), UndecidedRole.class)
            .build(AssistedRoleFactory.class));
    }

    @Provides
    @Singleton
    public NewManhuntManager provideNewManhuntManager(
            WinConditionEvaluator evaluator,
            DragonKilledCondition dragonCondition) {
        return new NewManhuntManager(evaluator, dragonCondition);
    }

    @Provides
    @Singleton
    public AllSpeedrunnersDeadCondition provideAllSpeedrunnersDeadCondition() {
        return new AllSpeedrunnersDeadCondition();
    }

    @Provides
    @Singleton
    public DragonKilledCondition provideDragonKilledCondition() {
        return new DragonKilledCondition();
    }

    /**
     * Provides the RoleRegistry singleton with all role factories registered.
     * Uses Injector to retrieve AssistedInject factories for each role type.
     */
    @Provides
    @Singleton
    public RoleRegistry provideRoleRegistry(Injector injector) {
        RoleRegistry registry = new RoleRegistry();

        // Register all role identifiers with their AssistedInject factories
        for (ManhuntRoleIdentifier id : ManhuntRoleIdentifier.values()) {
            try {
                AssistedRoleFactory<?> factory = injector.getInstance(
                    Key.get(AssistedRoleFactory.class, Names.named(id.name())));
                registry.register(id, factory);
            } catch (Exception e) {
                // Skip roles without factory bindings (if any)
                org.bukkit.Bukkit.getLogger().warning("No factory binding for role: " + id.name());
            }
        }

        return registry;
    }

    /**
     * Provider for GameSessionManager singleton instance.
     * Transitioning from static getInstance() pattern.
     */
    static class GameSessionManagerProvider implements com.google.inject.Provider<GameSessionManager> {
        @Override
        public GameSessionManager get() {
            return GameSessionManager.getInstance();
        }
    }

    /**
     * Provider for DomainEventPublisher.
     * Creates a new InMemoryEventPublisher instance.
     */
    static class EventPublisherProvider implements com.google.inject.Provider<DomainEventPublisher> {
        @Override
        public DomainEventPublisher get() {
            return new InMemoryEventPublisher();
        }
    }

    /**
     * Provider for WinConditionEvaluator with its dependencies.
     */
    static class WinConditionEvaluatorProvider implements com.google.inject.Provider<WinConditionEvaluator> {
        private final AllSpeedrunnersDeadCondition allSpeedrunnersDeadCondition;
        private final DragonKilledCondition dragonKilledCondition;

        @com.google.inject.Inject
        public WinConditionEvaluatorProvider(
                AllSpeedrunnersDeadCondition allSpeedrunnersDeadCondition,
                DragonKilledCondition dragonKilledCondition) {
            this.allSpeedrunnersDeadCondition = allSpeedrunnersDeadCondition;
            this.dragonKilledCondition = dragonKilledCondition;
        }

        @Override
        public WinConditionEvaluator get() {
            return new WinConditionEvaluator(
                java.util.Arrays.asList(
                    allSpeedrunnersDeadCondition,
                    dragonKilledCondition
                )
            );
        }
    }

    @Provides
    @Singleton
    public RoleCountStrategy provideRoleCountStrategy() {
        return new TieredRoleCountStrategy();
    }

    @Provides
    @Singleton
    public ConflictResolutionStrategy provideConflictResolutionStrategy() {
        return new OverwriteConflictResolution();
    }

    @Provides
    @Singleton
    public RoleAssignmentStrategy provideRoleAssignmentStrategy() {
        return new ProbabilisticRoleAssignment();
    }

    @Provides
    @Singleton
    public Random provideRandom() {
        return Common.rng;
    }
}
