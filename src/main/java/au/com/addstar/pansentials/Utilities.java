package au.com.addstar.pansentials;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities
{
	public static long parseDateDiff(String dateDiff)
	{
		if(dateDiff == null)
			return 0;
		
		Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
		dateDiff = dateDiff.toLowerCase();
		
		Matcher m = dateDiffPattern.matcher(dateDiff);

		if (m.matches()) {
			int years, months, weeks, days, hours, minutes, seconds;
			boolean negative;

			negative = m.group(1) != null && (m.group(1).compareTo("-") == 0);

			if (m.group(2) != null)
				years = Integer.parseInt(m.group(2));
			else
				years = 0;

			if (m.group(3) != null)
				months = Integer.parseInt(m.group(3));
			else
				months = 0;

			if (m.group(4) != null)
				weeks = Integer.parseInt(m.group(4));
			else
				weeks = 0;

			if (m.group(5) != null)
				days = Integer.parseInt(m.group(5));
			else
				days = 0;

			if (m.group(6) != null)
				hours = Integer.parseInt(m.group(6));
			else
				hours = 0;

			if (m.group(7) != null)
				minutes = Integer.parseInt(m.group(7));
			else
				minutes = 0;

			if (m.group(8) != null)
				seconds = Integer.parseInt(m.group(8));
			else
				seconds = 0;

			// Now calculate the time
			long time = 0;
			time += seconds * 1000L;
			time += minutes * 60000L;
			time += hours * 3600000L;
			time += days * 72000000L;
			time += weeks * 504000000L;
			time += months * 2191500000L;
			time += years * 26298000000L;

			if (negative)
				time *= -1;

			return time;
		}
		
		return 0;
	}
	
	public static boolean safeTeleport(Player player, Location loc)
	{
		int horRange = 30;
		
		double closestDist = Double.MAX_VALUE;
		Location closest = null;
		
		for(int y = 0; y < loc.getWorld().getMaxHeight(); ++y)
		{
			for(int x = loc.getBlockX() - horRange; x < loc.getBlockX() + horRange; ++x)
			{
				for(int z = loc.getBlockZ() - horRange; z < loc.getBlockZ() + horRange; ++z)
				{
					for(int i = 0; i < 2; ++i)
					{
						int yy = loc.getBlockY();
						
						if(i == 0)
						{
							yy -= y;
							if(yy < 0)
								continue;
						}
						else
						{
							yy += y;
							if(yy >= loc.getWorld().getMaxHeight())
								continue;
						}
	
						Location l = new Location(loc.getWorld(), x, yy, z);
						double dist = loc.distanceSquared(l);
						
						if(dist < closestDist && isSafeLocation(l))
						{
							closest = l;
							closestDist = dist;
						}
					}
				}
			}
			
			if(y*y > closestDist)
				break;
		}
		
		if(closest == null)
			return false;
		
		closest.setPitch(loc.getPitch());
		closest.setYaw(loc.getYaw());
		
		return player.teleport(closest.add(0.5, 0, 0.5));
	}

    private static boolean isSafeLocation(Location loc)
	{
		Block feet = loc.getBlock();
		Block ground = feet.getRelative(BlockFace.DOWN);
		Block head = feet.getRelative(BlockFace.UP);
		
		return (isSafe(feet) && isSafe(head) && (head.getType() != Material.WATER) && ground.getType().isSolid());
	}
	
	private static boolean isSafe(Block block)
	{
		switch(block.getType())
		{
			case AIR:
			case SUGAR_CANE:
			case WATER:
			case SHORT_GRASS:
			case WHEAT:
			case BEETROOT:
			case SEAGRASS:
			case CAVE_AIR:
			case TORCH:
			case WALL_TORCH:
			case CARROT:
			case POTATO:
			case RED_MUSHROOM:
			case ROSE_BUSH:
			case BROWN_MUSHROOM:
			case OXEYE_DAISY:
			case DEAD_BUSH:
			case OAK_SIGN:
			case OAK_WALL_SIGN:
			return true;
		default:
			return false;
		}
	}
	
	public static void adjustEventHandlerPosition(HandlerList list, Listener listener, String beforePlugin)
	{
		Plugin plugin = Bukkit.getPluginManager().getPlugin(beforePlugin);
		if(plugin == null || !plugin.isEnabled())
			return;

		ArrayList<RegisteredListener> theirs = new ArrayList<>();
		RegisteredListener mine = null;
		
		for(RegisteredListener regListener : list.getRegisteredListeners())
		{
			if(regListener.getListener() == listener)
				mine = regListener;
			if(regListener.getPlugin().equals(plugin))
				theirs.add(regListener);
		}
		
		if(mine == null)
			return;
		
		list.unregister(mine);
		for(RegisteredListener regListener : theirs)
			list.unregister(regListener);
		
		// Register in the order we want them in
		list.register(mine);
		list.registerAll(theirs);
		list.bake();
		
		MasterPlugin.getInstance().getLogger().info("NOTE: Listener " + listener + " injected before that of " + beforePlugin + " listener");
	}
	
	public static List<String> matchStrings(String str, Collection<String> values)
	{
		str = str.toLowerCase();
		ArrayList<String> matches = new ArrayList<>();
		
		for(String value : values)
		{
			if(value.toLowerCase().startsWith(str))
				matches.add(value);
		}
		
		if(matches.isEmpty())
			return null;
		return matches;
	}
	
	public static <T> List<String> matchStrings(String str, Collection<?> values, Function<T, String> toString)
	{
		str = str.toLowerCase();
		ArrayList<String> matches = new ArrayList<>();
		
		for(Object value : values)
		{
			@SuppressWarnings("unchecked")
			String name = toString.apply((T)value);

            if (name != null && name.toLowerCase().startsWith(str))
				matches.add(name);
		}
		
		if(matches.isEmpty())
			return null;
		return matches;
	}
	
	public static String format(FileConfiguration config, String path){
		if(!config.contains(path)) return ChatColor.RED + "No format value found for path:" + path + "!";
		return ChatColor.translateAlternateColorCodes("&".charAt(0), config.getString(path));
	}
	
	/**
	 * Formats a string from the format file and stuff...
	 * @param config The format config
	 * @param path The path of the config
	 * @param args Must match "%idname%:replacement
	 * @return A formatted string
	 */
	public static String format(FileConfiguration config, String path, String... args){
		if(!config.contains(path)) return ChatColor.RED + "No format value found for path:" + path + "!";
		String form = ChatColor.translateAlternateColorCodes("&".charAt(0), config.getString(path));
		for(String arg : args){
			String[] parts = arg.split(":", 2);
			form = form.replaceAll(parts[0], parts[1]);
		}
		return form;
	}
	
	public static int parseInt(String value, String error)
	{
		return parseInt(value, Integer.MIN_VALUE, Integer.MAX_VALUE, error);
	}

    private static int parseInt(String value, int min, int max, String error)
	{
		try
		{
			int i = Integer.parseInt(value);
			if (i < min)
				throw new IllegalArgumentException(error);
			if (i > max)
				throw new IllegalArgumentException(error);
			return i;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(error);
		}
	}
	
	public static float parseFloat(String value, String error)
	{
		return parseFloat(value, Float.MIN_VALUE, Float.MAX_VALUE, error);
	}

    private static float parseFloat(String value, float min, float max, String error)
	{
		try
		{
			float i = Float.parseFloat(value);
			if (i < min)
				throw new IllegalArgumentException(error);
			if (i > max)
				throw new IllegalArgumentException(error);
			return i;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(error);
		}
	}
	
	public static double parseDouble(String value, String error)
	{
		return parseDouble(value, Double.MIN_VALUE, Double.MAX_VALUE, error);
	}

    private static double parseDouble(String value, double min, double max, String error)
	{
		try
		{
			double i = Double.parseDouble(value);
			if (i < min)
				throw new IllegalArgumentException(error);
			if (i > max)
				throw new IllegalArgumentException(error);
			return i;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(error);
		}
	}

	public static final Function<Player, String> PlayerName = new PlayerNameFunction();

    static class PlayerNameFunction implements Function<Player, String>
	{
		@Override
		public String apply(Player player)
		{
			return player.getName();
		}
	}

	/**
	 * A Utility that sorts a map based on the value
	 *
	 * @param map The map to sort
	 * @param <K> The key
	 * @param <V> The Value
	 * @return  map  sorted by the value.
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V>
	sortByValue( Map<K, V> map )
	{
		List<Map.Entry<K, V>> list =
				new LinkedList<>( map.entrySet() );
        list.sort(Comparator.comparing(o -> (o.getValue())));

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put( entry.getKey(), entry.getValue() );
		}
		return result;
	}
}
