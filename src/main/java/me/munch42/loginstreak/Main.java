package me.munch42.loginstreak;

import me.munch42.loginstreak.commands.StreakCommand;
import me.munch42.loginstreak.commands.TopStreaksCommand;
import me.munch42.loginstreak.listeners.PlayerJoinListener;
import me.munch42.loginstreak.papi.LoginStreakExpansion;
import me.munch42.loginstreak.utils.ChatUtils;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {

    // NOTE: System.currentTimeMillis() returns The time in milliseconds. There are 1000 milliseconds in 1 second. So to turn it into 24 hours the math is 1000 x 60 x 60 x 24 = 86400000
    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;

    private Main plugin;
    private File streaksFile = new File(getDataFolder(), "streaks.yml");
    private FileConfiguration streaksConfig = YamlConfiguration.loadConfiguration(streaksFile);

    public ArrayList<String> top10Players = new ArrayList<>();
    public String placeholderColourCodes;
    public String topColourCodes;

    @Override
    public void onEnable() {
        Metrics metrics = new Metrics(this);
        plugin = this;

        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        if(!streaksFile.exists()){
            saveResource("streaks.yml", false);
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new LoginStreakExpansion(this).register();
        }

        new PlayerJoinListener(this);
        new StreakCommand(this);
        new TopStreaksCommand(this);

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

    public String getRankMessage(int rank){
        if(top10Players.size() < rank){
            String message = placeholderColourCodes + rank + ". " + plugin + getConfig().getString("blankRankPlaceholder");
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
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
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

    public Main getPlugin(){
        return plugin;
    }
}
