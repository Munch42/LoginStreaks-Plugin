package me.munch42.loginstreak.tabcompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class LoginStreakTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cde, String arg, String[] args) {
        List<String> loginstreakCommandList = new ArrayList<String>();

        loginstreakCommandList.add("reload");
        loginstreakCommandList.add("resetstreak");
        loginstreakCommandList.add("setstreak");
        loginstreakCommandList.add("backup");

        return loginstreakCommandList;
    }
}
