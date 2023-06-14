package me.flamboyant.manhunt;

import me.flamboyant.configurable.parameters.*;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.utils.ILaunchablePlugin;
import me.flamboyant.manhunt.roles.GameRolesManagement;
import me.flamboyant.manhunt.roles.ManhuntRoleFactory;
import me.flamboyant.manhunt.roles.ManhuntRoleIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NewManhuntLauncher implements ILaunchablePlugin {
    private boolean running;
    private BooleanParameter resetPlayersStuffParameter = new BooleanParameter(Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
    private BooleanParameter specialRolesOnlyParameter = new BooleanParameter(Material.NETHER_STAR, "Special only", "Random role = special");
    private BooleanParameter surpriseSpeedrunnerParameter = new BooleanParameter(Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
    private IntParameter allyCountParameter;
    private IntParameter speedrunnerCountParameter;
    private int initialMinutesBeforeRoles = 10;
    private IntParameter minutesBeforeRolesParameter = new IntParameter(Material.CLOCK, "Roles time", "Minutes avant annonce rôles", initialMinutesBeforeRoles, 0, 20);

    private HashMap<Player, EnumParameter<ManhuntRoleIdentifier>> playerRoles = new HashMap<>();
    private List<ILaunchablePlugin> optionalPlugin = new ArrayList<>();

    private static NewManhuntLauncher instance;
    public static NewManhuntLauncher getInstance()
    {
        if (instance == null)
        {
            instance = new NewManhuntLauncher();
        }

        return instance;
    }

    protected NewManhuntLauncher()
    {
        setDefaultParameters();
    }

    private void setDefaultParameters() {
        initializePlayerRolesParameter();
    }

    private void initializePlayerRolesParameter() {
        playerRoles.clear();
        for (Player player : Common.server.getOnlinePlayers()) {
            EnumParameter<ManhuntRoleIdentifier> param = new EnumParameter<>(Material.PLAYER_HEAD, player.getDisplayName(), "Select role", ManhuntRoleIdentifier.class);
            param.setIsNullable(true);
            param.setCategory("Players Role");
            playerRoles.put(player, param);
        }
    }

    @Override
    public boolean start() {
        if (running || !Launch()) {
            return false;
        }

        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.start();
        }

        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        if (!running) {
            return false;
        }

        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.stop();
        }

        running = false;
        return true;
    }

    @Override
    public void resetParameters() {
        resetPlayersStuffParameter = new BooleanParameter(Material.CHEST, "Reset stuff", "Reset le stuff au lancement");
        resetPlayersStuffParameter.setCategory("Manhunt Parameters");
        specialRolesOnlyParameter = new BooleanParameter(Material.NETHER_STAR, "Special only", "Random role = special");
        specialRolesOnlyParameter.setCategory("Manhunt Parameters");
        surpriseSpeedrunnerParameter = new BooleanParameter(Material.CREEPER_HEAD, "Hidden Speedrunner", "True = Speedrunner caché avant roles");
        surpriseSpeedrunnerParameter.setCategory("Manhunt Parameters");
        speedrunnerCountParameter = new IntParameter(Material.DIAMOND_BOOTS, "Speedrunners count", "0 = random", 1, 0, Common.server.getOnlinePlayers().size());
        speedrunnerCountParameter.setCategory("Manhunt Parameters");
        allyCountParameter = new IntParameter(Material.GOLDEN_APPLE, "Allies count", "0 = random", 0, 0, Common.server.getOnlinePlayers().size());
        allyCountParameter.setCategory("Manhunt Parameters");
        minutesBeforeRolesParameter = new IntParameter(Material.CLOCK, "Roles time", "Minutes avant annonce rôles", initialMinutesBeforeRoles, 0, 20);
        minutesBeforeRolesParameter.setCategory("Manhunt Parameters");

        for (ILaunchablePlugin plugin : optionalPlugin) {
            plugin.resetParameters();
        }

        setDefaultParameters();
    }

    @Override
    public List<AParameter> getParameters() {
        return getParameters(true);
    }

    private List<AParameter> getParameters(boolean includeNonModifiable) {
        List<AParameter> res = new ArrayList<>();
        if (includeNonModifiable) {
            res.add(resetPlayersStuffParameter);
            res.add(speedrunnerCountParameter);
            res.add(allyCountParameter);
            res.add(specialRolesOnlyParameter);
            res.add(surpriseSpeedrunnerParameter);
            for (Player player : playerRoles.keySet()) {
                res.add(new ValueOfPlayerParameter(playerRoles.get(player), "Role de ", player.getDisplayName(), ""));
            }
        }

        for (ILaunchablePlugin plugin : optionalPlugin) {
            if (includeNonModifiable || plugin.canModifyParametersOnTheFly())
            {
                res.addAll(plugin.getParameters());
            }
        }
        return res;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean canModifyParametersOnTheFly() { return false; }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (dragon.getHealth() - event.getFinalDamage() <= 0) {
            Bukkit.broadcastMessage(ChatHelper.feedback("Le speedrunner a gagné !!!"));
            stop();
        }
    }

    private void resetPlayerState(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(3);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private int getSpeedrunnerCount() {
        return (int) playerRoles.values().stream().filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("SPEEDRUNNER")).count();
    }

    private int getAllyCount() {
        return (int) playerRoles.values().stream().filter(r -> r.getSelectedValue() != null && r.getSelectedValue().toString().contains("ALLY")).count();
    }

    private boolean Launch() {
        int speedrunnerCount = speedrunnerCountParameter.getValue() == 0 ? getSpeedrunnerCount() : speedrunnerCountParameter.getValue();
        int allyCount = allyCountParameter.getValue() == 0 ? getAllyCount() : allyCountParameter.getValue();
        if (!GameRolesManagement.getInstance().setRandomRolesToEmpty(playerRoles, speedrunnerCount, allyCount, specialRolesOnlyParameter.getValue() > 0))
            return false;

        GameData.playerClassList.clear();
        for (Player player : playerRoles.keySet()) {
            GameData.playerClassList.put(player, ManhuntRoleFactory.createRole(player, playerRoles.get(player).getSelectedValue()));
        }

        for (Player player : Common.server.getOnlinePlayers()) {
            if (player == null) continue;
            resetPlayerState(player);
            if (resetPlayersStuffParameter.getValue() != 0) player.getInventory().clear();
        }

        return NewManhuntManager.getInstance().startGame(minutesBeforeRolesParameter.getValue(), surpriseSpeedrunnerParameter.getValue() > 0);
    }
}
