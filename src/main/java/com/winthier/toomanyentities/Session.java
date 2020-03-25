package com.winthier.toomanyentities;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Value
public final class Session {
    List<Entry> entries = new ArrayList<>();

    @Value
    static class Entry {
        String world;
        double x;
        double y;
        double z;

        Location getLocation(Player player) {
            World w = Bukkit.getServer().getWorld(world);
            if (w == null) return null;
            Location loc = player.getLocation();
            loc.setWorld(w);
            loc.setX(x);
            loc.setY(y);
            loc.setZ(z);
            return loc;
        }
    }
}
