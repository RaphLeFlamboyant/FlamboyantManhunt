package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Collections;
import java.util.Set;

/**
 * Win condition: All speedrunners are dead.
 * When met, the Hunter team wins.
 */
public class AllSpeedrunnersDeadCondition implements WinCondition {

    @Override
    public boolean isMet(GameSession session) {
        // Treat 0 or negative as "all dead"
        return session.getRemainingSpeedrunners() <= 0;
    }

    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Collections.singleton(ManhuntRoleType.HUNTER);
    }

    @Override
    public String getDescription() {
        return "Tous les speedrunners sont morts ! L'equipe HUNTER a gagne !";
    }
}
