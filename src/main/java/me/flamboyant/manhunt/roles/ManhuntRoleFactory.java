package me.flamboyant.manhunt.roles;

import me.flamboyant.manhunt.roles.impl.*;
import org.bukkit.entity.Player;

public class ManhuntRoleFactory {
    public static AManhuntRole createRole(Player owner, ManhuntRoleIdentifier roleIdentifier) {
        switch (roleIdentifier) {
            case SPEEDRUNNER_SIMPLE:
                return new SpeedrunnerRole(owner);
            case SPEEDRUNNER_SWAPPER:
                return new SpeedrunnerSwapperRole(owner);
            case SPEEDRUNNER_LINK:
                return new LinkSpeedrunnerRole(owner);
            case SPEEDRUNNER_CHECKPOINT:
                return new CheckpointSpeedrunnerRole(owner);
            case SPEEDRUNNER_ELF:
                return new ElfSpeedrunnerRole(owner);
            case SPEEDRUNNER_WEREWOLF:
                    return new WerewolfSpeedrunnerRole(owner);
            case SPEEDRUNNER_CUTCLEAN:
                return new CutCleanSpeedrunnerRole(owner);
            case SPEEDRUNNER_TNT_TACTICAL:
                return new TntTacticalSpeedrunnerRole(owner);
            case SUPER_HUNTER:
                return new SuperHunterRole(owner);
            case ALLY_IMPOSTER:
                return new ImposterRole(owner);
            case NEUTRAL_GLUER:
                return new GluerRole(owner);
            case NEUTRAL_UNDECIDED:
                return new UndecidedRole(owner);
            case HUNTER_CHECKPOINT:
                return new CheckpointHunterRole(owner);
            case HUNTER_CUTCLEAN:
                return new CutCleanHunterRole(owner);
            case HUNTER_LINK:
                return new LinkHunterRole(owner);
            case HUNTER_PRO_MINER:
                return new ProMinerRole(owner);
            case HUNTER_ELF:
                return new ElfHunterRole(owner);
            case HUNTER_SIMPLE:
            default:
                return new HunterRole(owner);
        }
    }
}
