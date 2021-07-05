package com.winthier.toomanyentities;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public final class TMECommand implements TabExecutor {
    final TooManyEntitiesPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        boolean res = onCommand(sender, args);
        if (!res) sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        if (args.length == 1) {
            return tab(cmd, Stream.of("scan", "sweep", "tp", "worlds", "entities", "players", "elytra", "nogoal"));
        }
        if (args.length == 2 && args[0].equals("nogoal")) {
            return tab(args[1], Stream.of(EntityType.values())
                       .filter(e -> e.getEntityClass() != null && Mob.class.isAssignableFrom(e.getEntityClass()))
                       .map(Enum::name).map(String::toLowerCase));
        }
        return null;
    }

    boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "scan": return scanCommand(sender, args);
        case "tp": return tpCommand(sender, args);
        case "sweep": return sweepCommand(sender, args);
        case "nogoal": return noGoalCommand(sender, args);
        case "worlds": return worldsCommand(sender, args);
        case "players": return playersCommand(sender, args);
        case "entities": return entitiesCommand(sender, args);
        case "elytra": return elytraCommand(sender, args);
        default: {
            sender.sendMessage("" + ChatColor.RED + "Unknown command: " + args[0]);
            return true;
        }
        }
    }

    boolean scanCommand(CommandSender sender, String[] args) {
        double paramRadius = 1.0;
        int paramLimit = 100;
        EntityType paramType = null;
        boolean paramExclude = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("r:")) {
                try {
                    paramRadius = Double.parseDouble(getParameterInput(args[i]));
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(ChatColor.RED
                                       + "Invalid input for radius, number expected: "
                                       + getParameterInput(args[i]));
                    return true;
                }
                if (paramRadius < 0.0) {
                    sender.sendMessage(ChatColor.RED
                                       + "Invalid input for radius, positive number expected: "
                                       + getParameterInput(args[i]));
                    return true;
                }
                if (paramRadius > 100.0) {
                    sender.sendMessage(ChatColor.RED
                                       + "Radius too large: "
                                       + paramRadius);
                    return true;
                }
            } else if (args[i].startsWith("l:")) {
                try {
                    paramLimit = Integer.parseInt(getParameterInput(args[i]));
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(ChatColor.RED
                                       + "Invalid input for limit, number expected: "
                                       + getParameterInput(args[i]));
                    return true;
                }
                if (paramLimit < 1) {
                    sender.sendMessage(ChatColor.RED
                                       + "Invalid input for limit, positive number expected: "
                                       + getParameterInput(args[i]));
                    return true;
                }
            } else if (args[i].trim().equals("-e")) {
                paramExclude = true;
            } else if (args[i].startsWith("t:")) {
                try {
                    paramType = EntityType.valueOf(getParameterInput(args[i])
                                                   .toUpperCase().replaceAll("-", "_"));
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage(ChatColor.RED
                                       + "Invalid input for type: " + getParameterInput(args[i]));
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED
                                   + "Unknown parameter: " + getParameterInput(args[i]));
                return true;
            }
        }
        if (paramType != null && paramExclude) {
            sender.sendMessage(ChatColor.RED
                               + "Can't use exclude option and type parameter at the same time");
            return true;
        }
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player != null) plugin.metadata.clearSession(player);
        TooManyEntitiesTask task = new TooManyEntitiesTask(plugin, sender,
                                                           paramRadius, paramLimit,
                                                           paramType, paramExclude, 300);
        task.init();
        task.start();
        return true;
    }

    boolean tpCommand(CommandSender sender, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
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
        Session session = plugin.metadata.sessionOf(player);
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
        return true;
    }

    boolean sweepCommand(CommandSender sender, String[] args) {
        SweepTask task = new SweepTask(plugin, sender, 500);
        task.init();
        task.start();
        return true;
    }

    boolean worldsCommand(CommandSender sender, String[] args) {
        Map<String, Integer> map = new HashMap<>();
        for (World world : plugin.getServer().getWorlds()) {
            map.put(world.getName(), world.getEntities().size());
        }
        List<String> list = map.keySet().stream()
            .sorted((a, b) -> Integer.compare(map.get(a), map.get(b)))
            .collect(Collectors.toList());
        sender.sendMessage(ChatColor.YELLOW + "Listing " + list.size() + " worlds:");
        for (String w : list) {
            sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + w + " "
                               + ChatColor.WHITE + map.get(w) + " entities");
        }
        return true;
    }

    boolean playersCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        int radius = 2;
        if (args.length >= 2) {
            String radiusArg = args[1];
            try {
                radius = Integer.parseInt(radiusArg);
            } catch (NumberFormatException nfe) {
                radius = -1;
            }
            if (radius < 0) {
                Msg.msg(sender, "&cRadius expected, got: %s", radiusArg);
                return true;
            }
            if (radius > 10) {
                Msg.msg(sender, "&cRadius must be 10 or less");
                return true;
            }
        }
        Msg.msg(sender, "&eRanking entities around players. Chunk radius %d", radius);
        for (Ranking ranking : Ranking.rank(radius)) {
            Msg.msg(sender, " &6%d&r %s", ranking.count, ranking.name);
        }
        return true;
    }

    boolean elytraCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isGliding()) continue;
            Location location = player.getLocation();
            Vector velo = player.getVelocity();
            sender.sendMessage(player.getName() + " "
                               + location.getWorld().getName()
                               + " " + location.getBlockX()
                               + " " + location.getBlockY()
                               + " " + location.getBlockZ()
                               + " velocity=" + velo.length());
            count += 1;
        }
        sender.sendMessage("Total " + count + " elytra flyers");
        return true;
    }

    boolean entitiesCommand(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        EnumMap<EntityType, Integer> map = new EnumMap<>(EntityType.class);
        for (EntityType type : EntityType.values()) {
            map.put(type, 0);
        }
        List<World> worlds;
        if (args.length >= 2) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "World not found: " + args[1]);
                return true;
            }
            worlds = Arrays.asList(world);
        } else {
            worlds = plugin.getServer().getWorlds();
        }
        for (World world : worlds) {
            for (Entity entity : world.getEntities()) {
                EntityType type = entity.getType();
                int count = map.get(type);
                map.put(type, count + 1);
            }
        }
        List<EntityType> entities = map.keySet().stream()
            .filter(et -> map.get(et) > 0)
            .sorted((a, b) -> Integer.compare(map.get(a), map.get(b)))
            .collect(Collectors.toList());
        sender.sendMessage(ChatColor.YELLOW + "Listing " + entities.size() + " entity types:");
        for (int i = 0; i < entities.size(); i += 1) {
            EntityType type = entities.get(i);
            sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + type.name().toLowerCase()
                               + " " + ChatColor.WHITE + map.get(type) + " entities");
        }
        return true;
    }

    boolean noGoalCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException iae) {
            entityType = null;
        }
        if (entityType == null || entityType.getEntityClass() == null
            || !Mob.class.isAssignableFrom(entityType.getEntityClass())) {
            sender.sendMessage("Invalid entity type: " + args[1]);
            return true;
        }
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() != entityType) continue;
                if (!(entity instanceof Mob)) continue;
                Mob mob = (Mob) entity;
                Bukkit.getMobGoals().removeAllGoals(mob);
                count += 1;
            }
        }
        sender.sendMessage("Cleared goals of " + count + "x" + entityType);
        return true;
    }

    List<String> tab(String cmd, Stream<String> args) {
        return args.filter(arg -> arg.startsWith(cmd))
            .collect(Collectors.toList());
    }

    void sendHelp(CommandSender sender) {
        ChatColor aq = ChatColor.AQUA;
        ChatColor rs = ChatColor.RESET;
        sender.sendMessage("" + ChatColor.YELLOW + "Too Many Entities - commands:");
        sender.sendMessage(" " + aq
                           + "/tme sweep" + rs + " - sweeps all unneeded mobs");
        sender.sendMessage(" " + aq
                           + "/tme scan <parameters>");
        sender.sendMessage(" " + aq
                           + "/tme tp <index>");
        sender.sendMessage(" " + aq
                           + "/tme worlds" + rs + " - Count entities in worlds");
        sender.sendMessage(" " + aq
                           + "/tme entities [world]" + rs + " - Count entity types");
        sender.sendMessage(" " + aq
                           + "/tme players <radius>" + rs + " - Count mobs near players");
        sender.sendMessage(" " + aq
                           + "/tme elytra" + rs + " - See who's flying with elytra");
        sender.sendMessage(" " + aq
                           + "/tme nogoal <type>" + rs + " - Clear mob goals");
        sender.sendMessage(" " + rs + "Parameters:");
        sender.sendMessage(" " + aq + "r:" + ChatColor.GREEN + "<radius>"
                           + rs + " - radius of the search (defaults to 1)");
        sender.sendMessage(" " + aq + "l:" + ChatColor.GREEN + "<limit>"
                           + rs + " - set entity limit (default 100)");
        sender.sendMessage(" " + aq + "t:" + ChatColor.GREEN + "<type>"
                           + rs + " - specify mob type");
        sender.sendMessage(" " + aq + "-e" + rs + " - exclude non-mobs");
    }

    private String getParameterInput(String s) {
        if (s.isEmpty()) {
            return "";
        }
        s = s.replace("p:", "");
        s = s.replace("l:", "");
        s = s.replace("r:", "");
        s = s.replace("t:", "");
        return s.trim();
    }
}
