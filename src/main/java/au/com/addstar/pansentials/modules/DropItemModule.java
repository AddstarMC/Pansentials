package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.MasterPlugin;
import au.com.addstar.pansentials.Module;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropItemModule implements Module, CommandExecutor, Listener{
	
	private MasterPlugin plugin;
	private List<Player> active;
    private final Map<String, Item> items = new HashMap<>();

	@Override
	public void onEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		plugin.getCommand("dropitem").setExecutor(this);
		active = new ArrayList<>();
		//Load from file
	}

	@Override
	public void onDisable() {
		plugin.getCommand("dropitem").setExecutor(null);
		active.clear();
		active = null;
		
		for (String key : new ArrayList<>(items.keySet())) {
			removeItem(key);
		}
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd,
			String[] args) {
		if(command.getName().equalsIgnoreCase("dropitem") && sender instanceof Player){
			Player ply = (Player)sender;
			if(active.contains(ply)){
				active.remove(ply);
				//Message
			}
			else{
				active.add((Player) sender);
				//Message
			}
			return true;
		}
		return false;
	}
	
	
	@EventHandler
	private void click(PlayerInteractEvent event){
		if(active.contains(event.getPlayer()) && event.getAction() == Action.LEFT_CLICK_BLOCK){
			Player ply = event.getPlayer();
			
			Location loc = event.getClickedBlock().getLocation();
			loc.setX(loc.getX() + 0.5);
			loc.setZ(loc.getZ() + 0.5);
			loc.setY(loc.getY() + 1.2);
			
			event.setCancelled(true);

			if (ply.getInventory().getItemInMainHand().getType() != Material.AIR) {
				spawnItem(ply.getInventory().getItemInMainHand(), loc);
				//Message
			}
			else{
				if(items.containsKey(getLocationString(loc)))
					removeItem(getLocationString(loc));
				//Message
			}
		}
	}
	
	@EventHandler
	private void despawn(ItemDespawnEvent event){
		if(event.getEntity().hasMetadata("permitem")){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
    private void pickup(EntityPickupItemEvent event) {
		if(event.getItem().hasMetadata("permitem")){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	private void death(EntityDamageEvent event){
		if(event.getEntity().hasMetadata("permitem"))
			event.setCancelled(true);
	}
	
	@EventHandler
	private void disconnect(PlayerQuitEvent event){
		if(active.contains(event.getPlayer()))
			active.remove(event.getPlayer());
	}
	
	private void removeItem(String loc){
		Item item = items.get(loc);
		item.remove();
		items.remove(loc);
		
	}
	
	private String getLocationString(Location loc){
		return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
	}
	
	private void spawnItem(ItemStack item, Location loc){
		Item it = loc.getWorld().dropItem(loc, item);
		it.setVelocity(new Vector(0, 0, 0));
		it.setMetadata("permitem", new FixedMetadataValue(plugin, true));
		if(items.containsKey(getLocationString(loc)))
			removeItem(getLocationString(loc));
		items.put(getLocationString(loc), it);
	}
}
