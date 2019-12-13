package me.munch42.loginstreak.listeners;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {

    // NOTE: System.currentTimeMillis() returns The time in milliseconds. There are 1000 milliseconds in 1 second. So to turn it into 24 hours the math is 1000 x 60 x 60 x 24 = 86400000

    private Main plugin;

    public PlayerJoinListener(Main plugin){
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event){
        // Save new player data on player join
        if(!plugin.getStreaksConfig().contains("players." + event.getPlayer().getUniqueId())){
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".name", event.getPlayer().getDisplayName());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
        }

        // Save New Display Name if name changed
        if(!plugin.getStreaksConfig().getString("players." + event.getPlayer().getUniqueId() + ".name").equals(event.getPlayer().getDisplayName())){
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".name", event.getPlayer().getDisplayName());
            plugin.saveConfig();
        }

        // See if it is has been less than a day, more than a day, or 1 day since last login.
        long lastStreakTime = plugin.getStreaksConfig().getLong("players." + event.getPlayer().getUniqueId() + ".lastStreakTime");
        int totalDays = plugin.getStreaksConfig().getInt("players." + event.getPlayer().getUniqueId() + ".totalStreakDays");
        long oneDay = 1000 * 60 * 60 * 24;
        long twoDays = 1000 * 60 * 60 * 24 * 2;

        if(System.currentTimeMillis() > lastStreakTime + oneDay && System.currentTimeMillis() < lastStreakTime + twoDays){
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", totalDays + 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
        } else if(System.currentTimeMillis() > lastStreakTime + twoDays) {
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
        }

        // Check for rewards for total streak
        int daysNow = plugin.getStreaksConfig().getInt("players." + event.getPlayer().getUniqueId() + ".totalStreakDays");
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("rewards");

        Player p = event.getPlayer();

        String rewardType = "";
        int moneyAmount = 0;
        String command = "";
        String itemName = "";
        int rewardAmount = 0;
        String commandExplanation = "";
        String commandScope = "";

        for(String key : rewards.getKeys(false)){
            if(key.equals(String.valueOf(daysNow))){
                if(plugin.getStreaksConfig().getBoolean( "players." + event.getPlayer().getUniqueId() + ".dayReward") == true){
                    break;
                }
                rewardType = rewards.getString(key + ".rewardType");
                if(rewardType.equals("MONEY")) {
                    moneyAmount = rewards.getInt(key + ".reward");
                    EconomyResponse r = Main.getEconomy().depositPlayer(p, moneyAmount);
                    if (r.transactionSuccess()) {
                        // Main.getEconomy().format(r.amount)
                        if (!plugin.getConfig().getString("rewardMoneyMessage").equals("")) {
                            String message = plugin.getConfig().getString("rewardMoneyMessage");
                            message = message.replace("%money%", Main.getEconomy().format(r.amount));
                            message = message.replace("%days%", String.valueOf(daysNow));
                            message = ChatUtils.parseColourCodes(message);
                            p.sendMessage(message);
                        }
                    } else {
                        p.sendMessage(String.format("An error occured: %s", r.errorMessage));
                    }

                    plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", true);
                    plugin.saveConfig();
                    break;
                } else if(rewardType.equals("ITEM")){
                    itemName = rewards.getString(key + ".reward");
                    String itemDisplayName = itemName.replace("_", " ");
                    itemDisplayName = itemDisplayName.toLowerCase();
                    itemDisplayName = WordUtils.capitalize(itemDisplayName);
                    rewardAmount = rewards.getInt(key + ".rewardAmount");

                    if(rewardAmount != 0){
                        ItemStack item = new ItemStack(Material.getMaterial(itemName), rewardAmount);
                        p.getInventory().addItem(item);
                        if(!plugin.getConfig().getString("rewardItemMessage").equals("")){
                            String message = plugin.getConfig().getString("rewardItemMessage");
                            message = message.replace("%item%", itemDisplayName);
                            message = message.replace("%itemAmount%", String.valueOf(rewardAmount));
                            message = message.replace("%days%", String.valueOf(daysNow));
                            message = ChatUtils.parseColourCodes(message);
                            p.sendMessage(message);
                        }
                    } else {
                        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Item Reward Amount was not set correctly!");
                    }

                    plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", true);
                    plugin.saveConfig();
                    break;
                } else if(rewardType.equals("COMMAND")){
                    command = rewards.getString(key + ".reward");
                    commandExplanation = rewards.getString(key + ".commandExplanation");
                    commandScope = rewards.getString(key + ".commandScope");

                    command = command.replace("%player%", p.getDisplayName());

                    if(commandScope.equals("PLAYER")){
                        Bukkit.getServer().dispatchCommand(p, command);
                    } else if(commandScope.equals("CONSOLE")){
                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                    } else {
                        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Command Scope was not set correctly!");
                        break;
                    }

                    if(!plugin.getConfig().getString("rewardCommandMessage").equals("")){
                        String message = plugin.getConfig().getString("rewardCommandMessage");
                        message = message.replace("%command%", commandExplanation);
                        message = message.replace("%days%", String.valueOf(daysNow));
                        message = ChatUtils.parseColourCodes(message);
                        p.sendMessage(message);
                    }

                    plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", true);
                    plugin.saveConfig();
                    break;
                } else {
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Reward Type was not set correctly!");
                    break;
                }
            }
        }
    }
}
