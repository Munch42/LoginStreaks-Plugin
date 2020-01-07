package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TopStreaksCommand implements CommandExecutor {

    private Main plugin;

    public TopStreaksCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("topstreaks").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission(plugin.getConfig().getString("streakLBPerm"))){
            if(!plugin.getConfig().getString("streakLBPerm").equals("")){
                sender.sendMessage(ChatUtils.parseColourCodes(plugin.getConfig().getString("streakTopMessage")));
            }

            int counter = 1;
            for(String player : plugin.top10Players){
                String name = plugin.getStreaksConfig().getString("players." + player + ".name");
                int streak = plugin.getStreaksConfig().getInt("players." + player + ".totalStreakDays");
                sender.sendMessage(ChatUtils.parseColourCodes(plugin.topColourCodes + counter + ". " + name + ": " + streak));

                counter++;
            }
        }

        return true;
    }
}
