package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.MasterPlugin;
import au.com.addstar.pansentials.Module;
import au.com.addstar.pansentials.Utilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class RepairModule implements Module, CommandExecutor {
	
	private MasterPlugin plugin;
	
	@Override
	public void onEnable() {
		plugin.getCommand("repair").setExecutor(this);
	}
	
	@Override
	public void onDisable() {
		plugin.getCommand("repair").setExecutor(null);
	}
	
	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd,
							 String[] args) {
		if (command.getName().equalsIgnoreCase("repair") && sender instanceof Player) {
			Player ply = (Player) sender;
			if (args.length == 0) {
				ItemStack stack = ply.getInventory().getItemInMainHand();
				if (repairItem(stack)) {
					ply.updateInventory();
				} else {
					return false;
				}
				sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "repair.hand"));
			} else {
				if (args[0].equalsIgnoreCase("all")) {
					for (ItemStack stack : ply.getInventory().getContents()) {
						if (needsRepair(stack))
							repairItem(stack);
					}
					for (ItemStack stack : ply.getInventory().getArmorContents()) {
						if (needsRepair(stack)) repairItem(stack);
						
					}
					ply.updateInventory();
				}
				sender.sendMessage(Utilities.format(plugin.getFormatConfig(), "repair.all"));
			}
		}
		return true;
	}
	
	private boolean repairItem(ItemStack stack) {
		if (stack == null) return false;
		if (!(stack.getItemMeta() instanceof Damageable)) return false;
		Damageable dStack = (Damageable) stack.getItemMeta();
		dStack.setDamage(0);
		stack.setItemMeta(dStack);
		return true;
	}
	
	private boolean needsRepair(ItemStack stack) {
		if (stack == null)
			return false;

		if (!(stack.getItemMeta() instanceof Damageable))
			return false;

		Damageable meta = (Damageable) stack.getItemMeta();
			return meta.getDamage() != 0;
	}
}
