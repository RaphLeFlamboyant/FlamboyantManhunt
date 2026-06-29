package me.flamboyant.manhunt.domain.role.ability;

public class ImposterDeceptionAbility extends PassiveAbility {

    public ImposterDeceptionAbility(AbilityContext context) {
        super(context);
    }

    @Override
    protected void registerEventHandlers(AbilityContext context) {
        // Special logic - appears as speedrunner to others
    }

    @Override
    public void onRoleStart(AbilityContext context) {
        super.onRoleStart(context);
        // Implementation depends on how role is displayed to other players
        // This is a placeholder for the deception mechanic
    }

    @Override
    public String getName() {
        return "Imposter Deception";
    }

    @Override
    public String getDescription() {
        return "Apparaît comme un speedrunner aux autres joueurs";
    }
}
