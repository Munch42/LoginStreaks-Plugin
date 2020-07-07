package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimeLeftCommand implements CommandExecutor {
    private Main plugin;

    public TimeLeftCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("timeleft").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.DARK_RED + "You must be a " + ChatColor.BOLD + "PLAYER " + ChatColor.DARK_RED + "to run this command!");
            return false;
        }

        Player p = (Player) sender;

        if(p.hasPermission(plugin.getConfig().getString("timeleftPerm"))){
            p.sendMessage(plugin.getTimeLeft(p));
        } else {
            String message = plugin.getConfig().getString("noPermsMessage");
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
            return false;
        }
        return true;
    }
}
