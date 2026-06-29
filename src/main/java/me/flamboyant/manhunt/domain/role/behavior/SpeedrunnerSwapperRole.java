package me.flamboyant.manhunt.domain.role.behavior;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.flamboyant.manhunt.application.GameSessionManager;
import me.flamboyant.manhunt.application.services.EventRegistrationService;
import me.flamboyant.manhunt.application.services.ItemService;
import me.flamboyant.manhunt.application.services.MessageService;
import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.infrastructure.ui.PlayerSelectionView;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.flamboyant.manhunt.domain.role.definition.ManhuntRoleIdentifier;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SpeedrunnerSwapperRole extends SpeedrunnerRole {
    private PlayerSelectionView roleView;

    @Inject
    public SpeedrunnerSwapperRole(
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
    protected boolean doStart() {
        owner.getInventory().addItem(getTargetSelectionItem());

        GameSession session = GameSessionManager.getInstance().getActiveSessionForPlayer(owner);
        if (session != null) {
            roleView = new PlayerSelectionView(
                session.getPlayers().stream().filter(p -> p != owner).collect(Collectors.toList()),
                "Death Swapper Selection"
            );
        }
        return super.doStart();
    }

    @Override
    protected boolean doStop() {
        owner.setCooldown(Material.RECOVERY_COMPASS, 0);
        return super.doStop();
    }

    @Override
    public String getName() {
        return "Death Swapper Speedrunner";
    }

    @Override
    protected String getDescription() {
        return super.getDescription() + " Pour t'aider tu as une boussole spéciale qui te permet de sélectionner un joueur toutes les 5 minutes." +
                " Cela intervertit vos position au moment du clic !";
    }

    @Override
    public ManhuntRoleIdentifier getRoleIdentifier() {
        return ManhuntRoleIdentifier.SPEEDRUNNER_SWAPPER;
    }

    @Override
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        super.onPlayerInteract(event);
        if (event.getPlayer() != owner) return;
        if (!itemService.isExactlySameItemKind(event.getItem(), getTargetSelectionItem())) return;
        event.setCancelled(true);

        server.getPluginManager().registerEvents(roleView, plugin);
        owner.openInventory(roleView.getView());
    }

    @Override
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        super.onInventoryClose(event);
        if (event.getPlayer() != owner) return;
        if (event.getInventory() != roleView.getView()) return;
        if (owner.getCooldown(Material.RECOVERY_COMPASS) > 0) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> doCountDown(6), 1 * 20);

        owner.setCooldown(Material.RECOVERY_COMPASS, 5 * 60 * 20);
    }

    private void doCountDown(int seconds) {
        int next = seconds - 1;
        if (next > 0) {
            owner.sendMessage(messageService.feedback(next + " secondes avant swap !"));
            Bukkit.getScheduler().runTaskLater(plugin, () -> doCountDown(next), 1 * 20);
        }
        else {
            Player target = roleView.getSelectedPlayer();
            roleView.close();
            Location ownerLocation = owner.getLocation();
            owner.teleport(target.getLocation());
            target.teleport(ownerLocation);
        }
    }

    private ItemStack getTargetSelectionItem() {
        return itemService.generateItem(Material.RECOVERY_COMPASS, 1, "Choisir le joueur", Arrays.asList("Change ta place avec un joueur"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
