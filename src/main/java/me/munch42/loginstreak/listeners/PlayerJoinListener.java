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

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class PlayerJoinListener implements Listener {

    // NOTE: System.currentTimeMillis() returns The time in milliseconds. There are 1000 milliseconds in 1 second. So to turn it into 24 hours the math is 1000 x 60 x 60 x 24 = 86400000

    private Main plugin;
    private int arraySize = 100;

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
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".rank", 0);
            plugin.saveConfig();
        }

        // Save New Display Name if name changed
        if(plugin.getStreaksConfig().getString("players." + event.getPlayer().getUniqueId() + ".rank") == null){
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".rank", 0);
            plugin.saveConfig();
        }

        // Save New Rank if no rank node is present
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

        // Update Rankings in Config
        checkAndUpdateRankings();

        // Check for rewards for total streak
        int daysNow = plugin.getStreaksConfig().getInt("players." + event.getPlayer().getUniqueId() + ".totalStreakDays");
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("rewards");

        Player p = event.getPlayer();

        String rewardType;
        int moneyAmount = 0;
        int[] rewardAmount = new int[arraySize];
        String commandExplanation = "";

        for(String key : rewards.getKeys(false)){
            if(key.equals(String.valueOf(daysNow))){
                if(plugin.getStreaksConfig().getBoolean( "players." + p.getUniqueId() + ".dayReward") == true){
                    break;
                }
                rewardType = rewards.getString(key + ".rewardType");

                if(rewardType.equals("MONEY")) {
                    rewardMoney(moneyAmount, rewards, key, daysNow,  p);
                } else if(rewardType.equals("ITEM")){
                    rewardItems(rewards, key, rewardAmount, p, daysNow);
                } else if(rewardType.equals("COMMAND")){
                    rewardCommands(rewards, key, commandExplanation, daysNow, p);
                } else {
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Reward Type was not set correctly!");
                    break;
                }
            }
        }
    }

    private boolean rewardMoney(int moneyAmount, ConfigurationSection rewards, String key, int daysNow, Player p){
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
            return false;
        }

        plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", true);
        plugin.saveConfig();
        return true;
    }

    private boolean rewardItems(ConfigurationSection rewards, String key, int[] rewardAmount, Player p, int daysNow){
        String[] itemDisplayName = new String[arraySize];
        boolean failed = false;
        String[] itemName = rewards.getString(key + ".reward").split(";");
        int counter = 0;
        for(String stringInt : rewards.getString(key + ".rewardAmount").split(";")){
            rewardAmount[counter] = Integer.parseInt(stringInt);
            counter++;
        }

        for(int i = 0; i < itemName.length; i++){
            itemDisplayName[i] = itemName[i].replace("_", " ");
            itemDisplayName[i] = itemDisplayName[i].toLowerCase();
            itemDisplayName[i] = WordUtils.capitalize(itemDisplayName[i]);

            if(rewardAmount[i] != 0){
                ItemStack item = new ItemStack(Material.getMaterial(itemName[i]), rewardAmount[i]);
                p.getInventory().addItem(item);
                if(!plugin.getConfig().getString("rewardItemMessage").equals("")){
                    String message = plugin.getConfig().getString("rewardItemMessage");
                    message = message.replace("%item%", itemDisplayName[i]);
                    message = message.replace("%itemAmount%", String.valueOf(rewardAmount[i]));
                    message = message.replace("%days%", String.valueOf(daysNow));
                    message = ChatUtils.parseColourCodes(message);
                    p.sendMessage(message);
                }
            } else {
                Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Item Reward Amount was not set correctly!");
                failed = true;
                break;
            }
        }

        if(failed == true){
            return false;
        }

        plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", true);
        plugin.saveConfig();
        return true;
    }
    private boolean rewardCommands(ConfigurationSection rewards, String key, String commandExplanation, int daysNow, Player p){
        String[] command = rewards.getString(key + ".reward").split(";");
        String[] commandScope = rewards.getString(key + ".commandScope").split(";");
        commandExplanation = rewards.getString(key + ".commandExplanation");
        boolean failed = false;

        for(int i = 0; i < command.length; i++){
            command[i] = command[i].replace("%player%", p.getDisplayName());

            if(commandScope.length < command.length){
                Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Command Scope did not have enough arguments for the supplied command rewards!");
                return false;
            }

            if(commandScope[i].equals("PLAYER")){
                Bukkit.getServer().dispatchCommand(p, command[i]);
            } else if(commandScope[i].equals("CONSOLE")){
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command[i]);
            } else {
                Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Command Scope was not set correctly!");
                failed = true;
                break;
            }
        }

        if(failed == true){
            return false;
        }

        if(!plugin.getConfig().getString("rewardCommandMessage").equals("")){
            String message = plugin.getConfig().getString("rewardCommandMessage");
            message = message.replace("%command%", commandExplanation);
            message = message.replace("%days%", String.valueOf(daysNow));
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
        }

        plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", true);
        plugin.saveConfig();
        return true;
    }

    private void checkAndUpdateRankings(){
        ConfigurationSection streaks = plugin.getStreaksConfig().getConfigurationSection("players");

        Map<String, Integer> streakMap = new HashMap<>();

        for(String key : streaks.getKeys(false)){
            streakMap.put(key, streaks.getInt(key + ".totalStreakDays", 1));
        }

        Map<String, Integer> sorted = streakMap
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));


        int rank = 1;
        for(String player : sorted.keySet()){
            plugin.getStreaksConfig().set("players." + player + ".rank", rank);
            plugin.saveConfig();

            if(rank <= 10){
                plugin.top10Players.add(player);
            }

            rank++;
        }

    }
}
