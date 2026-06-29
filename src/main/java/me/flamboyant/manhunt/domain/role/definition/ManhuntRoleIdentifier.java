package me.flamboyant.manhunt.domain.role.definition;

public enum ManhuntRoleIdentifier {
    HUNTER_SIMPLE(ManhuntRoleType.HUNTER),
    HUNTER_CHECKPOINT(ManhuntRoleType.HUNTER),
    HUNTER_CUTCLEAN(ManhuntRoleType.HUNTER),
    HUNTER_LINK(ManhuntRoleType.HUNTER),
    HUNTER_PRO_MINER(ManhuntRoleType.HUNTER),
    HUNTER_ELF(ManhuntRoleType.HUNTER),
    SPEEDRUNNER_SIMPLE(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_CUTCLEAN(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_SWAPPER(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_LINK(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_CHECKPOINT(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_ELF(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_WEREWOLF(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_TNT_TACTICAL(ManhuntRoleType.SPEEDRUNNER),
    SPEEDRUNNER_NO_NAME_TAG(ManhuntRoleType.SPEEDRUNNER),
    HUNTER_GLUER(ManhuntRoleType.HUNTER),
    HUNTER_IMPOSTER(ManhuntRoleType.HUNTER),
    HUNTER_SUPER(ManhuntRoleType.HUNTER),
    ALLY_UNDECIDED(ManhuntRoleType.ALLY),
    ALLY_SIMPLE(ManhuntRoleType.ALLY),
    NEUTRAL_SIMPLE(ManhuntRoleType.NEUTRAL);

    private final ManhuntRoleType roleType;

    ManhuntRoleIdentifier(ManhuntRoleType roleType) {
        this.roleType = roleType;
    }

    public ManhuntRoleType getRoleType() {
        return roleType;
    }
}
