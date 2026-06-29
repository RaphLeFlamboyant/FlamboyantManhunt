package me.flamboyant.manhunt.domain.role.ability;

public interface Ability {
    /**
     * Called when the role starts. Ability registers event handlers here.
     */
    void onRoleStart(AbilityContext context);

    /**
     * Called when the role stops. Ability cleans up here.
     */
    void onRoleStop(AbilityContext context);

    /**
     * Human-readable ability name.
     */
    String getName();

    /**
     * Human-readable ability description.
     */
    String getDescription();
}
