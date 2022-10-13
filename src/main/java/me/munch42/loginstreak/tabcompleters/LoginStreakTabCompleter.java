package me.munch42.loginstreak.tabcompleters;

import me.munch42.loginstreak.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoginStreakTabCompleter implements TabCompleter {
    Main plugin;

    public LoginStreakTabCompleter(Main plugin){
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cde, String arg, String[] args) {
        List<String> loginstreakCommandList = new ArrayList<String>();

        //StringUtils.printArray(args, true);
        /*
        Args variable:
        /loginstreak setstreak Munch42 5 true
        Appears as:
        ["setstreak", "Munch42", "5", "true"]
         */

        boolean isSetStreak = args[0].equals("setstreak");
        boolean isResetStreak = args[0].equals("resetstreak");

        switch (args.length) {
            case 2:
                // This is usually the username
                if (isSetStreak || isResetStreak){
                    // Add all the saved usernames to the loginstreak command list
                    //loginstreakCommandList.addAll(plugin.getUsernames());

                    for (Player p: plugin.getServer().getOnlinePlayers()){
                        loginstreakCommandList.add(p.getDisplayName());
                    }
                }
                break;
            case 3:
                // This is usually the number of days
                if(isSetStreak){
                    for (int x = 1; x <= 30; x++){
                        loginstreakCommandList.add(String.valueOf(x));
                    }
                }
                break;
            case 4:
                // This is usually whether it is true or false for giving the reward.
                // Check if the base command extension is setstreak since otherwise we just want to display nothing as it is not a case.
                if (isSetStreak){
                    loginstreakCommandList.add("true");
                    loginstreakCommandList.add("false");
                }
                break;
            default:
                // If it is a length of 1 or more than 3, then we show default values
                loginstreakCommandList.add("reload");
                loginstreakCommandList.add("resetstreak");
                loginstreakCommandList.add("setstreak");
                loginstreakCommandList.add("backup");
                break;
        }

        /*loginstreakCommandList.add("reload");
        loginstreakCommandList.add("resetstreak");
        loginstreakCommandList.add("setstreak");
        loginstreakCommandList.add("backup");*/

        return loginstreakCommandList;
    }
}
