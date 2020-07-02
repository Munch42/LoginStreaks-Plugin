package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoginStreakCommand implements CommandExecutor {
    private Main plugin;

    public LoginStreakCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("loginstreak").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission(plugin.getConfig().getString("loginstreakPerm"))){
            if(args.length > 0){
                if(args[0].equalsIgnoreCase("reload")){
                    if(sender.hasPermission(plugin.getConfig().getString("loginstreakReloadPerm"))) {
                        plugin.reloadAllConfigs();
                        sender.sendMessage(ChatColor.GREEN + "LoginStreaks Successfully Reloaded!");
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to run the reload command!");
                        return true;
                    }
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown Argument: " + args[0]);
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Did you mean " + ChatColor.BOLD + "/loginstreak reload" + ChatColor.RED + "?");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to run the LoginStreak command!");
            return true;
        }
    }
}
