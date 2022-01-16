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

import java.util.HashMap;

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

                        if (p.getInventory().firstEmpty() == -1){
                            // Their inventory is empty
                            // TODO: Edit this to be configurable
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Your inventory is full. If you would still like to claim your reward, please re-run the command as '/streak claim force'");
                            return false;
                        }

                        // Added a function for sending these messages where you input the player, message, and the things you want to replace in each message.
                        int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");
                        HashMap<String, Object> placeholders = new HashMap<String, Object>();
                        placeholders.put("%days%", daysTotal);

                        ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimMessage", placeholders, p);
                    } else {
                        ChatUtils.sendConfigurableMessage(plugin, "noPermsMessage", p);
                        return true;
                    }
                    return true;
                }
            } else {
                int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");
                HashMap<String, Object> placeholders = new HashMap<String, Object>();
                placeholders.put("%days%", daysTotal);
                placeholders.put("%player%", p.getDisplayName());

                ChatUtils.sendConfigurableMessage(plugin, "streakMessage", placeholders, p);
                return true;
            }
        } else {
            ChatUtils.sendConfigurableMessage(plugin, "noPermsMessage", p);
        }

        return false;
    }
}
