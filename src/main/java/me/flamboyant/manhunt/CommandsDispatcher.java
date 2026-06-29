package me.flamboyant.manhunt;

import com.google.inject.Inject;
import me.flamboyant.gui.ConfigurablePluginListener;
import me.flamboyant.manhunt.infrastructure.adapters.ManhuntPluginAdapter;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Singleton;

@Singleton
public class CommandsDispatcher implements CommandExecutor {
    private final ManhuntPluginAdapter adapter;

    @Inject
    public CommandsDispatcher(ManhuntPluginAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if (sender instanceof Player) {
            Player commandSender = (Player) sender;
            if ("f_manhunt".equals(cmd.getName())) {
                launchPlugin(commandSender, adapter);
            }
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

        if (!ConfigurablePluginListener.getInstance().isLaunched()) {
            ConfigurablePluginListener.getInstance().launch(plugin, sender);
        }

        sender.sendMessage("Plugin started");
    }
}
