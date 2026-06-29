package me.flamboyant.manhunt.domain.role.ability;

public class UndecidedRoleAbility extends PassiveAbility {

    public UndecidedRoleAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Special undecided role logic
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        context.sendMessage("&7Tu es indécis, ton rôle sera révélé plus tard...");
    }

    @Override
    public String getName() {
        return "Undecided Role";
    }

    @Override
    public String getDescription() {
        return "Rôle non encore décidé";
    }
}
