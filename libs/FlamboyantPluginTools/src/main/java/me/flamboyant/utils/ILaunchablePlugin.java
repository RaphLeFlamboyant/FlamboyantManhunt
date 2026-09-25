package me.flamboyant.utils;

public interface ILaunchablePlugin extends IParametrable {
    boolean start();
    boolean stop();
    boolean isRunning();
    boolean canModifyParametersOnTheFly();
}
