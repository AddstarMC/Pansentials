package au.com.addstar.pansentials.modules;

import com.google.common.collect.Maps;

import au.com.addstar.pansentials.MasterPlugin;
import au.com.addstar.pansentials.Module;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class PowertoolModule implements Module, CommandExecutor, Listener {
	private MasterPlugin plugin;
	
	private Map<Player, Map<Material, PowerTool>> powertools;
	private static final long cooldownDelay = 100;
	
	@Override
	public void onEnable() {
		plugin.getCommand("powertool").setExecutor(this);
		
		powertools = Maps.newHashMap();
	}
	
	@Override
		public void onDisable() {
		plugin.getCommand("powertool").setExecutor(null);
		powertools.clear();
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command calledCommand, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You need to be a player to use this");
			return true;
		}
		
		Player player = (Player)sender;

		ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().equals(Material.AIR)) {
			sender.sendMessage(ChatColor.RED + "You are not holding an item");
			return true;
		}
		
		if (item.getType().isBlock()) {
			sender.sendMessage(ChatColor.RED + "You can only use items, not blocks");
			return true;
		}
		
		if (args.length == 0) {
			Map<Material, PowerTool> tools = powertools.get(player);
			if (tools != null) {
                tools.remove(item.getType());
			}
			
			sender.sendMessage(ChatColor.GREEN + "That item is no longer a powertool");
			return true;
		}
		
		String message = StringUtils.join(args, ' ').trim();
		
		String caller = null;
		
		if (message.startsWith("#")) {
			int pos = message.indexOf(' ');
			if (pos < 0) {
				sender.sendMessage(ChatColor.RED + "You must also specify the command / chat after the caller tag");
				return true;
			}
			caller = message.substring(1, pos);
			message = message.substring(pos+1).trim();
		}
		boolean command = true;
		
		if (message.startsWith("-c")) {
			command = false;
			message = message.substring(2).trim();
		}
		
		Map<Material, PowerTool> tools = powertools.computeIfAbsent(player, k -> Maps.newHashMap());
		
		tools.put(item.getType(), new PowerTool(message, caller, command));
		
		sender.sendMessage(ChatColor.GREEN + "That item is now a powertool");
		
		return true;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		powertools.remove(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onClick(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();

		PowerTool tool = getTool(player.getInventory().getItemInMainHand(), player);
		if (tool != null) {
			Location loc = event.getRightClicked().getLocation();
			String converted = tool.message
					.replace("{x}", String.valueOf(loc.getBlockX()))
					.replace("{y}", String.valueOf(loc.getBlockY()))
					.replace("{z}", String.valueOf(loc.getBlockZ()))
					.replace("{world}", loc.getWorld().getName())
					.replace("{eid}", String.valueOf(event.getRightClicked().getEntityId()))
					.replace("{uuid}", event.getRightClicked().getUniqueId().toString())
					.replace("{caller}", player.getName())
					.replace("{calleruuid}", player.getUniqueId().toString());
			
			if (event.getRightClicked() instanceof Player) {
				converted = converted.replace("{player}", event.getRightClicked().getName());
			}
			
			runPowertool(tool, converted, resolveCaller(tool.caller, event.getRightClicked(), player));
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		PowerTool tool = getTool(player.getInventory().getItemInMainHand(), player);
		if (tool != null) {
			String converted = tool.message;
			
			if (event.hasBlock()) {
				if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					Block block = event.getClickedBlock();
					converted = converted
							.replace("{x}", String.valueOf(block.getX()))
							.replace("{y}", String.valueOf(block.getY()))
							.replace("{z}", String.valueOf(block.getZ()));
				} else {
					Block block = event.getClickedBlock().getRelative(event.getBlockFace());
					converted = converted
							.replace("{x}", String.valueOf(block.getX()))
							.replace("{y}", String.valueOf(block.getY()))
							.replace("{z}", String.valueOf(block.getZ()));
				}
			} else {
				Location loc = player.getLocation();
				converted = converted
					.replace("{x}", String.valueOf(loc.getBlockX()))
					.replace("{y}", String.valueOf(loc.getBlockY()))
					.replace("{z}", String.valueOf(loc.getBlockZ()));
			}
			
			converted = converted
					.replace("{world}", player.getWorld().getName())
					.replace("{caller}", player.getName())
					.replace("{calleruuid}", player.getUniqueId().toString());
			
			runPowertool(tool, converted, resolveCaller(tool.caller, null, player));
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPunchEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player)) {
			return;
		}

		Player player = (Player)event.getDamager();

		PowerTool tool = getTool(player.getInventory().getItemInMainHand(), player);
		if (tool != null) {
			Location loc = event.getEntity().getLocation();
			String converted = tool.message
					.replace("{x}", String.valueOf(loc.getBlockX()))
					.replace("{y}", String.valueOf(loc.getBlockY()))
					.replace("{z}", String.valueOf(loc.getBlockZ()))
					.replace("{world}", loc.getWorld().getName())
					.replace("{eid}", String.valueOf(event.getEntity().getEntityId()))
					.replace("{uuid}", event.getEntity().getUniqueId().toString());
			
			if (event.getEntity() instanceof Player) {
				converted = converted.replace("{player}", event.getEntity().getName());
			}
			
			runPowertool(tool, converted, resolveCaller(tool.caller, event.getEntity(), player));
			event.setCancelled(true);
		}
	}
	
	private void runPowertool(PowerTool tool, String converted, CommandSender player) {
		if (System.currentTimeMillis() < tool.cooldown) {
			return;
		}
		
		if (player == null) {
			return;
		}
		
		converted = ChatColor.translateAlternateColorCodes('&', converted);
		
		if (tool.command) {
			Bukkit.dispatchCommand(player, converted);
		} else if (player instanceof Player) {
			((Player)player).chat(converted);
		}
		
		tool.cooldown = System.currentTimeMillis() + cooldownDelay;
	}
	
	private CommandSender resolveCaller(String caller, Entity clicked, Player holder) {
		if (caller == null) {
			return holder;
		}
		
		if (caller.equals("")) {
			return Bukkit.getConsoleSender();
		}
		
		if (caller.equals("{player}")) {
			if (clicked instanceof Player) {
				return clicked;
			} else {
				return null;
			}
		}
		
		try {
			UUID id = UUID.fromString(caller);
			return Bukkit.getPlayer(id);
		} catch(IllegalArgumentException e) {
			return Bukkit.getPlayer(caller);
		}
	}
	
	private PowerTool getTool(ItemStack item, Player player) {
		if (item == null || item.getType().isBlock()) {
			return null;
		}
		
		Map<Material, PowerTool> tools = powertools.get(player);
		
		if (tools != null) {
            return tools.get(item.getType());
		}
		
		return null;
	}
	
	private static class PowerTool {
		final String message;
		final boolean command;
		long cooldown;
		final String caller;

		PowerTool(String message, String caller, boolean command) {
			this.message = message;
			this.command = command;
			this.caller = caller;
			cooldown = 0;
		}
	}
}
