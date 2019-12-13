package me.munch42.loginstreak.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

}
