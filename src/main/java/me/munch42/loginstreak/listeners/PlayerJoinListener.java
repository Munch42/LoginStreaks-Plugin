package me.munch42.loginstreak.listeners;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import me.munch42.loginstreak.utils.StringUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Array;
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
        boolean manualPlaytimeClaim = false;
        boolean manualClaiming = false;

        if(event.getJoinMessage().equalsIgnoreCase("Claiming for Playtime") && plugin.getConfig().getBoolean("playtimeManualClaim")){
            // If this is true, we tell them when it has reached the required playtime when it realizes a node has the playtime section and the playtime has been met.
            // This is just the reminder. When the play time scheduler sends the message to claim it for that player, it comes here, and we check if they need to manually claim it. If so, we tell them here.
            manualPlaytimeClaim = true;
        }

        // If we receive the call to run this from the play time scheduler meaning that someone can claim their reward based on their playtime, we then don't break out and run this as normal.
        // This is only if auto claiming for this is enabled since otherwise they will manually have to claim it, and we can manually call to check playtime above.
        boolean playtimeClaimEvent = event.getJoinMessage().equalsIgnoreCase("Claiming for Playtime") && !plugin.getConfig().getBoolean("playtimeManualClaim");

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

        // Save current playtime on every join:
        plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".playtime", event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE));

        plugin.saveConfig();

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
                //System.out.println("True rewards");
            } else if(System.currentTimeMillis() > lastStreakTime + twoDays){
                giveReward = false;
                moreThanTwoDays = true;
                //System.out.println("False rewards");
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

            LocalDate nextDateLocal = dateNextDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate originalDateLocal = dateSelectedFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            if(LocalDate.now().isEqual(nextDateLocal)){
                giveReward = true;
            } else if(LocalDate.now().isEqual(originalDateLocal)){
                giveReward = false;
                moreThanTwoDays = false;
            } else {
                giveReward = false;
                moreThanTwoDays = true;
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
            //System.out.println("True rewards 2");
        } else if(moreThanTwoDays) {
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".totalStreakDays", 1);
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".lastStreakTime", System.currentTimeMillis());
            plugin.getStreaksConfig().set("players." + event.getPlayer().getUniqueId() + ".dayReward", false);
            plugin.saveConfig();
            //System.out.println("False rewards 2");
        }

        // Moved from above to here so that we can remind the player to claim x day after updating to the new one.
        // Use the join message to differentiate between the event created by the streak command and a normal player joining.
        // So if this event is not from the command AND manual claiming is on, then we just return out of this.
        if(!event.getJoinMessage().equalsIgnoreCase("Player Claimed Reward") && plugin.getConfig().getBoolean("manualStreakClaiming")){
            if(!manualPlaytimeClaim){
                // Send a reminder to the player to manually claim their streak if manual claiming is on.
                int daysTotal = plugin.getStreaksConfig().getInt("players." + event.getPlayer().getUniqueId() + ".totalStreakDays");

                // If there is no reward for the day they are on, just continue on and claim it for them. No reminder is sent.
                if(getListOfAllRewardDaysForPlayer().contains(String.valueOf(daysTotal))) {
                    HashMap<String, Object> placeholders = new HashMap<String, Object>();
                    placeholders.put("%days%", daysTotal);

                    ChatUtils.sendConfigurableMessage(plugin, "streakManualClaimReminder", placeholders, event.getPlayer());
                    return;
                }
            }
        }

        if(plugin.getConfig().getBoolean("manualStreakClaiming")){
            manualClaiming =  true;
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
        boolean playtimeUsed = false;

        boolean useWeights = plugin.getConfig().getBoolean("weights");
        //      Permission, Weight
        HashMap<String, Integer> permissionWeights = new HashMap<>();

        // Done: For the permission system add a permission weight option meaning that the highest weight they have is the one they get
        // Done: Or make it so that they get them all or add an option for either of these (Best option)

        if (plugin.getStreaksConfig().getBoolean("players." + p.getUniqueId() + ".dayReward") == true) {
            return;
        }

        // Here we check if their inventory is full and if it is, then we return, leaving this event and remind the player that their inventory is full and that they should probably empty it.
        if (p.getInventory().firstEmpty() == -1 && plugin.getConfig().getBoolean("checkPlayerInventoryCapacity")){
            // If the player used the command to claim their reward, then they they will already have had their inventory checked and may have forced it so we can bypass it here.
            if (!event.getJoinMessage().equalsIgnoreCase("Player Claimed Reward")) {
                ChatUtils.sendConfigurableMessage(plugin, "fullInventoryOnJoinMessage", p);

                return;
            }
        }

        // Here we add to the backup count or backup the files if the threshold has been reached.
        // We do it here because we have checked to see if this is a unique (unique as it was defined in config.yml) login.
        // Done: Will have to add another file for the backup information such as backupcount as when it is in the main config file it saves weirdly removing all comments when it saves which isn't what we want.

        if(plugin.getConfig().getBoolean("backup")){
            int backupCount = plugin.getContinuityConfig().getInt("backupCount");
            int backupThreshold = plugin.getConfig().getInt("backupThreshold");

            // If we have reached or exceeded the backup threshold then we want to back up the files.
            if (backupCount >= backupThreshold){
                // Here we want to complete a back up.
                boolean backedUp = plugin.backupFiles();

                if(backedUp){
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[LoginStreak] Files Backed Up!");

                    // We reset to 0 to reset so we don't keep backing up.
                    plugin.getContinuityConfig().set("backupCount", 0);
                    plugin.saveContinuity();
                } else {
                    // Because it failed we won't reset count to 0 so that it will try again on next player join.
                    Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreak] File Backup Failed!");
                }
            } else {
                int newCount = backupCount + 1;
                plugin.getContinuityConfig().set("backupCount", newCount);
                plugin.saveContinuity();
            }
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

                            if(checkPlaytimeUsed(p, permDayRewards, rewardKey, playtimeClaimEvent, manualClaiming)){
                                break;
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

                            if(checkPlaytimeUsed(p, permissionSection, rewardKey, playtimeClaimEvent, manualClaiming)){
                                break;
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

                        if(checkPlaytimeUsed(p, permissionSection, rewardKey, playtimeClaimEvent, manualClaiming)){
                            break;
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

                if(checkPlaytimeUsed(p, rewards, key, playtimeClaimEvent, manualClaiming)){
                    break;
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
        commandScope = StringUtils.trimSpacesFromStringArray(commandScope);
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
            if(commandExplanation != null) {
                message = message.replace("%command%", commandExplanation);
            } else {
                message = message.replace("%command%", "");
                ChatUtils.sendError("[LoginStreak] ERROR: Command Explanation Not Set in Config File! If not in use, set it to \"\"");
            }
            message = message.replace("%days%", String.valueOf(daysNow));
            message = ChatUtils.parseColourCodes(message);
            p.sendMessage(message);
        }

        plugin.getStreaksConfig().set("players." + p.getUniqueId() + ".dayReward", true);
        plugin.saveConfig();
        return true;
    }

    private boolean checkPlaytimeUsed(Player p, ConfigurationSection configSec, String rewardKey, boolean playtimeClaimEvent, boolean isManualClaim){
        boolean playtimeUsed = false;

        // If playtime used doesn't equal null then it is true since they use it.
        playtimeUsed = configSec.getString(rewardKey + ".playtime") != null;

        if(playtimeUsed) {
            playtimeUsed = !configSec.getString(rewardKey + ".playtime").equalsIgnoreCase("");
        }

        //System.out.println("Break: " + playtimeUsed);

        // If this is the playtime claiming event, we don't want to break out, so we say that playtime is not used to avoid breaking out.
        if (playtimeClaimEvent){
            playtimeUsed = false;
        }

        // If we are manually claiming it, and we need to break since they don't have the playtime, we tell them.
        if(isManualClaim && playtimeUsed){
            HashMap<String, Object> placeholders = new HashMap<String, Object>();
            // Current Time comes from the streak file with the player's UUID and then .playtime converted into hours + mins
            float currentTime = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".playtime");

            int curSecs = Math.round(currentTime / 20);
            int curMins = (curSecs % 3600) / 60;
            int curHours = curSecs / 3600;

            String curTime = curHours + " hour(s) and " + curMins + " minute(s)";
            System.out.println(curTime);

            placeholders.put("%currentTime%", curTime);

            // Required time comes from the rewardKey.playtime converted into hours + mins
            String requiredTime = configSec.getString(rewardKey + ".playtime");
            placeholders.put("%requiredTime%", requiredTime);


            ChatUtils.sendConfigurableMessage(plugin, "playtimeManualClaimWarning", placeholders, p);
        }

        return playtimeUsed;
    }

    private ArrayList<String> getListOfAllRewardDaysForPlayer(){
        // Here we will get any days that the player gets rewards on including permission days.
        ArrayList<String> rewardingDays = new ArrayList<>();

        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("rewards");

        // We add all the reward keys since every one is a day they could receive a reward.
        rewardingDays.addAll(rewards.getKeys(false));

        // TODO: Add the permission rewards for only the ones that apply to this player.

        return rewardingDays;
    }
}
