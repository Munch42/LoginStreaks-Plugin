package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.tabcompleters.LoginStreakTabCompleter;
import me.munch42.loginstreak.tabcompleters.StreakTabCompleter;
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
        plugin.getCommand("streak").setTabCompleter(new StreakTabCompleter());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.DARK_RED + "[LoginStreaks] You must be a player to use the /streak command.");
            return true;
        }

        Player p = (Player) sender;

        if(p.hasPermission(plugin.getConfig().getString("streakPerm"))){
            if(args.length > 0) {
                if(args[0].equalsIgnoreCase("claim")) {
                    if (sender.hasPermission(plugin.getConfig().getString("claimPerm"))) {

                        // TODO: Claim their daily here!
                        // Perhaps add a thing so that they will be warned if they have a full inventory and if they do, they can still run it and claim it but they will have to maybe do like "/stk claim force"

                        sender.sendMessage(ChatColor.GREEN + "You have successfully claimed your daily reward!");
                    } else {
                        String message = plugin.getConfig().getString("noPermsMessage");
                        message = ChatUtils.parseColourCodes(message);
                        sender.sendMessage(message);
                        return true;
                    }
                    return true;
                }
            } else {
                int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");

                String message = plugin.getConfig().getString("streakMessage");
                message = message.replace("%days%", String.valueOf(daysTotal));
                message = message.replace("%player%", p.getDisplayName());
                message = ChatUtils.parseColourCodes(message);
                p.sendMessage(message);
                return true;
            }
        } else {
            String message = plugin.getConfig().getString("noPermsMessage");
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
        }

        return false;
    }
}
