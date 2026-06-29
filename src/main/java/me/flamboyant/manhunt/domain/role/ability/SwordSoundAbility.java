package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SwordSoundAbility extends PassiveAbility {
    private final Sound sound;

    public SwordSoundAbility(AbilityContext context, Sound sound) {
        super(context);
        this.sound = sound;
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        context.registerEventHandler(EntityDamageByEntityEvent.class, this::onEntityDamageByEntity);
        context.registerEventHandler(PlayerInteractEvent.class, this::onPlayerInteract);
    }

    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() != context.getOwner()) {
            return;
        }

        Player owner = context.getOwner();
        if (owner.getInventory().getItemInMainHand() == null
                || owner.getInventory().getItemInMainHand().getType() == Material.AIR) {
            return;
        }

        if (!owner.getInventory().getItemInMainHand().getType().toString().contains("SWORD")) {
            return;
        }

        owner.getWorld().playSound(owner, sound, SoundCategory.VOICE, 1, 1.3f);
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!event.getItem().getType().toString().contains("SWORD")) {
            return;
        }

        Player owner = context.getOwner();
        owner.getWorld().playSound(owner, sound, SoundCategory.VOICE, 1, 1.3f);
    }

    @Override
    public String getName() {
        return "Sword Sound";
    }

    @Override
    public String getDescription() {
        return "Fait un bruit courageux quand tu attaques avec une épée";
    }
}
