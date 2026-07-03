package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.MasterPlugin;
import au.com.addstar.pansentials.Module;
import au.com.addstar.pansentials.Utilities;
import com.google.common.collect.Iterables;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public class HealModule implements Module, CommandExecutor{
	
	private MasterPlugin plugin;

	@Override
	public void onEnable() {
		plugin.getCommand("heal").setExecutor(this);
		plugin.getCommand("feed").setExecutor(this);
	}

	@Override
	public void onDisable() {
		plugin.getCommand("heal").setExecutor(null);
		plugin.getCommand("feed").setExecutor(null);
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}
	
	private void heal(Player player) {
		player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getDefaultValue());
		player.setRemainingAir(player.getMaximumAir());
		player.setFoodLevel(20);
		player.setSaturation(20f);
		player.setFireTicks(0);
		
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (command.getName().equalsIgnoreCase("heal")) {
			if (args.length == 0) {
				if (sender instanceof Player) {
					heal((Player)sender);
					sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "heal.self"));
				}
			} else {
				if (sender.hasPermission("pansentials.heal.other")) {
					List<Player> players = plugin.getServer().matchPlayer(args[0]);
					Player target = Iterables.getFirst(players, null);
					if (target != null) {
						heal(target);
						target.sendMessage(Utilities.format(plugin.getFormatConfig(), "heal.self"));
						sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "heal.other", "%player%:" + target.getDisplayName()));
					} else {
						sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "noPlayer", "%name%:" + args[0]));
					}
				} else {
					sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "noPermission"));
				}
			}
		} else if(command.getName().equalsIgnoreCase("feed")) {
			if (args.length == 0) {
				if(sender instanceof Player){
					Player ply = (Player) sender;
					ply.setFoodLevel(20);
					ply.setSaturation(20f);
					ply.sendMessage(Utilities.format(plugin.getFormatConfig(), "feed.self"));
				}
			} else {
				if (sender.hasPermission("pansentials.feed.other")) {
					List<Player> players = plugin.getServer().matchPlayer(args[0]);
					Player target = Iterables.getFirst(players, null);
					if (target != null) {
						target.setFoodLevel(20);
						target.setSaturation(20f);
						target.sendMessage(Utilities.format(plugin.getFormatConfig(), "feed.self"));
						sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "feed.other", "%player%:" + target.getDisplayName()));
					} else {
						sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "noPlayer", "%name%:" + args[0]));
					}
				} else {
					sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "noPermission"));
				}
			}
		}
		return true;
	}
}
