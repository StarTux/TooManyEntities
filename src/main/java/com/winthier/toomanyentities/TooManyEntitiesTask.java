package com.winthier.toomanyentities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TooManyEntitiesTask extends BukkitRunnable
{
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
	private LinkedList<EntityType> excluded_types = new LinkedList<EntityType>();
	private boolean first = true;
	private boolean found = false;

	public TooManyEntitiesTask(TooManyEntitiesPlugin plugin, CommandSender sender, double radius, int limit, EntityType type, boolean exclude, int checksPerTick)
	{
		this.plugin = plugin;
		this.sender = sender;
		this.limit = limit;
		this.radius = radius;
		this.type = type;
		this.exclude = exclude;
		this.checksPerTick = checksPerTick;

		if(exclude)
		{
			EntityType[] types = EntityType.values();

			for(int i = 0; i < types.length; i++)
			{
				if(!types[i].isAlive())
					excluded_types.add(types[i]);
			}
		}
	}

	@Override
	public void run()
	{
		if(first)
		{
			String s = "";

			s += "more than " + limit + " ";

			if(type != null)
				s += type.name().toLowerCase();
			else
				s += "entities";

			s += " in a radius of " + radius;

			if(exclude)
				s+= ", excluding non-mobs";

			sender.sendMessage("" + ChatColor.YELLOW + "Too Many Entities - search result for " + s + ":");

			first = false;
		}

		for(int i = 0; i < checksPerTick; ++i)
		{
			if(entities.isEmpty())
			{
				if(!found)
					sender.sendMessage(" " + ChatColor.WHITE + "Nothing found");

				stop();
				return;
			}
			else
			{
				Entity entity = entities.removeFirst();

				if(!entity.isValid())
					continue;

				if(findings.contains(entity))
					continue;

				if(type != null && entity.getType() != type)
					continue;

				List<Entity> tmp = entity.getNearbyEntities(radius, radius, radius);
				List<Entity> nearby = new ArrayList<Entity>(tmp.size() + 1);

				for(Entity e : tmp)
				{
					boolean add = false;

					if(type == null || e.getType() == type)
						add = true;

					if(exclude && excluded_types.contains(e.getType()))
						add = false;

					if(add)
						nearby.add(e);
				}

				nearby.add(entity);

				if(nearby.size() > limit)
				{
					report(nearby);
					findings.add(entity);
					findings.addAll(nearby);
					found = true;
				}
			}
		}
	}
	
	public void report(List<Entity> entities)
	{
		Location loc = entities.get(0).getLocation();
		EntityType top = null;
		int max = 0;
		EnumMap<EntityType, Integer> entityCount = new EnumMap<EntityType, Integer>(EntityType.class);

		for(Entity entity : entities)
		{
			int count = 1;
			Integer tmp = entityCount.get(entity.getType());

			if(tmp != null)
				count = tmp + 1;

			entityCount.put(entity.getType(), count);

			if(count > max)
			{
				max = count;
				top = entity.getType();
				loc = entity.getLocation(loc);
			}
		}

		sender.sendMessage(" " + ChatColor.WHITE + entities.size() + " found in " + ChatColor.LIGHT_PURPLE + loc.getWorld().getName() + " " + ChatColor.WHITE + "at " + ChatColor.GREEN + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " " + ChatColor.WHITE + "(mostly " + niceEntityName(top) + ")");
	}

	public void init()
	{
		for(World world : plugin.getServer().getWorlds())
		{
			entities.addAll(world.getEntities());
		}
	}

	public void start()
	{
		runTaskTimer(plugin, 0L, 1L);
	}

	public void stop()
	{
		try
		{
			cancel();
		}
		catch(Exception e)
		{}
	}

	private static String niceEntityName(EntityType e)
	{
		return e.name().toLowerCase().replaceAll("_", " ");
	}
}
