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
import org.bukkit.configuration.ConfigurationSection;
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
                                    //p.sendMessage("1");
                                    return false;
                                }
                            } else {
                                ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimFull", p);
                                //p.sendMessage("2");
                                return false;
                            }
                        }

                        // We send the message before claiming so that on reset days, it still outputs the right day that you claimed.
                        // Added a function for sending these messages where you input the player, message, and the things you want to replace in each message.
                        int daysTotal = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".totalStreakDays");
                        HashMap<String, Object> placeholders = new HashMap<String, Object>();
                        placeholders.put("%days%", daysTotal);

                        ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimMessage", placeholders, p);

                        // Complete: Claim their daily here!
                        // This seems to run the PlayerJoinEvent in the PlayerJoinListener as if a player had joined
                        //p.sendMessage("3");
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
                        //p.sendMessage("4");
                        //p.sendMessage("5");
                    } else {
                        ChatUtils.sendConfigurableMessage(plugin, "noPermsMessage", p);
                        return true;
                    }
                    return true;
                } else {
                    ConfigurationSection playerStreaks = plugin.getStreaksConfig().getConfigurationSection("players");

                    // Loop through all the player's streaks and check to see if the name matches the name you want to find. If so, send their streak, if not send the message saying there was no one by that name.
                    for(String key : playerStreaks.getKeys(false)) {
                        if (args[0].equals(playerStreaks.getString(key + ".name"))){
                            if (!p.hasPermission(plugin.getConfig().getString("otherStreakPerm"))){
                                // If they don't have the permission, then we return/break.
                                ChatUtils.sendConfigurableMessage(plugin, "noPermsMessage", p);

                                return false;
                            }

                            int streakDays = playerStreaks.getInt(key + ".totalStreakDays");
                            HashMap<String, Object> placeholders = new HashMap<String, Object>();
                            placeholders.put("%days%", streakDays);
                            placeholders.put("%player%", playerStreaks.getString(key + ".name"));

                            ChatUtils.sendConfigurableMessage(plugin, "otherPlayerStreakMessage", placeholders, p);

                            return true;
                        }
                    }

                    // We also do it out here so that in the case that the name doesn't exist, they still get the no perm message and we don't tell them that the player doesn't exist.
                    if (!p.hasPermission(plugin.getConfig().getString("otherStreakPerm"))){
                        // If they don't have the permission, then we return/break.
                        ChatUtils.sendConfigurableMessage(plugin, "noPermsMessage", p);

                        return false;
                    }

                    // This sends a message saying that no one exists with the input name.
                    HashMap<String, Object> placeholders = new HashMap<String, Object>();
                    placeholders.put("%player%", args[0]);

                    ChatUtils.sendConfigurableMessage(plugin, "otherPlayerNoStreakMessage", placeholders, p);
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
