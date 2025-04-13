package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.CommandModule;
import au.com.addstar.pansentials.Utilities;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KillModule extends CommandModule implements Listener
{
	private final Map<Player, String> messages;
	
	public KillModule()
	{
		super("kill");
		
		messages = Maps.newHashMap();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length < 1)
			return false;
		
		// Find the player we will be targeting
		Player target = Bukkit.getPlayer(args[0]);
		if (target == null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
			return true;
		}
		
		KillEffect effect = null;
		String deathMessage = null;
		
		// Find what kill effect they will have
		if (args.length > 1)
		{
			for (KillEffect e : KillEffect.values())
			{
				if (args[1].equalsIgnoreCase(e.name()))
				{
					effect = e;
					break;
				}
			}
			
			if (effect == null)
			{
				sender.sendMessage(ChatColor.RED + "Unknown effect " + args[1]);
				return true;
			}
			
			// Death message
			if (args.length > 2)
				deathMessage = String.format("%s %s", ChatColor.stripColor(target.getDisplayName()), StringUtils.join(args, ' ', 2, args.length));
		}
		else
			effect = KillEffect.Default;
		
		killPlayer(target, effect, deathMessage);
		
		sender.sendMessage(Utilities.format(getPlugin().getFormatConfig(), "kill." + effect.name().toLowerCase() + ".done", "%player%:" + target.getDisplayName()));
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length == 1)
			return Utilities.matchStrings(args[0], Bukkit.getOnlinePlayers(), Utilities.PlayerName);
		else if (args.length == 2)
			return Utilities.matchStrings(args[1], Arrays.asList(KillEffect.values()), Functions.toStringFunction());
		
		return null;
	}
	
	private void killPlayer(Player target, KillEffect effect, String message)
	{
		// Give appropriate message if none is specified
		if (message == null)
		{
			String path = "kill." + effect.name().toLowerCase() + ".msg";
			FileConfiguration config = getPlugin().getFormatConfig();
			
			if (!config.contains(path))
				path = "kill.default.msg";
			
			// Pick one of the messages
			if (config.isList(path))
			{
				List<String> messages = config.getStringList(path);
				if (!messages.isEmpty())
					message = messages.get(RandomUtils.nextInt(0, messages.size()));
			}
			else
				message = config.getString(path);

			if (message != null)
			{
				message = ChatColor.translateAlternateColorCodes('&', message);
				message = message.replace("%player%", ChatColor.stripColor(target.getDisplayName()));
			}
		}
		
		switch (effect)
		{
		default:
		case Default:
			kill(target, message);
			break;
		case Explode:
			explodeKill(target, message);
			break;
		case Fall:
			kill(target, message);
			break;
		case Firework:
			fireworkKill(target, message);
			break;
		case Lightning:
			lightningKill(target, message);
			break;
		case Fire:
			fireKill(target, message);
			break;
		}
	}
	
	private void kill(Player player, String message)
	{
		messages.put(player, message);
		player.setHealth(0);
	}
	
	private void explodeKill(Player target, String message)
	{
		target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation(), 100);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 30);
		
		kill(target, message);
	}
	
	private void lightningKill(Player target, String message)
	{
		target.getWorld().strikeLightningEffect(target.getLocation());
		
		kill(target, message);
	}
	
	private void fireworkKill(final Player target, final String message)
	{
		if (target.isFlying())
			target.setFlying(false);
		
		target.setVelocity(new Vector(0, 2, 0));

		Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
			Location loc = target.getLocation();
			final Firework firework = loc.getWorld().spawn(loc, Firework.class);

			// Assign effect
			FireworkMeta meta = firework.getFireworkMeta();

			meta.addEffect(newRandomFirework());
			meta.addEffect(newRandomFirework());
			meta.addEffect(newRandomFirework());

			firework.setFireworkMeta(meta);

			// Fireworks cannot be detonated on the same tick
			Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
				firework.detonate();

				// Kill them
				kill(target, message);
			}, 2);
		}, 30);
	}
	
	private void fireKill(final Player target, final String message)
	{
		target.setFireTicks(300000);
		target.setHealth(2);
		
		messages.put(target, message);
		
		// Just in case they dont die from fire damage
		Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
			if (messages.containsKey(target)) {
				// Kill them
				kill(target, message);
			}
		}, 40);
	}
	
	private FireworkEffect newRandomFirework()
	{
		return FireworkEffect.builder()
			.withColor(Color.fromRGB(RandomUtils.nextInt(0, 255), RandomUtils.nextInt(0, 255), RandomUtils.nextInt(0, 255)))
			.withFade(Color.fromRGB(RandomUtils.nextInt(0, 255), RandomUtils.nextInt(0, 255), RandomUtils.nextInt(0, 255)))
			.with(Type.values()[RandomUtils.nextInt(0, Type.values().length)])
			.flicker(RandomUtils.nextBoolean())
			.trail(RandomUtils.nextBoolean())
			.build();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onPlayerDeath(PlayerDeathEvent event)
	{
		String message = messages.remove(event.getEntity());
		
		if (message == null)
			return;
		
		event.setDeathMessage(message);
	}
	
	private enum KillEffect
	{
		Default,
		Explode,
		Fall,
		Firework,
		Lightning,
		Fire
	}
}
