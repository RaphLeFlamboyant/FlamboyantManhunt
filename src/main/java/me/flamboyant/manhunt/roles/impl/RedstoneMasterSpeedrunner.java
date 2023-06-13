package me.flamboyant.manhunt.roles.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class RedstoneMasterSpeedrunner extends SpeedrunnerRole {
    public RedstoneMasterSpeedrunner(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        BlockBreakEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    protected boolean doStart() {


        return super.doStart();
    }

    @Override
    protected String getName() {
        return "Redstone Master Speedrunner";
    }

    @Override
    protected String getDescription() {
        return "Tu gagnes quand le dragon meurt mais tu perds si tu meurs avant ! " +
                "Toutes les 5 minutes tu reçois du matériel de redstone aléatoire.";
    }
}
