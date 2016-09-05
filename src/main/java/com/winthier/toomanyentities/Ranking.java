package com.winthier.toomanyentities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class Ranking implements Comparable<Ranking> {
    public final String name;
    public final int count;

    public Ranking(String name, int count) {
        this.name = name;
        this.count = count;
    }

    @Override
    public int compareTo(Ranking o) {
        if (this.count < o.count) {
            return -1;
        } else if (this.count == o.count) {
            return 0;
        } else {
            return 1;
        }
    }

    public static List<Ranking> rank(int radius) {
        List<Ranking> rankings = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            final Chunk cChunk = player.getLocation().getChunk();
            final int cx = cChunk.getX();
            final int cz = cChunk.getZ();
            int count = 0;
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    final int x = cx + dx;
                    final int z = cz + dz;
                    final Chunk chunk = player.getWorld().getChunkAt(x, z);
                    for (Entity e : chunk.getEntities()) {
                        if (e.getType().isAlive()) {
                            count += 1;
                        }
                    }
                }
            }
            rankings.add(new Ranking(player.getName(), count));
        }
        Collections.sort(rankings);
        return rankings;
    }
}
