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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        final long oneDay = 1000 * 60 * 60 * 24;
        final long twoDays = 1000 * 60 * 60 * 24 * 2;

        // To convert from System.currentTimeMillis() use this:
        // LocalDate localDate = new Timestamp(timeInMillis).toLocalDateTime().toLocalDate();
        // https://www.concretepage.com/java/java-8/convert-between-java-localdate-epoch#Epoch
        // https://beginnersbook.com/2013/04/get-the-previous-day-date-and-next-day-date-from-the-given-date/

        LocalDate localDateLastStreak = new Timestamp(lastStreakTime).toLocalDateTime().toLocalDate();

        boolean giveReward = false;
        boolean moreThanTwoDays = false;

        if(plugin.getConfig().getBoolean("defaultStreakSystem")) {
            if(System.currentTimeMillis() > lastStreakTime + oneDay && System.currentTimeMillis() < lastStreakTime + twoDays){
                giveReward = true;
            } else if(System.currentTimeMillis() > lastStreakTime + twoDays){
                giveReward = false;
                moreThanTwoDays = true;
            }
        } else if(!plugin.getConfig().getBoolean("defaultStreakSystem")) {
            StringBuffer sBuffer = new StringBuffer(localDateLastStreak.toString());
            String year = sBuffer.substring(2,4);
            String mon = sBuffer.substring(5,7);
            String dd = sBuffer.substring(8,10);

            String modifiedFromDate = dd +'/'+mon+'/'+year;
            final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
            Date dateSelectedFrom = null;
            Date dateNextDate = null;
            Date datePreviousDate = null;

            // convert date present in the String to java.util.Date.
            try
            {
                dateSelectedFrom = dateFormat.parse(modifiedFromDate);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            //get the next date in String.
            String nextDate = dateFormat.format(dateSelectedFrom.getTime() + MILLIS_IN_DAY);

            //get the previous date in String.
            String previousDate = dateFormat.format(dateSelectedFrom.getTime() - MILLIS_IN_DAY);

            //get the next date in java.util.Date.
            try
            {
                dateNextDate = dateFormat.parse(nextDate);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            //get the previous date in java.util.Date.
            try
            {
                datePreviousDate = dateFormat.parse(previousDate);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        } else {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreaks] Streak Counting System was set incorrectly. Please set it to either \"true\" or \"false\"");
            return;
        }

        if(giveReward){
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", totalDays + 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
        } else if(moreThanTwoDays) {
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
        }

        int daysNow = plugin.getStreaksConfig().getInt("players." + event.getPlayer().getUniqueId() + ".totalStreakDays");
        Player p = event.getPlayer();

        // Update Ranking Performance Friendly
        plugin.getRanksConfig().set("topPlayers." + p.getUniqueId() + ".rank", 1);
        plugin.saveRanksConfig();
        plugin.streakMap.put(p.getUniqueId().toString(), daysNow);

        ConfigurationSection ranks = plugin.getRanksConfig().getConfigurationSection("topPlayers");

        for(String key : ranks.getKeys(false)){
            if(key.equals(p.getUniqueId().toString())){
                continue;
            }

            int theirDays = plugin.getStreaksConfig().getInt("players." + key + ".totalStreakDays");

            plugin.streakMap.put(key, theirDays);
        }

        plugin.checkAndUpdateRankings();

        // Check for rewards with permissions
        ConfigurationSection permRewards = plugin.getConfig().getConfigurationSection("permissionRewards");

        String rewardType;
        int moneyAmount = 0;
        int[] rewardAmount = new int[arraySize];
        String commandExplanation = "";
        boolean reset = false;
        boolean permRewardGiven = false;

        boolean useWeights = plugin.getConfig().getBoolean("weights");
        //      Permission, Weight
        HashMap<String, Integer> permissionWeights = new HashMap<>();

        // Done: For the permission system add a permission weight option meaning that the highest weight they have is the one they get
        // Done: Or make it so that they get them all or add an option for either of these (Best option)

        if (plugin.getStreaksConfig().getBoolean("players." + p.getUniqueId() + ".dayReward") == true) {
            return;
        }

        for(String key : permRewards.getKeys(false)) {
            String keyPermission = key.replace(";", ".");

            if(event.getPlayer().hasPermission(keyPermission)) {
                ConfigurationSection permDayRewards = plugin.getConfig().getConfigurationSection("permissionRewards." + key);

                if(useWeights){
                    permissionWeights.put(key, plugin.getConfig().getInt("permissionRewards." + key + ".weight"));
                } else {
                    for (String rewardKey : permDayRewards.getKeys(false)) {
                        // In here, this means they have the given permission and now we need to do the same logic as below, checking if they get a rewards and giving it.

                        if (rewardKey.equals(String.valueOf(daysNow))) {

                            rewardType = permDayRewards.getString(rewardKey + ".rewardType");

                            reset = permDayRewards.getBoolean(rewardKey + ".reset");

                            if (reset) {
                                plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", 1);
                                plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                                plugin.saveConfig();
                            }

                            if (rewardType.equals("MONEY")) {
                                rewardMoney(moneyAmount, permDayRewards, rewardKey, daysNow, p);
                                permRewardGiven = true;
                            } else if (rewardType.equals("ITEM")) {
                                rewardItems(permDayRewards, rewardKey, rewardAmount, p, daysNow);
                                permRewardGiven = true;
                            } else if (rewardType.equals("COMMAND")) {
                                rewardCommands(permDayRewards, rewardKey, commandExplanation, daysNow, p);
                                permRewardGiven = true;
                            } else {
                                Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Reward Type was not set correctly!");
                                break;
                            }
                        }
                    }
                }
            }
        }

        Integer topWeight = 0;
        Integer count = 0;
        String topPerm = "";

        ArrayList<String> equalWeightPermissions = new ArrayList<String>();

        if(useWeights && permissionWeights.size() != 0){
            for(Map.Entry<String, Integer> entry : permissionWeights.entrySet()){
                String permission = entry.getKey();
                Integer weight = entry.getValue();

                if(count == 0){
                    topWeight = weight;
                    topPerm = permission;
                } else {
                    if(weight > topWeight) {
                        topWeight = weight;
                        topPerm = permission;

                        if(equalWeightPermissions.size() != 0){
                            equalWeightPermissions.clear();
                        }
                    } else if (weight.equals(topWeight)){
                        if(equalWeightPermissions.size() == 0) {
                            equalWeightPermissions.add(topPerm);
                            equalWeightPermissions.add(permission);
                        } else {
                            equalWeightPermissions.add(permission);
                        }
                    }
                }

                count++;
            }

            if(equalWeightPermissions.size() != 0){
                for(String perm : equalWeightPermissions){
                    ConfigurationSection permissionSection = plugin.getConfig().getConfigurationSection("permissionRewards." + perm);

                    for (String rewardKey : permissionSection.getKeys(false)) {
                        // In here, this means they have the given permission and now we need to do the same logic as below, checking if they get a rewards and giving it.

                        if (rewardKey.equals(String.valueOf(daysNow))) {

                            rewardType = permissionSection.getString(rewardKey + ".rewardType");

                            reset = permissionSection.getBoolean(rewardKey + ".reset");

                            if (reset) {
                                plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", 1);
                                plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                                plugin.saveConfig();
                            }

                            if (rewardType.equals("MONEY")) {
                                rewardMoney(moneyAmount, permissionSection, rewardKey, daysNow, p);
                                permRewardGiven = true;
                            } else if (rewardType.equals("ITEM")) {
                                rewardItems(permissionSection, rewardKey, rewardAmount, p, daysNow);
                                permRewardGiven = true;
                            } else if (rewardType.equals("COMMAND")) {
                                rewardCommands(permissionSection, rewardKey, commandExplanation, daysNow, p);
                                permRewardGiven = true;
                            } else {
                                Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Reward Type was not set correctly!");
                                break;
                            }
                        }
                    }
                }
            } else {
                ConfigurationSection permissionSection = plugin.getConfig().getConfigurationSection("permissionRewards." + topPerm);

                for (String rewardKey : permissionSection.getKeys(false)) {
                    // In here, this means they have the given permission and now we need to do the same logic as below, checking if they get a rewards and giving it.

                    if (rewardKey.equals(String.valueOf(daysNow))) {

                        rewardType = permissionSection.getString(rewardKey + ".rewardType");

                        reset = permissionSection.getBoolean(rewardKey + ".reset");

                        if (reset) {
                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", 1);
                            plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                            plugin.saveConfig();
                        }

                        if (rewardType.equals("MONEY")) {
                            rewardMoney(moneyAmount, permissionSection, rewardKey, daysNow, p);
                            permRewardGiven = true;
                        } else if (rewardType.equals("ITEM")) {
                            rewardItems(permissionSection, rewardKey, rewardAmount, p, daysNow);
                            permRewardGiven = true;
                        } else if (rewardType.equals("COMMAND")) {
                            rewardCommands(permissionSection, rewardKey, commandExplanation, daysNow, p);
                            permRewardGiven = true;
                        } else {
                            Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] ERROR: Reward Type was not set correctly!");
                            break;
                        }
                    }
                }
            }
        }

        // If the player has been given one or more perm rewards, return and don't give normal rewards.
        if(permRewardGiven){
            return;
        }

        // Check for rewards for total streak in basic rewards
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("rewards");

        for(String key : rewards.getKeys(false)){
            if(key.equals(String.valueOf(daysNow))){
                if(plugin.getStreaksConfig().getBoolean( "players." + p.getUniqueId() + ".dayReward") == true){
                    break;
                }
                rewardType = rewards.getString(key + ".rewardType");

                reset = rewards.getBoolean(key + ".reset");

                if(reset){
                    plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".totalStreakDays", 1);
                    plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", false);
                    plugin.saveConfig();
                }

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
            command[i] = command[i].replace("%player%", p.getName());

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
}
