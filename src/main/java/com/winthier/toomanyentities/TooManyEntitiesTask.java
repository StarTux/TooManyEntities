package com.winthier.toomanyentities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import static com.cavetale.core.util.CamelCase.toCamelCase;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public final class TooManyEntitiesTask extends BukkitRunnable {
    TooManyEntitiesPlugin plugin;
    private CommandSender sender;
    private int limit;
    private double radius;
    private Player player;
    private boolean exclude;
    private int checksPerTick;
    private LinkedList<Entity> entities = new LinkedList<Entity>();
    private Set<Entity> findings = new HashSet<Entity>();
    private EntityType type = null;
    private LinkedList<EntityType> excludedTypes = new LinkedList<EntityType>();
    private boolean first = true;
    private boolean found = false;
    private int currentIndex = 1;

    public TooManyEntitiesTask(final TooManyEntitiesPlugin plugin, final CommandSender sender,
                               final double radius, final int limit,
                               final EntityType type, final boolean exclude,
                               final int checksPerTick) {
        this.plugin = plugin;
        this.sender = sender;
        this.limit = limit;
        this.radius = radius;
        this.type = type;
        this.exclude = exclude;
        this.checksPerTick = checksPerTick;
        this.player = sender instanceof Player ? (Player) sender : null;

        if (exclude) {
            EntityType[] types = EntityType.values();
            for (EntityType et : EntityType.values()) {
                if (!et.isAlive()) {
                    excludedTypes.add(et);
                }
            }
            excludedTypes.add(EntityType.ARMOR_STAND);
        }
    }

    @Override
    public void run() {
        if (player != null && !player.isValid()) return;
        if (first) {
            String s = "";

            s += "more than " + limit + " ";

            if (type != null) {
                s += type.name().toLowerCase();
            } else {
                s += "entities";
            }
            s += " in a radius of " + radius;
            if (exclude) {
                s += ", excluding non-mobs";
            }
            sender.sendMessage(text("Too Many Entities - search result for " + s + ":", YELLOW));
            first = false;
        }

        for (int i = 0; i < checksPerTick; ++i) {
            if (entities.isEmpty()) {
                if (!found) {
                    sender.sendMessage(text("Nothing found", RED));
                }
                stop();
                return;
            } else {
                Entity entity = entities.removeFirst();
                if (!entity.isValid()) continue;
                if (findings.contains(entity)) continue;
                if (type != null && entity.getType() != type) continue;
                List<Entity> tmp = entity.getNearbyEntities(radius, radius, radius);
                List<Entity> nearby = new ArrayList<Entity>(tmp.size() + 1);
                for (Entity e : tmp) {
                    boolean add = false;
                    if (type == null || e.getType() == type) {
                        add = true;
                    }
                    if (exclude && excludedTypes.contains(e.getType())) {
                        add = false;
                    }
                    if (add) {
                        nearby.add(e);
                    }
                }
                nearby.add(entity);
                if (nearby.size() > limit) {
                    report(nearby);
                    findings.add(entity);
                    findings.addAll(nearby);
                    found = true;
                }
            }
        }
    }

    public void report(List<Entity> list) {
        Location loc = list.get(0).getLocation();
        EntityType top = null;
        int max = 0;
        EnumMap<EntityType, Integer> entityCount = new EnumMap<>(EntityType.class);
        for (Entity entity : list) {
            int count = 1;
            Integer tmp = entityCount.get(entity.getType());
            if (tmp != null) {
                count = tmp + 1;
            }
            entityCount.put(entity.getType(), count);
            if (count > max) {
                max = count;
                top = entity.getType();
                loc = entity.getLocation(loc);
            }
        }
        sender.sendMessage(textOfChildren(text("" + (currentIndex++) + ") ", YELLOW),
                                          text(list.size() + " found at "),
                                          text(loc.getWorld().getName() + " ", YELLOW),
                                          text(loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()),
                                          text(" (" + toCamelCase(" ", top) + ")")));
        if (player != null) {
            Session.Entry entry = new Session
                .Entry(loc.getWorld().getName(),
                       loc.getX(), loc.getY(), loc.getZ());
            plugin.metadata.sessionOf(player).getEntries().add(entry);
        }
    }

    public void init() {
        for (World world : plugin.getServer().getWorlds()) {
            entities.addAll(world.getEntities());
        }
    }

    public void start() {
        runTaskTimer(plugin, 0L, 1L);
    }

    public void stop() {
        try {
            cancel();
        } catch (Exception e) { }
    }
}
