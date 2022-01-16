package me.munch42.loginstreak.utils;

import me.munch42.loginstreak.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ChatUtils {

    public static void broadcast(String msg){
        for(Player p : Bukkit.getOnlinePlayers()){
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    public static String parseColourCodes(String msg){
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        return msg;
    }

    public static void sendError(String error){
        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + error);
    }

    //                                         Plugin, Name of Node for String, Hashmap with a key of the placeholder such as %days% and a value of the value to replace such as the total days.
    public static void sendConfigurableMessage (Main plugin, String configName, HashMap<String, Object> placeholders, Player p){
        String message = plugin.getConfig().getString(configName);

        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholderText = entry.getKey();
            Object placeholderValue = entry.getValue();

            message = message.replace(placeholderText, String.valueOf(placeholderValue));
        }

        message = ChatUtils.parseColourCodes(message);
        p.sendMessage(message);
    }

    public static void sendConfigurableMessage (Main plugin, String configName, Player p){
        String message = plugin.getConfig().getString(configName);

        message = ChatUtils.parseColourCodes(message);
        p.sendMessage(message);
    }
}
