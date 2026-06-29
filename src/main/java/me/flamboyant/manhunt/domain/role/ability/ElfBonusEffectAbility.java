package me.flamboyant.manhunt.domain.role.ability;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ElfBonusEffectAbility extends PassiveAbility {

    public ElfBonusEffectAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Passive effect applied on start
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);

        // Apply permanent effects
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false);
        context.getOwner().addPotionEffect(speed);

        PotionEffect jumpBoost = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false);
        context.getOwner().addPotionEffect(jumpBoost);
    }

    @Override
    public String getName() {
        return "Elf Bonus Effect";
    }

    @Override
    public String getDescription() {
        return "Obtient Speed 1 et Jump Boost 2 en permanence";
    }
}
