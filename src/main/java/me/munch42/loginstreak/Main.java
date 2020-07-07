package me.munch42.loginstreak;

import me.munch42.loginstreak.commands.LoginStreakCommand;
import me.munch42.loginstreak.commands.StreakCommand;
import me.munch42.loginstreak.commands.TimeLeftCommand;
import me.munch42.loginstreak.commands.TopStreaksCommand;
import me.munch42.loginstreak.listeners.PlayerJoinListener;
import me.munch42.loginstreak.papi.LoginStreakExpansion;
import me.munch42.loginstreak.utils.ChatUtils;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toMap;

public final class Main extends JavaPlugin {

    // NOTE: System.currentTimeMillis() returns The time in milliseconds. There are 1000 milliseconds in 1 second. So to turn it into 24 hours the math is 1000 x 60 x 60 x 24 = 86400000
    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;

    private Main plugin;
    private File streaksFile = new File(getDataFolder(), "streaks.yml");
    private FileConfiguration streaksConfig = YamlConfiguration.loadConfiguration(streaksFile);
    private File ranksFile = new File(getDataFolder(), "ranks.yml");
    private FileConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

    //              Player UUID, Total Streak Days
    public HashMap<String, Integer> streakMap = new HashMap<>();
    public ArrayList<String> top10Players = new ArrayList<>();
    public String placeholderColourCodes;
    public String topColourCodes;

    @Override
    public void onEnable() {
        Metrics metrics = new Metrics(this);
        plugin = this;

        // Possibly not needed as Vault is a hard dependency so crashes the plugin when not found.
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if(setupEconomy()){
            if(!checkEconomy() && getConfig().getBoolean("economy")){
                // In here this will be run if they have vault but no economy plugin. Can add && to the check to see if a option in the plugin.yml is set.
                log.severe(String.format("[%s] - Disabled due to no Economy plugin found! If you do not require the plugin's economy features, change the 'economy' section in the config.yml to false", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        saveDefaultConfig();
        if(!streaksFile.exists()){
            saveResource("streaks.yml", false);
        }

        if(!ranksFile.exists()){
            saveResource("ranks.yml", false);
        }


        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new LoginStreakExpansion(this).register();
        }

        new PlayerJoinListener(this);
        new StreakCommand(this);
        new TopStreaksCommand(this);
        new LoginStreakCommand(this);
        new TimeLeftCommand(this);

        placeholderColourCodes = getConfig().getString("placeholderColourCodes");
        topColourCodes = getConfig().getString("streakTopEntriesColourCode");
    }

    @Override
    public void onDisable() {
    }

    public void saveConfig(){
        try{
            getStreaksConfig().save(getStreaksFile());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void saveRanksConfig(){
        try{
            getRanksConfig().save(getRanksFile());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public String getRankMessage(int rank){
        if(top10Players.size() < rank){
            String message = placeholderColourCodes + rank + ". " + getConfig().getString("blankRankPlaceholder");
            message = ChatUtils.parseColourCodes(message);
            return message;
        }

        String id = top10Players.get(rank - 1);
        String name = plugin.getStreaksConfig().getString("players." + id + ".name");
        String streak = plugin.getStreaksConfig().getString("players." + id + ".totalStreakDays");
        String message = placeholderColourCodes + rank + ". " + name + ": " + streak;
        message = ChatUtils.parseColourCodes(message);
        return message;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        return true;
    }

    private boolean checkEconomy(){
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void checkAndUpdateRankings(){
        Map<String, Integer> sorted = streakMap
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));


        top10Players.clear();
        int rank = 1;
        for(String player : sorted.keySet()){
            if(rank <= 10){
                getRanksConfig().set("topPlayers." + player + ".rank", rank);
                saveRanksConfig();
                top10Players.add(player);
            }

            rank++;
        }

        streakMap.clear();
        ConfigurationSection ranks = getRanksConfig().getConfigurationSection("topPlayers");

        for(String key : ranks.getKeys(false)){
            if(!top10Players.contains(key)){
                getRanksConfig().set("topPlayers." + key, null);
                saveRanksConfig();
            }
        }
    }

    public void reloadAllConfigs(){
        reloadConfig();
        streaksConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "streaks.yml"));
        ranksConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "ranks.yml"));
    }

    public String getTimeLeft(Player p){
        long lastStreakTime = getStreaksConfig().getLong("players." + p.getUniqueId() + ".lastStreakTime");
        long nextStreakTime;

        if(getConfig().getBoolean("defaultStreakSystem")){
            nextStreakTime = lastStreakTime + 86400000;
        } else if(!getConfig().getBoolean("defaultStreakSystem")){
            LocalDate lastStreakLocal = new Timestamp(lastStreakTime).toLocalDateTime().toLocalDate();

            StringBuffer sBuffer = new StringBuffer(lastStreakLocal.toString());
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
        } else {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreaks] Streak Counting System was set incorrectly. Please set it to either \"true\" or \"false\"");
            return "";
        }
    }

    public static Economy getEconomy() {
        return econ;
    }

    public FileConfiguration getStreaksConfig() {
        return streaksConfig;
    }

    public File getStreaksFile(){
        return streaksFile;
    }

    public FileConfiguration getRanksConfig() {
        return ranksConfig;
    }

    public File getRanksFile(){
        return ranksFile;
    }

    public Main getPlugin(){
        return plugin;
    }
}
