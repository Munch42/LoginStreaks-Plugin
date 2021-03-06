package me.munch42.loginstreak.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.munch42.loginstreak.Main;
import org.bukkit.entity.Player;

public class LoginStreakExpansion extends PlaceholderExpansion {

    private Main plugin;

    public LoginStreakExpansion(Main plugin){
        this.plugin = plugin;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist(){
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "loginstreak";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.entity.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }

        // %loginstreak_playerdays%
        if(identifier.equals("playerdays")){
            int daysTotal = plugin.getStreaksConfig().getInt("players." + player.getUniqueId() + ".totalStreakDays");
            return String.valueOf(daysTotal);
        }

        // %loginstreak_timeleft%
        if(identifier.equals("timeleft")){
            return plugin.getTimeLeft(player);
        }

        // %loginstreak_top1%
        if(identifier.equals("top1")){
            return plugin.getRankMessage(1);
        }

        // %loginstreak_top2%
        if(identifier.equals("top2")){
            return plugin.getRankMessage(2);
        }

        // %loginstreak_top3%
        if(identifier.equals("top3")){
            return plugin.getRankMessage(3);
        }

        // %loginstreak_top4%
        if(identifier.equals("top4")){
            return plugin.getRankMessage(4);
        }

        // %loginstreak_top5%
        if(identifier.equals("top5")){
            return plugin.getRankMessage(5);
        }

        // %loginstreak_top6%
        if(identifier.equals("top6")){
            return plugin.getRankMessage(6);
        }

        // %loginstreak_top7%
        if(identifier.equals("top7")){
            return plugin.getRankMessage(7);
        }

        // %loginstreak_top8%
        if(identifier.equals("top8")){
            return plugin.getRankMessage(8);
        }

        // %loginstreak_top9%
        if(identifier.equals("top9")){
            return plugin.getRankMessage(9);
        }

        // %loginstreak_top10%
        if(identifier.equals("top10")){
            return plugin.getRankMessage(10);
        }

        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
        // was provided
        return null;
    }

}
