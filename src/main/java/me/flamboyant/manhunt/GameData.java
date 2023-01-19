package me.flamboyant.manhunt;

import me.flamboyant.manhunt.roles.AManhuntRole;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class GameData {
    public static HashMap<Player, AManhuntRole> playerClassList;
    public static HashMap<Player, Location> overworldLocationBeforePortal = new HashMap<>();
    public static HashMap<Player, Location> netherLocationBeforePortal = new HashMap<>();
    public static int remainingSpeedrunner;
}
