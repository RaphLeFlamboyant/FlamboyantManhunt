package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public class SwapperAbility extends ItemActivatedAbility {
    private Player lastTargetedPlayer;

    public SwapperAbility(AbilityContext context) {
        super(context, new ItemStack(Material.ENDER_PEARL), Duration.ofMinutes(5));
    }

    @Override
    protected void activate() {
        if (lastTargetedPlayer == null || !lastTargetedPlayer.isOnline()) {
            context.sendMessage("&cAucun joueur ciblé !");
            return;
        }

        Location ownerLoc = context.getOwner().getLocation();
        Location targetLoc = lastTargetedPlayer.getLocation();

        context.getOwner().teleport(targetLoc);
        lastTargetedPlayer.teleport(ownerLoc);

        context.sendMessage("&aÉchange de position avec " + lastTargetedPlayer.getDisplayName());
    }

    @Override
    public String getName() {
        return "Swapper";
    }

    @Override
    public String getDescription() {
        return "Échange de position avec le dernier joueur ciblé";
    }
}
