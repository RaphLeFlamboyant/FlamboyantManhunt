package me.flamboyant.manhunt.application.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import me.flamboyant.manhunt.NewManhuntManager;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.sagas.EndGameSaga;
import me.flamboyant.manhunt.application.sagas.StartGameSaga;
import me.flamboyant.manhunt.application.services.EventHandlerRegistrationService;
import me.flamboyant.manhunt.application.services.GameLifecycleService;
import me.flamboyant.manhunt.application.services.RoleAssignmentService;
import me.flamboyant.manhunt.application.services.RoleDistributionService;
import me.flamboyant.manhunt.domain.event.DomainEventPublisher;
import me.flamboyant.manhunt.domain.event.InMemoryEventPublisher;
import me.flamboyant.manhunt.domain.role.definition.RoleRegistry;
import me.flamboyant.manhunt.domain.role.behavior.*;
import me.flamboyant.manhunt.domain.role.distribution.*;
import me.flamboyant.manhunt.domain.role.distribution.strategies.*;
import me.flamboyant.manhunt.domain.wincondition.AllSpeedrunnersDeadCondition;
import me.flamboyant.manhunt.domain.wincondition.DragonKilledCondition;
import me.flamboyant.manhunt.domain.wincondition.WinConditionEvaluator;
import me.flamboyant.utils.Common;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;

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

        // Sagas
        bind(StartGameSaga.class);
        bind(EndGameSaga.class);

        // Domain services
        bind(WinConditionEvaluator.class).toProvider(WinConditionEvaluatorProvider.class);

        // Infrastructure (transition from singleton)
        bind(GameSessionManager.class).toProvider(GameSessionManagerProvider.class);
        bind(NewManhuntLauncher.class);

        // Event publisher
        bind(DomainEventPublisher.class).toProvider(EventPublisherProvider.class).in(Singleton.class);
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
     * Each role is registered using method references for type-safe construction.
     */
    @Provides
    @Singleton
    public RoleRegistry provideRoleRegistry() {
        RoleRegistry registry = new RoleRegistry();

        // Speedrunner roles
        registry.register(SPEEDRUNNER_SIMPLE, SpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_SWAPPER, SpeedrunnerSwapperRole::new);
        registry.register(SPEEDRUNNER_LINK, LinkSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_CHECKPOINT, CheckpointSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_ELF, ElfSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_WEREWOLF, WerewolfSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_CUTCLEAN, CutCleanSpeedrunnerRole::new);
        registry.register(SPEEDRUNNER_TNT_TACTICAL, TntTacticalSpeedrunnerRole::new);

        // Hunter roles
        registry.register(HUNTER_SIMPLE, HunterRole::new);
        registry.register(HUNTER_CHECKPOINT, CheckpointHunterRole::new);
        registry.register(HUNTER_CUTCLEAN, CutCleanHunterRole::new);
        registry.register(HUNTER_LINK, LinkHunterRole::new);
        registry.register(HUNTER_PRO_MINER, ProMinerRole::new);
        registry.register(HUNTER_ELF, ElfHunterRole::new);
        registry.register(SUPER_HUNTER, SuperHunterRole::new);

        // Special roles
        registry.register(ALLY_IMPOSTER, ImposterRole::new);
        registry.register(NEUTRAL_GLUER, GluerRole::new);
        registry.register(NEUTRAL_UNDECIDED, UndecidedRole::new);

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
