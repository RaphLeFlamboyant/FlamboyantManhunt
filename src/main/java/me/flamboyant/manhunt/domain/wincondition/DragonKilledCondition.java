package me.flamboyant.manhunt.domain.wincondition;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;

import java.util.Set;

/**
 * Win condition: The Ender Dragon has been killed.
 * When met, the Speedrunner team wins.
 *
 * Note: This is a stateful win condition that must be notified when the dragon dies.
 */
public class DragonKilledCondition implements WinCondition {

    private boolean dragonKilled = false;

    /**
     * Mark that the Ender Dragon has been killed.
     * Should be called by infrastructure when dragon death is detected.
     */
    public void markDragonKilled() {
        this.dragonKilled = true;
    }

    @Override
    public boolean isMet(GameSession session) {
        return dragonKilled;
    }

    @Override
    public Set<ManhuntRoleType> getWinners() {
        return Set.of(ManhuntRoleType.SPEEDRUNNER);
    }

    @Override
    public String getDescription() {
        return "Le dragon de l'End a ete tue ! L'equipe SPEEDRUNNER a gagne !";
    }

    /**
     * Reset the condition (useful for tests or new games).
     */
    public void reset() {
        this.dragonKilled = false;
    }
}
