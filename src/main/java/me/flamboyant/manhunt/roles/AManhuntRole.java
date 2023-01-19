package me.flamboyant.gamemodes.newmanhunt.roles;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public abstract class AManhuntRole {
    private boolean running;
    protected Player owner;

    public AManhuntRole(Player owner) {
        this.owner = owner;
    }

    public boolean start() {
        if (running || owner == null) {
            return false;
        }

        running = doStart();

        owner.sendMessage(ChatColor.GOLD + "Tu es " + ChatColor.AQUA + ChatColor.BOLD + "[" + getName() + "]"
                + "\n" + ChatColor.RESET + ChatColor.LIGHT_PURPLE + getDescription());

        return running;
    }

    public boolean stop() {
        if (!running) {
            return false;
        }

        broadcastPlayerResultMessage();
        running = doStop();

        return true;
    }

    public boolean isRunning() {
        return running;
    }

    protected abstract boolean doStop();
    protected abstract boolean doStart();
    protected abstract void broadcastPlayerResultMessage();

    protected abstract String getName();
    protected abstract String getDescription();
    public abstract ManhuntRoleType getRoleType();
}
