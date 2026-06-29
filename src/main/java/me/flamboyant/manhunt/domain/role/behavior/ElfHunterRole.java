package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class ElfHunterRole extends HunterRole {
    private static final int degres = 3;

    @Inject
    public ElfHunterRole(
        @Assisted Player owner,
        Server server,
        Plugin plugin,
        MessageService messageService,
        ItemService itemService,
        EventRegistrationService eventRegistration
    ) {
        super(owner, server, plugin, messageService, itemService, eventRegistration);
    }

    @Override
    protected boolean doStop() {
        EntityShootBowEvent.getHandlerList().unregister(this);
        return super.doStop();
    }

    @Override
    public String getName() {
        return "Hunter Elfe";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() +
                "Tirer à l'arc envoie une salve de 5 flèches. " +
                "Tirer à l'abalète t'inflige des dégâts. " +
                "Taper un joueur au corps à corps t'inflige des dégâts.";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.HUNTER_ELF;
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() != owner) return;
        if (event.getEntity().getType() != EntityType.PLAYER) return;

        owner.damage(4);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() != owner) return;
        if (event.getBow().getType() == Material.CROSSBOW) {
            owner.damage(2);
            return;
        }

        AbstractArrow projectile = (AbstractArrow) event.getProjectile();
        Vector velocity = projectile.getVelocity();
        Vector fakeVector = new Vector(velocity.getX(), 0, velocity.getZ());
        double length = fakeVector.length();
        fakeVector = fakeVector.normalize();
        double cos = fakeVector.getX();
        double sin = fakeVector.getZ();
        double angle = Math.toDegrees(sin < 0 ? Math.acos(cos) * -1 : Math.acos(cos));

        for (int i = -2; i < 3; i++){
            if (i == 0) continue;

            double newAngle = angle + (degres * i);
            cos = Math.cos(Math.toRadians(newAngle));
            sin = Math.sin(Math.toRadians(newAngle));
            Vector arrowAngled = new Vector(cos, 0, sin).normalize().multiply(length);
            arrowAngled.setY(velocity.getY());
            owner.launchProjectile(Arrow.class, arrowAngled);
        }
    }
}
