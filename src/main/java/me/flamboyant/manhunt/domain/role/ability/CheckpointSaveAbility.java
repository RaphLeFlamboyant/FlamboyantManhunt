package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

public class CheckpointSaveAbility extends ItemActivatedAbility {
    private final CheckpointStorage storage;

    public CheckpointSaveAbility(
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
        Player owner = context.getOwner();
        Checkpoint checkpoint = new Checkpoint(
            owner.getLocation(),
            owner.getHealth(),
            owner.getFoodLevel(),
            owner.getSaturation(),
            owner.getFireTicks(),
            Arrays.asList(owner.getInventory().getContents()),
            new HashSet<>(owner.getActivePotionEffects())
        );
        storage.saveCheckpoint(checkpoint);
        context.sendMessage("&aCheckpoint sauvegardé!");
    }

    @Override
    public String getName() {
        return "Checkpoint Save";
    }

    @Override
    public String getDescription() {
        return "Sauvegarde ta position et ton état";
    }
}
