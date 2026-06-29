package me.flamboyant.manhunt.domain.role.ability;

public class NoNameTagAbility extends PassiveAbility {

    public NoNameTagAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Implementation depends on scoreboard/name tag system
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        // Hide name tag logic
        context.getOwner().setCustomNameVisible(false);
    }

    @Override
    public String getName() {
        return "No Name Tag";
    }

    @Override
    public String getDescription() {
        return "Cache le nom au-dessus de la tête";
    }
}
