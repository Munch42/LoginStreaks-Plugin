package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StreakCommand implements CommandExecutor {

    private Main plugin;

    public StreakCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("streak").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.DARK_RED + "[LoginStreaks] You must be a player to use the /streak command.");
            return true;
        }

        Player p = (Player) sender;

        if(p.hasPermission(plugin.getConfig().getString("streakPerm"))){
            int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");

            String message = plugin.getConfig().getString("streakMessage");
            message = message.replace("%days%", String.valueOf(daysTotal));
            message = message.replace("%player%", p.getDisplayName());
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
            return true;
        } else {
            String message = plugin.getConfig().getString("noPermsMessage");
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
        }

        return false;
    }
}
