package me.munch42.loginstreak.schedulers;

import me.munch42.loginstreak.Main;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayTimeScheduler {
    Main plugin;

    public PlayTimeScheduler(Main plugin){
        if (!plugin.getConfig().getBoolean("playtimeRewards")){
            // If the playtime rewards are disabled, we do not start the scheduler and leave the function early.
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                UpdatePlaytimes();
            }
        }.runTaskTimer(plugin, 1000L, 1200L); //Delay: Ticks to wait before starting. Period: Ticks to wait between runs.
        // We delay it by 1000 ticks (50 seconds) and then every minute or 60 x 20 ticks per second = 1200 ticks we run it again.

        this.plugin = plugin;
    }

    private void UpdatePlaytimes() {
        // Loop through all the online players and see if the current playtime minus the one they got when they joined is greater than they need for the day they are on.
        for (Player p: plugin.getServer().getOnlinePlayers()) {
            int entryPlaytime = plugin.getStreaksConfig().getInt("players." + p.getUniqueId() + ".playtime");

            int difference = p.getStatistic(Statistic.PLAY_ONE_MINUTE) - entryPlaytime;


        }
    }
}
