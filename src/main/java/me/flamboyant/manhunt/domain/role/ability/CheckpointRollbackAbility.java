package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public class CheckpointRollbackAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage;

    public CheckpointRollbackAbility(
        AbilityContext context,
        ItemStack triggerItem,
        Duration cooldown,
        CheckpointStorage storage
    ) {
        super(context, triggerItem, cooldown);
        this.storage = storage;
    }

    @Override
    protected void activate() {
        Checkpoint checkpoint = storage.getCheckpoint();
        if (checkpoint == null) {
            context.sendMessage("&cAucun checkpoint sauvegardé!");
            return;
        }

        Player owner = context.getOwner();
        owner.teleport(checkpoint.getLocation());
        owner.setHealth(checkpoint.getHealth());
        owner.setFoodLevel(checkpoint.getFoodLevel());
        owner.setSaturation(checkpoint.getSaturation());
        owner.setFireTicks(checkpoint.getFireTicks());

        owner.getInventory().clear();
        checkpoint.getInventory().forEach(item -> {
            if (item != null) {
                owner.getInventory().addItem(item);
            }
        });

        owner.getActivePotionEffects().forEach(effect ->
            owner.removePotionEffect(effect.getType())
        );
        checkpoint.getEffects().forEach(effect ->
            owner.addPotionEffect(effect)
        );

        context.sendMessage("&aRetour au checkpoint!");
    }

    @Override
    public String getName() {
        return "Checkpoint Rollback";
    }

    @Override
    public String getDescription() {
        return "Retourne au dernier checkpoint";
    }
}
