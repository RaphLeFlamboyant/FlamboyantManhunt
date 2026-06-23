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
    ALLY_IMPOSTER(ManhuntRoleType.ALLY),
    SUPER_HUNTER(ManhuntRoleType.HUNTER),
    NEUTRAL_GLUER(ManhuntRoleType.NEUTRAL),
    NEUTRAL_UNDECIDED(ManhuntRoleType.NEUTRAL);

    private final ManhuntRoleType roleType;

    ManhuntRoleIdentifier(ManhuntRoleType roleType) {
        this.roleType = roleType;
    }

    public ManhuntRoleType getRoleType() {
        return roleType;
    }
}
