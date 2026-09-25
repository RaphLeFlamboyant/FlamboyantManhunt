package me.flamboyant.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChatHelper {
    public static String feedback(String message) {
        return ChatColor.GREEN + "¤" + ChatColor.RESET + message;
    }

    public static String importantMessage(String message) {
        return "" + ChatColor.GOLD + message;
    }

    public static String errorMessage(String message) {
        return "" + ChatColor.RED + message;
    }

    public static String titledMessage(String title, String message) {
        return "" + ChatColor.AQUA + ChatColor.BOLD + "[" + title + "]" + ChatColor.RESET + message;
    }

    public static String highlightMessage(String messageToBuild, String[] highlightedParts) {
        String color = ChatColor.getLastColors(messageToBuild);
        return String.format(messageToBuild,
                Arrays.stream(highlightedParts).map(s -> "" + ChatColor.RESET + ChatColor.UNDERLINE + ChatColor.GREEN + s + ChatColor.RESET + color).collect(Collectors.toList()));
    }
}
