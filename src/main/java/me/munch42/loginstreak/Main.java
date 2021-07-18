package me.munch42.loginstreak;

import me.munch42.loginstreak.commands.*;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    private File continuityFile = new File(getDataFolder(), "continuity.yml");
    private FileConfiguration continuityConfig = YamlConfiguration.loadConfiguration(continuityFile);

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

        saveDefaultConfig();

        if(setupEconomy()){
            if(!checkEconomy() && getConfig().getBoolean("economy")){
                // In here this will be run if they have vault but no economy plugin. Can add && to the check to see if a option in the plugin.yml is set.
                log.severe(String.format("[%s] - Disabled due to no Economy plugin found! If you do not require the plugin's economy features, change the 'economy' section in the config.yml to false", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        if(!streaksFile.exists()){
            saveResource("streaks.yml", false);
        }

        if(!ranksFile.exists()){
            saveResource("ranks.yml", false);
        }

        if(!continuityFile.exists()){
            saveResource("continuity.yml", false);
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

    public void saveContinuity(){
        // This should save the updated config.yml file from anywhere into the actual yml file.
        try{
            getContinuityConfig().save(getContinuityFile());
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
        continuityConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "continuity.yml"));
    }

    public boolean backupFiles(){
        File streakFile = getStreaksFile();
        File rankFile = getRanksFile();
        File configFile = new File(getDataFolder(), "config.yml");;
        File continuityFile = getContinuityFile();

        File dest = new File(getDataFolder(), "backups/streaks.yml");
        File dest2 = new File(getDataFolder(), "backups/ranks.yml");
        File dest3 = new File(getDataFolder(), "backups/config.yml");
        File dest4 = new File(getDataFolder(), "backups/continuity.yml");

        // If the backup location doesn't exist, we create it.
        if(!dest.exists() && backupCheck(streakFile)) {
            dest.mkdirs();
        }
        if(!dest2.exists() && backupCheck(rankFile)){
            dest2.mkdirs();
        }
        if(!dest3.exists() && backupCheck(configFile)) {
            dest3.mkdirs();
        }
        if(!dest4.exists() && backupCheck(continuityFile)){
            dest4.mkdirs();
        }

        // We then copy the files over to the backup location.
        try {
            if (backupCheck(streakFile)) Files.copy(streakFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (backupCheck(rankFile)) Files.copy(rankFile.toPath(), dest2.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (backupCheck(configFile)) Files.copy(configFile.toPath(), dest3.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (backupCheck(continuityFile)) Files.copy(continuityFile.toPath(), dest4.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException err) {
            return false;
        }
    }

    private boolean backupCheck(File fileToCheck){
        String filename = fileToCheck.getName().replace(".yml", "");
        //Bukkit.getConsoleSender().sendMessage(filename);

        // This is the name of the node we need to check in the config file.
        // The nodes are named for example: streaksBackup so we add the streaks.yml name stripped of the .yml to Backup to get the node.
        String configVarName = filename + "Backup";

        // We return the value of the boolean for that file to see if we back it up or not.
        return getConfig().getBoolean(configVarName);
    }

    public String getTimeLeft(Player p){
        long lastStreakTime = getStreaksConfig().getLong("players." + p.getUniqueId() + ".lastStreakTime");
        long nextStreakTime = lastStreakTime;

        LocalDate nextStreakLocal;

        if(getConfig().getBoolean("defaultStreakSystem")){
            // Add 1 day from the last streak.
            nextStreakTime = lastStreakTime + 86400000;

            // Convert to LocalDate
            nextStreakLocal = new Timestamp(nextStreakTime).toLocalDateTime().toLocalDate();
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

            nextStreakLocal = nextDateLocal;
        } else {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.DARK_RED + "[LoginStreaks] Streak Counting System was set incorrectly. Please set it to either \"true\" or \"false\"");
            return "";
        }

        long hoursLeft;
        long minutesLeft;

        // https://mkyong.com/java8/java-8-difference-between-two-localdate-or-localdatetime/

        if(!getConfig().getBoolean("defaultStreakSystem")) {
            hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), nextStreakLocal.atStartOfDay());
            minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), nextStreakLocal.atStartOfDay());
        } else {
            hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), new Timestamp(nextStreakTime).toLocalDateTime());
            minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), new Timestamp(nextStreakTime).toLocalDateTime());
        }

        minutesLeft = minutesLeft - (hoursLeft * 60);

        String baseMessage = getConfig().getString("timeLeftMessage");

        if(baseMessage == ""){
            return "";
        }

        baseMessage = baseMessage.replace("%hoursLeft%", String.valueOf(hoursLeft));
        baseMessage = baseMessage.replace("%minutesLeft%", String.valueOf(minutesLeft));
        baseMessage = ChatUtils.parseColourCodes(baseMessage);

        return baseMessage;
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

    public FileConfiguration getContinuityConfig() { return continuityConfig; }

    public File getContinuityFile() {
        return continuityFile;
    }

    public Main getPlugin(){
        return plugin;
    }
}
