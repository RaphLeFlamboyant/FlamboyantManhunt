package me.flamboyant.manhunt.roles.impl;

import me.flamboyant.common.utils.ChatColorUtils;
import me.flamboyant.common.utils.Common;
import me.flamboyant.manhunt.roles.AManhuntRole;
import me.flamboyant.manhunt.roles.ManhuntRoleType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.HashSet;

public class ItemFarmerRole extends AManhuntRole implements Listener {
    private HashSet<Material> carftedItems = new HashSet<>();

    public ItemFarmerRole(Player owner) {
        super(owner);
    }

    @Override
    protected boolean doStop() {
        Bukkit.broadcastMessage(ChatColorUtils.feedback(owner.getDisplayName() + ", qui était " + getName() + " a " + (carftedItems.size() >= 500 ? "gagné" : "perdu") + " !"));

        CraftItemEvent.getHandlerList().unregister(this);
        EntityPickupItemEvent.getHandlerList().unregister(this);
        return true;
    }

    @Override
    protected boolean doStart() {
        Common.server.getPluginManager().registerEvents(this, Common.plugin);
        return true;
    }

    @Override
    protected String getName() {
        return "Farmeur d'items";
    }

    @Override
    protected String getDescription() {
        return "Tu dois avoir obtenu 500 items différents quand la partie se termine !";
    }

    @Override
    public ManhuntRoleType getRoleType() {
        return ManhuntRoleType.NEUTRAL;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!event.getWhoClicked().equals(owner)) return;
        if (carftedItems.contains(event.getInventory().getResult().getType()))

        carftedItems.add(event.getInventory().getResult().getType());
        checkVictory();
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!event.getEntity().equals(owner)) return;
        Material material = event.getItem().getItemStack().getType();
        if (carftedItems.contains(material)) return;

        carftedItems.add(material);
    }

    private void checkVictory() {
        if (carftedItems.size() < 500)
            owner.sendMessage(ChatColorUtils.feedback("Encore " + (500 - carftedItems.size()) + " items pour gagner !"));
        else if (carftedItems.size() == 500)
            owner.sendMessage(ChatColorUtils.feedback("Tu as obtenu le nombre d'items requis pour gagner !"));
    }
}
