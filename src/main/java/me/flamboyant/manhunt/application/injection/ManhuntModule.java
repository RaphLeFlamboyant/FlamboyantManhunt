package me.flamboyant.manhunt.application.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import me.flamboyant.utils.ILaunchablePlugin;
import me.flamboyant.utils.Common;
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
import me.flamboyant.manhunt.domain.services.SessionRepository;
import me.flamboyant.manhunt.domain.services.MessagingPort;
import me.flamboyant.manhunt.domain.services.ItemPort;
import me.flamboyant.manhunt.domain.services.EventRegistrationPort;
import me.flamboyant.manhunt.domain.role.ability.*;
import me.flamboyant.manhunt.domain.role.definition.AssistedRoleFactory;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinition;
import me.flamboyant.manhunt.domain.role.definition.RoleDefinitionRegistry;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier.*;
import static me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType.*;
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
        // Bind domain ports to application implementations
        bind(SessionRepository.class).toProvider(GameSessionManagerProvider.class);
        bind(MessagingPort.class).to(MessageService.class);
        bind(ItemPort.class).to(ItemService.class);
        bind(EventRegistrationPort.class).to(EventRegistrationService.class);

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
     * Install AssistedInject factory for the new composition-based Role class.
     * Single factory creates all role types using RoleDefinition.
     */
    private void installRoleFactories() {
        install(new FactoryModuleBuilder()
            .implement(AManhuntRole.class, Role.class)
            .build(AssistedRoleFactory.class));
    }

    @Provides
    @Singleton
    public NewManhuntManager provideNewManhuntManager(
            WinConditionEvaluator evaluator,
            DragonKilledCondition dragonCondition,
            EventRegistrationService eventRegistrationService) {
        return new NewManhuntManager(evaluator, dragonCondition, eventRegistrationService);
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

    @Provides
    @Singleton
    public RoleDefinitionRegistry provideRoleDefinitionRegistry() {
        RoleDefinitionRegistry registry = new RoleDefinitionRegistry();

        // Register all 19 roles
        registerSpeedrunnerVariants(registry);
        registerHunterVariants(registry);
        registerAllyVariants(registry);

        return registry;
    }

    private void registerSpeedrunnerVariants(RoleDefinitionRegistry registry) {
        // SPEEDRUNNER_SIMPLE
        registry.register(SPEEDRUNNER_SIMPLE, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SIMPLE)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Utiliser une boussole te donne la localisation d'un hunter, avec un " +
                        "cooldown de 15 minutes (mais il te faudra la fabriquer ou la trouver).")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .build());

        // SPEEDRUNNER_CHECKPOINT
        CheckpointStorage checkpointStorage = new CheckpointStorage();
        ItemStack checkpointItem = createCheckpointItem();
        ItemStack rollbackItem = createRollbackItem();

        registry.register(SPEEDRUNNER_CHECKPOINT, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_CHECKPOINT)
            .roleType(SPEEDRUNNER)
            .name("Checkpoint Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Tu obtiens deux objets. Le premier te permet de poser un checkpoint. " +
                        "Le second te permet de revenir à ton dernier checkpoint.")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new CheckpointSaveAbility(ctx, checkpointItem, Duration.ofSeconds(15), checkpointStorage))
            .withAbility(ctx -> new CheckpointRollbackAbility(ctx, rollbackItem, Duration.ofSeconds(15), checkpointStorage))
            .build());

        // SPEEDRUNNER_LINK
        registry.register(SPEEDRUNNER_LINK, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_LINK)
            .roleType(SPEEDRUNNER)
            .name("Link Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Casser des herbes te drop parfois des émeraudes. " +
                        "Tu fais un bruit courageux quand tu attaques avec une épée")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new GrassDropAbility(ctx))
            .withAbility(ctx -> new SwordSoundAbility(ctx, Sound.ENTITY_VILLAGER_AMBIENT))
            .build());

        // SPEEDRUNNER_TNT_TACTICAL
        registry.register(SPEEDRUNNER_TNT_TACTICAL, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_TNT_TACTICAL)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner Tactique TNT")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Tu as également une télécommande qui fait exploser le dernier bloc que tu as posé !")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new TntTacticalAbility(ctx))
            .build());

        // SPEEDRUNNER_WEREWOLF
        registry.register(SPEEDRUNNER_WEREWOLF, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_WEREWOLF)
            .roleType(SPEEDRUNNER)
            .name("Speedrunner Garou")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "La nuit tu obtiens Force 1, Night Vision et tu peux détecter les hunters avec " +
                        "une boussole toutes les 30 secondes")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new WerewolfNightStrengthAbility(ctx))
            .build());

        // SPEEDRUNNER_ELF
        registry.register(SPEEDRUNNER_ELF, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_ELF)
            .roleType(SPEEDRUNNER)
            .name("Elf Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Tu obtiens Speed 1 et Jump Boost 2 en permanence")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new ElfBonusEffectAbility(ctx))
            .build());

        // SPEEDRUNNER_CUTCLEAN
        registry.register(SPEEDRUNNER_CUTCLEAN, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_CUTCLEAN)
            .roleType(SPEEDRUNNER)
            .name("CutClean Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Les minerais et la nourriture sont automatiquement cuits")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new CutCleanAbility(ctx))
            .build());

        // SPEEDRUNNER_NO_NAME_TAG
        registry.register(SPEEDRUNNER_NO_NAME_TAG, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_NO_NAME_TAG)
            .roleType(SPEEDRUNNER)
            .name("No Name Tag Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Ton nom est caché au-dessus de ta tête")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new NoNameTagAbility(ctx))
            .build());

        // SPEEDRUNNER_SWAPPER
        registry.register(SPEEDRUNNER_SWAPPER, RoleDefinition.builder()
            .identifier(SPEEDRUNNER_SWAPPER)
            .roleType(SPEEDRUNNER)
            .name("Swapper Speedrunner")
            .description("Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                        "Tu peux échanger de position avec le dernier joueur ciblé")
            .withAbility(ctx -> new UIPickerCompassAbility(ctx, Duration.ofMinutes(15)))
            .withAbility(ctx -> new DragonWinConditionAbility(ctx))
            .withAbility(ctx -> new PortalTrackingAbility(ctx))
            .withAbility(ctx -> new SpeedrunnerDeathAbility(ctx))
            .withAbility(ctx -> new SwapperAbility(ctx))
            .build());
    }

    private void registerHunterVariants(RoleDefinitionRegistry registry) {
        // HUNTER_SIMPLE
        registry.register(HUNTER_SIMPLE, RoleDefinition.builder()
            .identifier(HUNTER_SIMPLE)
            .roleType(HUNTER)
            .name("Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position.")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .build());

        // HUNTER_CHECKPOINT
        CheckpointStorage hunterCheckpointStorage = new CheckpointStorage();
        ItemStack hunterCheckpointItem = createCheckpointItem();
        ItemStack hunterRollbackItem = createRollbackItem();

        registry.register(HUNTER_CHECKPOINT, RoleDefinition.builder()
            .identifier(HUNTER_CHECKPOINT)
            .roleType(HUNTER)
            .name("Checkpoint Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Tu peux poser des checkpoints.")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new CheckpointSaveAbility(ctx, hunterCheckpointItem, Duration.ofSeconds(15), hunterCheckpointStorage))
            .withAbility(ctx -> new CheckpointRollbackAbility(ctx, hunterRollbackItem, Duration.ofSeconds(15), hunterCheckpointStorage))
            .build());

        // HUNTER_PRO_MINER
        registry.register(HUNTER_PRO_MINER, RoleDefinition.builder()
            .identifier(HUNTER_PRO_MINER)
            .roleType(HUNTER)
            .name("Pro Miner Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Parfois en minant de la roche, de la " +
                        "deepslate ou de la netherack, tu obtiens du minerai.")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new ProMinerAbility(ctx))
            .build());

        // HUNTER_SUPER
        registry.register(HUNTER_SUPER, RoleDefinition.builder()
            .identifier(HUNTER_SUPER)
            .roleType(HUNTER)
            .name("Super Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Conditions de victoire modifiées.")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new SuperHunterWinModifierAbility(ctx))
            .build());

        // HUNTER_ELF
        registry.register(HUNTER_ELF, RoleDefinition.builder()
            .identifier(HUNTER_ELF)
            .roleType(HUNTER)
            .name("Elf Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Tu obtiens Speed 1 et Jump Boost 2 en permanence")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new ElfBonusEffectAbility(ctx))
            .build());

        // HUNTER_LINK
        registry.register(HUNTER_LINK, RoleDefinition.builder()
            .identifier(HUNTER_LINK)
            .roleType(HUNTER)
            .name("Link Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Casser des herbes te drop parfois des émeraudes.")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new GrassDropAbility(ctx))
            .withAbility(ctx -> new SwordSoundAbility(ctx, Sound.ENTITY_VILLAGER_AMBIENT))
            .build());

        // HUNTER_CUTCLEAN
        registry.register(HUNTER_CUTCLEAN, RoleDefinition.builder()
            .identifier(HUNTER_CUTCLEAN)
            .roleType(HUNTER)
            .name("CutClean Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Les minerais et la nourriture sont automatiquement cuits")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new CutCleanAbility(ctx))
            .build());

        // HUNTER_GLUER
        registry.register(HUNTER_GLUER, RoleDefinition.builder()
            .identifier(HUNTER_GLUER)
            .roleType(HUNTER)
            .name("Gluer Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Applique Slowness 2 aux speedrunners proches")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new GluerSlownessAbility(ctx))
            .build());

        // HUNTER_IMPOSTER
        registry.register(HUNTER_IMPOSTER, RoleDefinition.builder()
            .identifier(HUNTER_IMPOSTER)
            .roleType(HUNTER)
            .name("Imposter Hunter")
            .description("Gagne quand le speedrunner meurt. Tu détiens une boussole qui " +
                        "te donne sa position. Tu apparais comme un speedrunner aux autres joueurs")
            .withAbility(ctx -> new CyclingCompassAbility(ctx, Duration.ofSeconds(30)))
            .withAbility(ctx -> new CompassOnStartAbility(ctx))
            .withAbility(ctx -> new CompassOnRespawnAbility(ctx))
            .withAbility(ctx -> new ImposterDeceptionAbility(ctx))
            .build());
    }

    private void registerAllyVariants(RoleDefinitionRegistry registry) {
        // ALLY_UNDECIDED
        registry.register(ALLY_UNDECIDED, RoleDefinition.builder()
            .identifier(ALLY_UNDECIDED)
            .roleType(ALLY)
            .name("Undecided")
            .description("Ton rôle sera révélé plus tard...")
            .withAbility(ctx -> new UndecidedRoleAbility(ctx))
            .build());
    }

    // Helper methods for item creation
    private ItemStack createCheckpointItem() {
        return new ItemStack(Material.RECOVERY_COMPASS);
    }

    private ItemStack createRollbackItem() {
        return new ItemStack(Material.ECHO_SHARD);
    }
}
