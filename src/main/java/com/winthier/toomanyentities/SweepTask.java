package com.winthier.toomanyentities;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public final class SweepTask extends BukkitRunnable {
    private final TooManyEntitiesPlugin plugin;
    private final CommandSender sender;
    private final int monstersPerTick;
    private final LinkedList<Monster> monsters = new LinkedList<Monster>();
    private final Random random = new Random(System.currentTimeMillis());

    public SweepTask(final TooManyEntitiesPlugin plugin, final CommandSender sender,
                     final int monstersPerTick) {
        this.plugin = plugin;
        this.sender = sender;
        this.monstersPerTick = monstersPerTick;
    }

    @Override
    public void run() {
        for (int i = 0; i < monstersPerTick; ++i) {
            if (monsters.isEmpty()) {
                stop();
                return;
            } else {
                Monster monster = monsters.removeFirst();

                if (!monster.isValid()) continue;
                if (monster.customName() != null) continue;
                if (!monster.isEmpty()) continue;
                if (monster.getVehicle() != null) continue;

                // drop equipment
                final EntityEquipment equipment = monster.getEquipment();
                if (equipment != null) {
                    final int size = 6;
                    ItemStack[] items = new ItemStack[size];
                    float[] chances = new float[size];
                    items[0] = equipment.getItemInMainHand();
                    chances[0] = equipment.getItemInMainHandDropChance();
                    items[1] = equipment.getItemInOffHand();
                    chances[1] = equipment.getItemInOffHandDropChance();
                    items[2] = equipment.getHelmet();
                    chances[2] = equipment.getHelmetDropChance();
                    items[3] = equipment.getChestplate();
                    chances[3] = equipment.getChestplateDropChance();
                    items[4] = equipment.getLeggings();
                    chances[4] = equipment.getLeggingsDropChance();
                    items[5] = equipment.getBoots();
                    chances[5] = equipment.getBootsDropChance();
                    for (int j = 0; j < size; ++j) {
                        final ItemStack item = items[j];
                        final float chance = chances[j];
                        if (chance >= 0.99f && item != null && item.getType() != Material.AIR) {
                            final Location loc = monster.getLocation();
                            loc.getWorld().dropItemNaturally(loc, item.clone());
                        }
                    }
                }

                monster.remove();
            }
        }
    }

    public void init() {
        sender.sendMessage("" + ChatColor.YELLOW + "Too Many Entities - sweeping monsters...");
        for (World world : plugin.getServer().getWorlds()) {
            Collection<Monster> e = world.getEntitiesByClass(Monster.class);
            sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + world.getName()
                               + " " + ChatColor.WHITE + e.size() + " monsters");
            monsters.addAll(e);
        }
    }

    public void start() {
        runTaskTimer(plugin, 0L, 1L);
    }

    public void stop() {
        sender.sendMessage("" + ChatColor.YELLOW + "Done.");

        try {
            cancel();
        } catch (Exception e) { }
    }
}
