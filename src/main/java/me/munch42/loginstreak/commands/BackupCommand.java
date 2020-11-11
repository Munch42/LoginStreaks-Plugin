package me.munch42.loginstreak.commands;

import me.munch42.loginstreak.Main;
import me.munch42.loginstreak.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BackupCommand implements CommandExecutor {
    private Main plugin;

    public BackupCommand(Main plugin){
        this.plugin = plugin;

        plugin.getCommand("backup").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = "loginstreaks.backup";

        if(plugin.getConfig().getString("backupPerm") != null){
            permission = plugin.getConfig().getString("backupPerm");
        }

        if(sender.hasPermission(permission)) {
            File streakFile = plugin.getStreaksFile();
            File rankFile = plugin.getRanksFile();
            File dest = new File("backups/streaks.yml");
            File dest2 = new File("backups/ranks.yml");

            try {
                Files.copy(streakFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(rankFile.toPath(), dest2.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException err) {
                sender.sendMessage(ChatColor.DARK_RED + "There was an error backing up your files!");
                return false;
            }
        } else {
            if(plugin.getConfig().getString("noPermsMessage") != null) {
                String message = plugin.getConfig().getString("noPermsMessage");
                message = ChatUtils.parseColourCodes(message);

                sender.sendMessage(message);
            }

            return true;
        }
    }
}
