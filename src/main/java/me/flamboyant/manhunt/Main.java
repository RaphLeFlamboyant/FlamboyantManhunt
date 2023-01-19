package me.flamboyant.manhunt;

import me.flamboyant.FlamboyantPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends FlamboyantPlugin {

    @Override
    public void onEnable() {
        super.onEnable();

        CommandsDispatcher commandDispatcher = new CommandsDispatcher();

        getCommand("f_manhunt").setExecutor(commandDispatcher);
    }
}
