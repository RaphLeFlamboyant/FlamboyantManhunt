package me.flamboyant.manhunt.domain.plugin;

/**
 * Domain interface for optional game plugins.
 * Replaces framework ILaunchablePlugin.
 */
public interface GamePlugin {
    /**
     * Start the plugin.
     */
    void start();

    /**
     * Stop the plugin.
     */
    void stop();
}
