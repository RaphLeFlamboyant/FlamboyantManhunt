package me.flamboyant.manhunt.domain.role.behavior;

import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.wincondition.IHunterWinConditionModifier;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleType;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SuperHunterRole extends HunterRole implements IHunterWinConditionModifier {
    private int speedRunnerKillCount = 0;

    public SuperHunterRole(Player owner) {
        super(owner);
    }

    @Override
    protected void broadcastPlayerResultMessage() {
        boolean wincon = isWinning();
        Bukkit.broadcastMessage(ChatHelper.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (wincon ? "gagné" : "perdu") + " !"));
    }

    @Override
    protected String getName() {
        return "Super Hunter";
    }

    @Override
    protected String getDescription() {
        return "Gagne quand tu as tué tous les speedrunners de tes propres mains, tu voles alors la victoir aux hunters." +
                " Si un des speedrunners meurt d'une autre façon alors tu as perdu" +
                " Tu détiens une boussole qui te donne sa position.";
    }

    @Override
    protected boolean doStart() {
        HunterRole.winconModifiers.add(this);
        return super.doStart();
    }

    @Override
    protected boolean doStop() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> HunterRole.winconModifiers.remove(this), 20);

        return super.doStop();
    }

    @Override
    public boolean isHunterWinPossible() {
        return !isWinning();
    }

    @EventHandler
    public void onEntityByEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) return;
        if (event.getDamager() != owner) return;

        Player player = (Player) event.getEntity();
        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session == null) return;

        AManhuntRole role = session.getRole(player);
        if (role == null || role.getRoleType() != ManhuntRoleType.SPEEDRUNNER) return;
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            speedRunnerKillCount++;
            owner.sendMessage("Tu as tué un total de " + speedRunnerKillCount + " speedrunners");
        }
    }

    private boolean isWinning() {
        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session == null) return false;

        long speedrunnerCount = 0;
        for (Player player : session.getPlayers()) {
            AManhuntRole role = session.getRole(player);
            if (role.getRoleType() == ManhuntRoleType.SPEEDRUNNER) {
                speedrunnerCount++;
            }
        }
        return speedRunnerKillCount >= speedrunnerCount;
    }
}
