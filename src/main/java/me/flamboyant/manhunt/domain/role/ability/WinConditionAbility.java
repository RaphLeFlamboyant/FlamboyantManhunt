package me.flamboyant.manhunt.domain.role.ability;

public abstract class WinConditionAbility implements Ability {
    protected final AbilityContext context;

    protected WinConditionAbility(AbilityContext context) {
        this.context = context;
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        registerWinConditionHandlers(context);
    }

    @Override
    public void onRoleStop(AbilityContext context) {
        // Event cleanup handled by EventRegistrationService
    }

    protected abstract void registerWinConditionHandlers(AbilityContext context);
}
