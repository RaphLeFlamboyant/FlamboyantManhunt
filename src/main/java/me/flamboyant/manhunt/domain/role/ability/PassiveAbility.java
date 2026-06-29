package me.flamboyant.manhunt.domain.role.ability;

public abstract class PassiveAbility implements Ability {
    protected final AbilityContext context;

    protected PassiveAbility(AbilityContext context) {
        this.context = context;
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        registerEventHandlers(context);
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
    }

    protected abstract void registerEventHandlers(AbilityContext context);
}
