package me.flamboyant.manhunt.roles;

import me.flamboyant.manhunt.roles.impl.HunterRole;
import me.flamboyant.manhunt.roles.impl.ImposterRole;
import me.flamboyant.manhunt.roles.impl.ItemFarmerRole;
import me.flamboyant.manhunt.roles.impl.SpeedrunnerRole;
import org.bukkit.entity.Player;

public class ManhuntRoleFactory {
    public static AManhuntRole createRole(Player owner, ManhuntRoleIdentifier roleIdentifier) {
        switch (roleIdentifier) {
            case SPEEDRUNNER_SIMPLE:
                return new SpeedrunnerRole(owner);
            case ALLY_IMPOSTER:
                return new ImposterRole(owner);
            case NEUTRAL_ITEM_FARMER:
                return new ItemFarmerRole(owner);
            case HUNTER_SIMPLE:
            default:
                return new HunterRole(owner);
        }
    }
}
