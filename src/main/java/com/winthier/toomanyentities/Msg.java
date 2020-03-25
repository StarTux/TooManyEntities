package com.winthier.toomanyentities;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    private Msg() { }

    public static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    public static void msg(CommandSender sender, String msg, Object... args) {
        msg = format(msg, args);
        sender.sendMessage(msg);
    }

    public static String niceEntityName(Enum<?> e) {
        return e.name().toLowerCase().replaceAll("_", " ");
    }
}
