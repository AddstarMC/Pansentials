package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.CommandModule;
import au.com.addstar.pansentials.Utilities;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;
import java.util.Map;

public class BurnCommand extends CommandModule implements Listener {
	private final Map<Player, Long> immunePlayers;
	
	public BurnCommand() {
		super("burn");
		
		immunePlayers = Maps.newHashMap();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }

        // Parse player
        final Player player = Bukkit.getPlayer(args[0]);

        if (player == null) {
            sender.sendMessage(Utilities.format(getPlugin().getFormatConfig(), "noPlayer", "%name%:" + args[0]));
            return true;
        }

        // Parse time
        long time = 0;
        try {
            time = Long.parseLong(args[1]);
            if (time < 0) {
                sender.sendMessage(Utilities.format(getPlugin().getFormatConfig(), "burn.invalid.time"));
                return true;
            }

            time *= 1000; // Was specified in seconds
        } catch (NumberFormatException e) {
            time = Utilities.parseDateDiff(args[1]);

            if (time == 0) {
                sender.sendMessage(Utilities.format(getPlugin().getFormatConfig(), "burn.invalid.time"));
                return true;
            }
        }

        // Should the player take damage?
        boolean takeDamage;

        takeDamage = args.length < 3 || !args[2].equalsIgnoreCase("none") && !args[2].equalsIgnoreCase("false");

        // Message to show to players
        String message;
        if (args.length >= 4) {
            message = ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, ' ', 3, args.length));
        } else {
            message = Utilities.format(getPlugin().getFormatConfig(), "burn.ignited");
        }

        int tickTime = (int) (time / 50);

        // Handle immunity
        if (!takeDamage) {
            immunePlayers.put(player, System.currentTimeMillis() + time);

            // Schedule task to remove the immunity
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> immunePlayers.remove(player), tickTime);
        }

        player.setFireTicks(tickTime);
        player.sendMessage(message);
        sender.sendMessage(Utilities.format(getPlugin().getFormatConfig(), "burn.done", "%player%:" + player.getDisplayName()));
        return true;
    }

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			return Utilities.matchStrings(args[0], Bukkit.getOnlinePlayers(), Utilities.PlayerName);
		}
		
		return null;
	}

	@EventHandler(ignoreCancelled=true)
	private void onPlayerBurn(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		
		if (event.getCause() != DamageCause.FIRE_TICK) {
			return;
		}
		
		Long expiryTime = immunePlayers.get(event.getEntity());
		
		// Block if not expired
		if (expiryTime != null && System.currentTimeMillis() < expiryTime) {
			event.setDamage(0);
			event.setCancelled(true);
		}
	}
}
