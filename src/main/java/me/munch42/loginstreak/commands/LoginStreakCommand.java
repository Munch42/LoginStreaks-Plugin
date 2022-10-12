package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import me.munch42.loginstreak.tabcompleters.LoginStreakTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginStreakCommand implements CommandExecutor {
    private Main plugin;

    public LoginStreakCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("loginstreak").setExecutor(this);
        plugin.getCommand("loginstreak").setTabCompleter(new LoginStreakTabCompleter(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission(plugin.getConfig().getString("loginstreakPerm"))){
            if(args.length > 0){
                if(args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission(plugin.getConfig().getString("loginstreakReloadPerm"))) {
                        plugin.reloadAllConfigs();
                        sender.sendMessage(ChatColor.GREEN + "LoginStreaks Successfully Reloaded!");
                    } else {
                        String message = plugin.getConfig().getString("noPermsMessage");
                        message = ChatUtils.parseColourCodes(message);
                        sender.sendMessage(message);
                        return true;
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("backup")){
                    String permission = "loginstreaks.loginstreak.backup";

                    if(plugin.getConfig().getString("loginstreakBackupPerm") != null){
                        permission = plugin.getConfig().getString("loginstreakBackupPerm");
                    }

                    if(sender.hasPermission(permission)) {
                        boolean backedUp = plugin.backupFiles();

                        if(backedUp){
                            sender.sendMessage(ChatColor.GREEN + "Files successfully backed up!");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.DARK_RED + "There was an error backing up your files!");
                            return false;
                        }
                    } else {
                        if(!plugin.getConfig().getString("noPermsMessage").equals("")) {
                            String message = plugin.getConfig().getString("noPermsMessage");
                            message = ChatUtils.parseColourCodes(message);

                            sender.sendMessage(message);
                        }

                        return true;
                    }
                } else if(args[0].equalsIgnoreCase("setstreak")) {
                    if (sender.hasPermission(plugin.getConfig().getString("loginstreakSetStreakPerm"))) {
                        if (args.length >= 3) {
                            String playerName = args[1];
                            String days = args[2];

                            Player p = Bukkit.getPlayer(playerName);
                            if (p == null) {
                                sender.sendMessage(ChatColor.RED + "There was an error finding the player specified, are you sure they are online?");
                                return true;
                            }

                            if(!isNumeric(days)){
                                sender.sendMessage(ChatColor.RED + "There was an error with the specified amount of days, are you sure it is a number?");
                                return true;
                            }

                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", Integer.valueOf(days));

                            if (args.length >= 4) {
                                String giveReward = args[3];

                                if (giveReward.equalsIgnoreCase("true")) {
                                    plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                                }
                            }

                            plugin.saveConfig();
                            sender.sendMessage(ChatColor.GREEN + "Successfully Set " + playerName + "'s Streak!");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "The correct usage of this command is: " + ChatColor.BOLD + "/loginstreak setstreak <player> <days> [giveReward]");
                            sender.sendMessage(ChatColor.RED + "[giveReward] is optional and if used and set to 'true' will give the player the specified day reward when they next login.");
                            return true;
                        }
                    } else {
                        String message = plugin.getConfig().getString("noPermsMessage");
                        message = ChatUtils.parseColourCodes(message);
                        sender.sendMessage(message);
                        return true;
                    }
                } else if(args[0].equalsIgnoreCase("resetstreak")){
                    if (sender.hasPermission(plugin.getConfig().getString("loginstreakResetStreakPerm"))) {
                        if (args.length >= 2) {
                            String playerName = args[1];

                            Player p = Bukkit.getPlayer(playerName);
                            if (p == null) {
                                sender.sendMessage(ChatColor.RED + "There was an error finding the player specified, are you sure they are online?");
                                return true;
                            }

                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", 1);
                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                            plugin.saveConfig();

                            sender.sendMessage(ChatColor.GREEN + "Successfully Reset " + playerName + "'s Streak!");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "The correct usage of this command is: " + ChatColor.BOLD + "/loginstreak resetstreak <player>");
                            return true;
                        }
                    } else {
                        String message = plugin.getConfig().getString("noPermsMessage");
                        message = ChatUtils.parseColourCodes(message);
                        sender.sendMessage(message);
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown Argument: " + args[0]);
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Did you mean " + ChatColor.BOLD + "/loginstreak reload" + ChatColor.RED + "?");
                sender.sendMessage(ChatColor.RED + "Did you mean " + ChatColor.BOLD + "/loginstreak resetstreak" + ChatColor.RED + "?");
                sender.sendMessage(ChatColor.RED + "Did you mean " + ChatColor.BOLD + "/loginstreak setstreak" + ChatColor.RED + "?");
                sender.sendMessage(ChatColor.RED + "Did you mean " + ChatColor.BOLD + "/loginstreak backup" + ChatColor.RED + "?");
                return true;
            }
        } else {
            String message = plugin.getConfig().getString("noPermsMessage");
            message = ChatUtils.parseColourCodes(message);
            sender.sendMessage(message);
            return true;
        }
    }

    private static boolean isNumeric(String strNum){
        if(strNum == null){
            return false;
        }

        try{
            int i = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe){
            return false;
        }

        return true;
    }
}
