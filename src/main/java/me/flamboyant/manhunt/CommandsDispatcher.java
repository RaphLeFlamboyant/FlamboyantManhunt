package me.flamboyant.manhunt;

import me.flamboyant.gui.ConfigurablePluginListener;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandsDispatcher implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(sender instanceof Player)
        {
            Player commandSender = (Player) sender;
            ILaunchablePlugin pluginToLaunch = null;
            switch (cmd.getName())
            {
                case "f_manhunt":
                    pluginToLaunch = NewManhuntLauncher.getInstance();
                    break;
                default :
                    break;
            }
            if (pluginToLaunch != null) launchPlugin(commandSender, pluginToLaunch);
            return true;
        }
        return false;
    }

    private void launchPlugin(Player sender, ILaunchablePlugin plugin) {
        if (plugin.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Plugin stopped");
            plugin.stop();
            return;
        }

        plugin.resetParameters();

        if (!ConfigurablePluginListener.getInstance().isLaunched())
            ConfigurablePluginListener.getInstance().launch(plugin, sender);

        sender.sendMessage("Plugin started");
    }
}
