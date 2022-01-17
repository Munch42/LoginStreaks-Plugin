package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.listeners.PlayerJoinListener;
import me.munch42.loginstreak.tabcompleters.LoginStreakTabCompleter;
import me.munch42.loginstreak.tabcompleters.StreakTabCompleter;
import me.munch42.loginstreak.utils.ChatUtils;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
                        // Perhaps add a thing so that they will be warned if they have a full inventory and if they do, they can still run it and claim it but they will have to maybe do like "/stk claim force"

                        // This means that they have already received their reward so we don't want it to say they claimed it again when they haven't.
                        if (plugin.getStreaksConfig().getBoolean("players." + p.getUniqueId() + ".dayReward") == true) {
                            int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");
                            HashMap<String, Object> placeholders1 = new HashMap<String, Object>();
                            placeholders1.put("%days%", daysTotal);

                            ChatUtils.sendConfigurableMessage(plugin, "streakAlreadyClaimedMessage", placeholders1, p);

                            return true;
                        }

                        if (p.getInventory().firstEmpty() == -1 && plugin.getConfig().getBoolean("checkPlayerInventoryCapacity")){
                            // Their inventory is full
                            if(args.length > 1) {
                                if (!args[1].equalsIgnoreCase("force")) {
                                    ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimFull", p);
                                    return false;
                                }
                            } else {
                                ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimFull", p);
                                return false;
                            }
                        }

                        // Complete: Claim their daily here!
                        // This seems to run the PlayerJoinEvent in the PlayerJoinListener as if a player had joined
                        ArrayList<RegisteredListener> rls = HandlerList.getRegisteredListeners(plugin);
                        for (RegisteredListener rl : rls) {
                            if(rl.getListener() instanceof PlayerJoinListener){
                                PlayerJoinEvent test = new PlayerJoinEvent(p, "Player Claimed Reward");

                                try {
                                    rl.callEvent(test);
                                } catch (EventException e) {
                                    e.printStackTrace();
                                }
                            }
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
