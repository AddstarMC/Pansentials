package au.com.addstar.pansentials.modules;

import au.com.addstar.pansentials.MasterPlugin;
import au.com.addstar.pansentials.Module;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class PlayerListModule implements Module, CommandExecutor {

	private MasterPlugin plugin;
	private static Chat chat;

	private static final MiniMessage mm = MiniMessage.miniMessage();

	private static class PlayerInfo {
		private final String displayName;
		private final String suffix;

		public PlayerInfo(String displayName, String suffix) {
			this.displayName = displayName;
			this.suffix = suffix;
		}

		public String fullName() {
			String suffix = this.suffix != null ? this.suffix : "";
			return suffix + this.displayName;  // Raw minimessage string
		}
	}

	private static void setupVault() {
		RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
		if (rsp != null) {
			chat = rsp.getProvider();
		}
	}

	@Override
	public void onEnable() {
		setupVault();
		plugin.getCommand("mvw").setExecutor(this);
	}

	@Override
	public void onDisable() {
		plugin.getCommand("mvw").setExecutor(null);
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		Map<World, List<PlayerInfo>> worldPlayersMap = new HashMap<>();

		for (Player player : players) {
			boolean vanished = isVanished(player);
			if (!vanished || (vanished && canSeeVanished(sender))) {
				String suffix = (chat != null) ? chat.getPlayerSuffix(player) : "";
				worldPlayersMap.computeIfAbsent(player.getWorld(), k -> new ArrayList<>())
						.add(new PlayerInfo(player.getDisplayName(), suffix));
			}
		}

		sender.sendMessage(mm.deserialize("<aqua>--- Worlds and players (" + players.size() + "): ---</aqua>"));

		List<Map.Entry<World, List<PlayerInfo>>> worldEntries = new ArrayList<>(worldPlayersMap.entrySet());
		worldEntries.sort(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER));

		for (Map.Entry<World, List<PlayerInfo>> entry : worldEntries) {
			World world = entry.getKey();
			List<PlayerInfo> infos = entry.getValue();
			infos.sort(Comparator.comparing(pi -> pi.displayName, String.CASE_INSENSITIVE_ORDER));

			// Collect MiniMessage strings
			List<String> playerStrings = new ArrayList<>();
			for (PlayerInfo info : infos) {
				playerStrings.add(info.fullName());  // Raw minimessage string
			}

			String playersJoined = String.join("<reset><white>, </white>", playerStrings);
			String fullMessage = "<green>" + world.getName() + "</green> <yellow>(" + infos.size() + ")</yellow><white>: "
					+ playersJoined;

			sender.sendMessage(mm.deserialize(fullMessage));
		}

		return true;
	}

	private boolean isVanished(Player player) {
		if (player.hasMetadata("vanished")) {
			for (MetadataValue meta : player.getMetadata("vanished")) {
				if (meta.asBoolean()) return true;
			}
		}
		return false;
	}

	private boolean canSeeVanished(CommandSender sender) {
		if (sender instanceof Player player) {
			return player.hasPermission("pv.see") || player.hasPermission("vanish.see");
		}
		return true; // Console
	}
}
