package me.flamboyant;

import me.flamboyant.utils.Common;
import org.bukkit.plugin.java.JavaPlugin;

public class FlamboyantPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        super.onEnable();

        Common.plugin = this;
        Common.server = getServer();
    }
}
