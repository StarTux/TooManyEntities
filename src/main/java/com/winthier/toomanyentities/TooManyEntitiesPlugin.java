package com.winthier.toomanyentities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TooManyEntitiesPlugin extends JavaPlugin
{
    @Value static class Entry {
        String world;
        double x, y ,z;
        Location getLocation(Player player) {
            World world = Bukkit.getServer().getWorld(this.world);
            if (world == null) return null;
            Location loc = player.getLocation();
            loc.setWorld(world);
            loc.setX(x);
            loc.setY(y);
            loc.setZ(z);
            return loc;
        }
    }
    @Value static class Session {
        List<Entry> entries = new ArrayList<>();
    }

    private final Map<UUID, Session> sessions = new HashMap<>();
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String token, String args[])
    {
        Player player = sender instanceof Player ? (Player)sender : null;
        if(args.length == 0)
        {
            sender.sendMessage("" + ChatColor.YELLOW + "Too Many Entities - commands:");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme count" + ChatColor.WHITE + " - shows a count of all entities in all worlds");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme rank <radius>" + ChatColor.WHITE + " - Rank players by nearby mobs");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme sweep" + ChatColor.WHITE + " - sweeps all unneeded mobs");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme types" + ChatColor.WHITE + " - shows a list of valid entity types");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme scan <parameters>");
            sender.sendMessage(" " + ChatColor.AQUA + "/tme tp <index>");
            sender.sendMessage(" " + ChatColor.WHITE + "Parameters:");
            sender.sendMessage(" " + ChatColor.AQUA + "r:" + ChatColor.GREEN + "<radius>" + ChatColor.WHITE + " - radius of the search (defaults to 1)");
            sender.sendMessage(" " + ChatColor.AQUA + "l:" + ChatColor.GREEN + "<limit>" + ChatColor.WHITE + " - only report occurrences of more than this amount (defaults to 100)");
            sender.sendMessage(" " + ChatColor.AQUA + "t:" + ChatColor.GREEN + "<type>" + ChatColor.WHITE + " - only report occurrences of this type of entity (see /tme types)");
            sender.sendMessage(" " + ChatColor.AQUA + "-e" + ChatColor.WHITE + " - exclude non-mobs (can't be used with the type parameter");
        }

        if(args.length > 0)
        {
            String param_command = args[0].trim();

            if(param_command.equals("scan"))
            {
                double param_radius = 1.0;
                int param_limit = 100;
                EntityType param_type = null;
                boolean param_exclude = false;

                for(int i = 1; i < args.length; i++)
                {
                    if(args[i].startsWith("r:"))
                    {
                        try
                        {
                            param_radius = Double.parseDouble(getParameterInput(args[i]));
                        }
                        catch(NumberFormatException nfe)
                        {
                            sender.sendMessage("" + ChatColor.RED + "Invalid input for radius, number expected: " + getParameterInput(args[i]));
                            return true;
                        }

                        if(param_radius < 0.0)
                        {
                            sender.sendMessage("" + ChatColor.RED + "Invalid input for radius, positive number expected: " + getParameterInput(args[i]));
                            return true;
                        }
                    }
                    else if(args[i].startsWith("l:"))
                    {
                        try
                        {
                            param_limit = Integer.parseInt(getParameterInput(args[i]));
                        }
                        catch(NumberFormatException nfe)
                        {
                            sender.sendMessage("" + ChatColor.RED + "Invalid input for limit, number expected: " + getParameterInput(args[i]));
                            return true;
                        }

                        if(param_limit < 1)
                        {
                            sender.sendMessage("" + ChatColor.RED + "Invalid input for limit, positive number larger than 0 expected: " + getParameterInput(args[i]));
                            return true;
                        }
                    }
                    else if(args[i].trim().equals("-e"))
                    {
                        param_exclude = true;
                    }
                    else if(args[i].startsWith("t:"))
                    {
                        try
                        {
                            param_type = EntityType.valueOf(getParameterInput(args[i]).toUpperCase().replaceAll("-", "_"));
                        }
                        catch(IllegalArgumentException iae)
                        {
                            sender.sendMessage("" + ChatColor.RED + "Invalid input for type: " + getParameterInput(args[i]));
                            return true;
                        }
                    }
                    else
                    {
                        sender.sendMessage("" + ChatColor.RED + "Unknown parameter: " + getParameterInput(args[i]));
                        return true;
                    }
                }

                if(param_type != null && param_exclude)
                {
                    sender.sendMessage("" + ChatColor.RED + "Can't use exclude option and type parameter at the same time");
                    return true;
                }

                if (player != null) sessions.remove(player.getUniqueId());
                TooManyEntitiesTask task = new TooManyEntitiesTask(this, sender, param_radius, param_limit, param_type, param_exclude, 300);
                task.init();
                task.start();
                return true;
            }
            else if(param_command.equals("tp"))
            {
                if (player == null) {
                    sender.sendMessage("Player expected.");
                    return true;
                }
                if (args.length != 2) return false;
                int index;
                try {
                    index = Integer.parseInt(args[1]);
                } catch (NumberFormatException nfe) {
                    index = -1;
                }
                if (index <= 0) return false;
                Session session = getSession(player);
                if (session.getEntries().size() <= index - 1) {
                    player.sendMessage(ChatColor.RED + "Index out of bounds.");
                    return true;
                }
                Location loc = session.getEntries().get(index - 1).getLocation(player);
                if (loc == null) {
                    player.sendMessage(ChatColor.RED + "Could not find location. Was the world unloaded?");
                    return true;
                }
                player.teleport(loc);
                player.sendMessage(ChatColor.YELLOW + "Teleported to finding #" + index);
            }
            else if(param_command.equals("sweep"))
            {
                SweepTask task = new SweepTask(this, sender, 500);
                task.init();
                task.start();
                return true;
            }
            else if(param_command.equals("types"))
            {
                sender.sendMessage("" + ChatColor.WHITE + "Available entity types:");

                EntityType[] types = EntityType.values();
                String list = "";

                for(int i = 0; i < types.length; i++)
                {
                    list += types[i].name();

                    if(i + 1 < types.length)
                        list += ", ";
                }

                sender.sendMessage(" " + ChatColor.WHITE + list);

                return true;
            }
            else if(param_command.equals("count"))
            {
                sender.sendMessage("" + ChatColor.YELLOW + "Too Many Entities - scanning...");

                for(World world : this.getServer().getWorlds())
                {
                    List<Entity> e = world.getEntities();
                    sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + world.getName() + " " + ChatColor.WHITE + e.size() + " entities");
                }

                sender.sendMessage("" + ChatColor.YELLOW + "Done.");

                return true;
            }
            else if (args.length >= 1 && args.length <= 2 && "Rank".equalsIgnoreCase(args[0]))
            {
                int radius = 2;
                if (args.length >= 2) {
                    String radiusArg = args[1];
                    try {
                        radius = Integer.parseInt(radiusArg);
                    } catch (NumberFormatException nfe) {
                        radius = -1;
                    }
                    if (radius < 0) {
                        msg(sender, "&cRadius expected, got: %s", radiusArg);
                        return true;
                    }
                    if (radius > 10) {
                        msg(sender, "&cRadius must be 10 or less");
                        return true;
                    }
                }
                msg(sender, "&eRanking entities around players. Chunk radius %d", radius);
                for (Ranking ranking : Ranking.rank(radius)) {
                    msg(sender, " &6%d&r %s", ranking.count, ranking.name);
                }
                return true;
            }
            else
            {
                sender.sendMessage("" + ChatColor.RED + "Unknown command: " + param_command);
                return true;
            }
        }

        return false;
    }

    private String getParameterInput(String s)
    {
        if(s.isEmpty())
            return "";

        s = s.replace("p:", "");
        s = s.replace("l:", "");
        s = s.replace("r:", "");
        s = s.replace("t:", "");

        return s.trim();
    }

    public static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    public static void msg(CommandSender sender, String msg, Object... args) {
        msg = format(msg, args);
        sender.sendMessage(msg);
    }

    Session getSession(Player player) {
        Session result = sessions.get(player.getUniqueId());
        if (result == null) {
            result = new Session();
            sessions.put(player.getUniqueId(), result);
        }
        return result;
    }

    void storeSession(Player player, Location location) {
        getSession(player).getEntries().add(new Entry(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
    }
}
