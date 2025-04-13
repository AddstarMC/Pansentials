package au.com.addstar.pansentials.modules;

import au.com.addstar.monolith.lookup.EntityDefinition;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.template.EntitySettings;
import au.com.addstar.monolith.template.EntityTemplate;
import au.com.addstar.monolith.template.internal.EntityTemplateSetting;
import au.com.addstar.monolith.util.Parser;
import au.com.addstar.monolith.util.Raytrace;
import au.com.addstar.monolith.util.Raytrace.Hit;
import au.com.addstar.monolith.util.Stringifier;
import au.com.addstar.pansentials.CommandModule;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpawnMobModule extends CommandModule {
	public SpawnMobModule() {
		super("spawnmob");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private EntityTemplate createEntity(String definition) {
		String[] options = definition.split("\\|");

		EntityDefinition def = Lookup.findEntityByName(options[0]);
		if (def == null)
			throw new IllegalArgumentException("Unknown entity type " + options[0]);

		if (!def.isSpawnable())
			throw new IllegalArgumentException(options[0] + " is not spawnable");

		EntityTemplate template = new EntityTemplate(def);

		// Try options
		if (options.length > 1) {
			for (int i = 1; i < options.length; ++i) {
				String option = options[i];
				String optionName;
				String optionValue;

				if (option.contains("=")) {
					String[] pair = option.split("=");
					optionName = pair[0];
					optionValue = pair[1];
				} else {
					optionName = option;
					optionValue = null;
				}

				boolean handled = false;
				for (EntityTemplateSetting setting : EntitySettings.values()) {
					if (!setting.appliesTo(def))
						continue;

					for (String name : setting.getNames()) {
						if (name.equalsIgnoreCase(optionName)) {
							if (template.isSet(setting))
								throw new IllegalArgumentException(optionName + " is already set");
						} else
							continue;

						if (optionValue == null) {
							if (setting.getDefault() instanceof Boolean) {
								Boolean bool = (Boolean) setting.getDefault();
								template.set(setting, !bool);
								handled = true;
							} else
								throw new IllegalArgumentException("Option " + name + " requires an argument");
						} else {
							try {
								Object value = Parser.parse(setting.getType(), optionValue);
								template.set(setting, value);
								handled = true;
							} catch (UnsupportedOperationException e) {
								throw new IllegalArgumentException("Unable to set " + optionName);
							}
						}
					}
				}

				if (!handled)
					throw new IllegalArgumentException("Unknown setting " + optionName);
			}
		}

		return template;
	}

	private void doHelp(CommandSender sender, String entityType) {
        if (entityType.equals("types")) {
          Set<String> type = Lookup.getAllEntityNames();
          int num = type.size();
          int numRows = num / 10;
          List<String> rows = new ArrayList<>(numRows);
            int curr = 0;
          for (String s : type) {
                if (rows.size() < curr + 1) {
                  rows.add(curr, "");
                }
                String currRow = rows.get(curr);
              ChatColor color;
                if(curr % 2 == 0)
                  color = ChatColor.GRAY;
                else
                  color = ChatColor.WHITE;
                currRow += color+ StringUtils.rightPad(s, 10)+ChatColor.RESET;
                rows.set(curr,currRow);
                if (curr == 9)
                    curr = 0;
                else
                    curr++;
            }
            sender.sendMessage("----List of entities----");
          for (String row :rows) {
            sender.sendMessage(row);
          }
          sender.sendMessage("--------");
          return;
        }
		EntityDefinition def = Lookup.findEntityByName(entityType);
		if (def == null) {
			sender.sendMessage(ChatColor.RED + "Unknown entity type " + entityType);
			return;
		}

		sender.sendMessage(ChatColor.WHITE + "Available options for " + ChatColor.YELLOW + entityType + ChatColor.GRAY + ":");
		for (EntityTemplateSetting setting : EntitySettings.values()) {
			if (!setting.appliesTo(def))
				continue;

			StringBuilder builder = new StringBuilder();
			builder.append(' ');
			builder.append(ChatColor.YELLOW);
			builder.append(setting.getNames()[0]);

			builder.append(ChatColor.GRAY);
			builder.append(':');
			builder.append(ChatColor.GOLD);
			builder.append(setting.getType().getSimpleName());

			if (setting.getDefault() != null) {
				builder.append(ChatColor.GRAY);
				builder.append(" default=");
				builder.append(ChatColor.WHITE);
				builder.append(Stringifier.toString(setting.getDefault()));
			}

			if (setting.getNames().length > 1) {
				builder.append("\n  ");
				builder.append(ChatColor.GRAY);
				builder.append("aliases: ");
				builder.append(ChatColor.WHITE);
				builder.append(StringUtils.join(setting.getNames(), ", ", 1, setting.getNames().length));
			}

			sender.sendMessage(builder.toString());
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		boolean showDebug = false;

		if (args.length == 0)
			return false;

		if (args[0].equalsIgnoreCase("help")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " help <entity name>");
        sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " help types");
        sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " <entitySpec> [mob count]");
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " [x,y,z] <entitySpec> [mob count]");
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " PlayerName[~x,~y,~z] <entitySpec> [mob count]");
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " #world[x,y,z] <entitySpec> [mob count]");
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " @a[~x,~y,~z] <entitySpec> [mob count]");
				sender.sendMessage(ChatColor.WHITE + "Usage /" + label + " @a#world[~x,~y,~z] <entitySpec> [mob count]");
			} else {
				// Show help on the given entity (typically mob name)
				doHelp(sender, args[1]);
			}

			return true;
		}

		if (showDebug)
			sender.sendMessage("Debug: Look for mobCount integer at far right");

		// Look for a mob count integer at the end
		int mobCount = 1;

		int startArgIndex = 0;
		int endArgIndex = args.length;

		if (args[args.length - 1].matches("[0-9]+")) {
			mobCount = Integer.parseInt(args[args.length - 1]);
			--endArgIndex;
		}

		// Look for coordinates as the first argument (arguments are space separated)
		// Examples:
		//
		// [x,y,z]               spawn entities at the given coordinates (not valid at console)
		// PlayerName[~x,~y,~z]  spawn at coordinates relative to the given player (not all terms must be relative)
		// @a[~x,~y,~z]          spawn at coordinates relative to all players on the server
		// #survival[15,65,43] slime|size=1 10    - Spawn 10 slimes of size 1 at absolute coordinates x=15,y=65,z=43 on survival
		// @a#survival[~5,~,~-15] slime|size=1 5  - Spawn 5 slimes at relative coordinates from every player on the survival world

		Pattern coordsPattern = Pattern.compile("([a-zA-Z0-9_@#]+)?\\[([0-9~-]+),([0-9~-]+),([0-9~-]+)\\]");
		Matcher reMatch = coordsPattern.matcher(args[0]);

		String coordX = "";
		String coordY = "";
		String coordZ = "";
		boolean relativeCoordinates = false;

		// List of player names to target
		// If using @a with absolute coordinates, instead has world names starting with #, for example #hub
		List<String> playerList = new ArrayList<>();

		if (showDebug)
			sender.sendMessage("Debug: Look for coordinate info");

		if (reMatch.find()) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Coordinates found in the first argument, but the entitySpec argument is missing");
				return true;
			}

			if (showDebug)
				sender.sendMessage("Debug: parse " + reMatch.group(0));

			// playerWorldSpec could be a specific playerName, @a, #worldName, @a#worldName, or blank

			String playerWorldSpec = "";

			if (reMatch.group(1) != null) {
				playerWorldSpec = reMatch.group(1);
			}

			coordX = reMatch.group(2);
			coordY = reMatch.group(3);
			coordZ = reMatch.group(4);

			boolean coordsParsed = false;

			if (showDebug)
				sender.sendMessage("Debug: groups found: " + playerWorldSpec + " / " + coordX + " / " + coordY + " / " + coordZ);

			if (coordX.startsWith("~") || coordY.startsWith("~") || coordZ.startsWith("~"))
				relativeCoordinates = true;

			if (playerWorldSpec.isEmpty()) {
				if ((sender instanceof Player)) {
					Player targetPlayer = (Player) sender;
					String playerName = targetPlayer.getName();
					playerList.add(playerName);
				} else {

					// When calling from the console, must use @a or PlayerName or #world
					ShowConsoleUsageWarning(sender);
					return true;
				}

				coordsParsed = true;
			}

			if (!coordsParsed && playerWorldSpec.equals("@a")) {
				// Add all players in all worlds if relative coordinates
				// Add just one player for each world if absolute coordinates

				if (showDebug)
					sender.sendMessage("Debug: found @a, Bukkit.getOnlinePlayers");

				for (Player player : Bukkit.getOnlinePlayers()) {
					if (showDebug)
						sender.sendMessage("Debug: found @a, add " + player.getName());

					if (relativeCoordinates) {
						// Store the player name
						playerList.add(player.getName());
					} else {
						// Store the world name, but only if we haven't parsed this world yet
						String playerWorld = "#" + player.getWorld().getName();

						if (!playerList.contains(playerWorld))
							playerList.add(playerWorld);
					}


				}

				if (playerList.isEmpty()) {
					sender.sendMessage(ChatColor.RED + "No online players found on this server");
					return true;
				}

				coordsParsed = true;
			}

			if (!coordsParsed && playerWorldSpec.startsWith("@a")) {
				// World was specified, for example @a#survival

				if (showDebug)
					sender.sendMessage("Debug: found @a plus other text (length= " + playerWorldSpec.length() + ")");

				String worldSpec = playerWorldSpec.substring(2);
				if (!worldSpec.startsWith("#")) {
					sender.sendMessage(ChatColor.RED + "Invalid syntax; use @a#worldName[x,y,z], not:" + playerWorldSpec);
					return true;
				}

				String worldName = worldSpec.substring(1);

				if (!AddWorldPlayers(sender, worldName, playerList, relativeCoordinates)) {
					// Warning message has already been shown
					return true;
				}

				coordsParsed = true;
			}

			if (!coordsParsed && playerWorldSpec.startsWith("#")) {
				String worldName = playerWorldSpec.substring(1);

				// Auto-remove @a if present; it is implied when using #world
				worldName.replace("@a", "");

				if (!AddWorldPlayers(sender, worldName, playerList, relativeCoordinates)) {
					// Warning message has already been shown
					return true;
				}

				coordsParsed = true;
			}

			if (!coordsParsed) {

				// Assume playerWorldSpec is a specific player name

				List<Player> players = Bukkit.getServer().matchPlayer(playerWorldSpec);
				if (players.isEmpty()) {
					sender.sendMessage(ChatColor.RED + "Player not found: " + playerWorldSpec);
					return true;
				}

				Player targetPlayer = players.get(0);

				playerList.add(targetPlayer.getName());

				if (showDebug)
					sender.sendMessage("Debug: target player " + targetPlayer.getName());

				coordsParsed = true;
			}

			if (showDebug)
				sender.sendMessage("Debug: coords " + coordX + "," + coordY + "," + coordZ + " for specified player list");

			startArgIndex = 1;
		}

		if (startArgIndex == 0 && (sender instanceof Player)) {

			Player targetPlayer = (Player) sender;

			// Find the target location using the point toward which the player is looking
			Raytrace trace = new Raytrace()
					.ignoreAllEntities()
					.hitAir(true);

			Location playerLoc = targetPlayer.getEyeLocation();
			Hit hit = trace.traceOnce(playerLoc, playerLoc.getDirection(), 50);
			Location target = hit.getLocation();

			coordX = String.valueOf(target.getX());
			coordY = String.valueOf(target.getY());
			coordZ = String.valueOf(target.getZ());

			String playerName = targetPlayer.getName();
			playerList.add(playerName);

			if (showDebug)
				sender.sendMessage("Debug: coords " + coordX + "," + coordY + "," + coordZ + " from Raytrace of " + targetPlayer);

		} else {

			// Likely called via the console
			if (startArgIndex == 0) {
				ShowConsoleUsageWarning(sender);
				return true;
			}

			// coordX, coordY, and coordZ are already defined
		}

		// Join the arguments using a space, optionally excluding the coordinate term from the beginning
		// and/or the mob count integer from the end
		String line = StringUtils.join(args, ' ', startArgIndex, endArgIndex);
		String[] mobs = line.split(",");

		String firstMob = "";
		String lastMob = "";

		try {
			List<EntityTemplate> templates = Lists.newArrayList();

			// Create the templates
			for (String mob : mobs) {
				if (mob.startsWith("^")) {
					// Mob index specified instead of name
					mob = mob.substring(1);

					String indexValueWarning = "Illegal value '^" + mob + "'; must be between 1 and " + templates.size();

					int index;
					if (mob.isEmpty())
						index = 1;
					else {
						try {
							index = Integer.parseInt(mob);
							if (index <= 0)
								throw new IllegalArgumentException(indexValueWarning);
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException(indexValueWarning);
						}
					}

					if (index > templates.size())
						throw new IllegalArgumentException(indexValueWarning);
					templates.add(templates.get(templates.size() - index));
				} else {
					templates.add(createEntity(mob));
				}

				if (firstMob.isEmpty())
					firstMob = mob;

				lastMob = mob;
			}

			// Spawn the entities around 1 or more players
			int playerCount = 0;
			int spawned = 0;

			for (String playerName : playerList) {

				World targetWorld;
				Double targetX;
				Double targetY;
				Double targetZ;

				if (playerName.startsWith("#")) {
					// Absolute coordinates on a given world

					String worldName = playerName.substring(1);

					if (worldName.isEmpty()) {
						// World name required
						ShowConsoleUsageWarning(sender);
						return true;
					}

					targetWorld = Bukkit.getWorld(worldName);
					if (targetWorld == null) {
						ShowWorldNotFound(sender, worldName);
						return true;
					}

					targetX = Double.parseDouble(coordX);
					targetY = Double.parseDouble(coordY);
					targetZ = Double.parseDouble(coordZ);

					if (showDebug)
						sender.sendMessage("Debug: spawning at coords " + targetX + "," + targetY + "," + targetZ + ", world " + targetWorld);

				} else {

					List<Player> players = Bukkit.getServer().matchPlayer(playerName);
					if (players.isEmpty())
						continue;

					Player targetPlayer = players.get(0);
					Location playerLocation = targetPlayer.getLocation();

					targetX = ResolveCoordinate(coordX, playerLocation.getX(), sender);
					targetY = ResolveCoordinate(coordY, playerLocation.getY(), sender);
					targetZ = ResolveCoordinate(coordZ, playerLocation.getZ(), sender);
					targetWorld = targetPlayer.getWorld();

					if (showDebug)
						sender.sendMessage("Debug: spawning at coords relative to " + playerName + ": " + targetX + "," + targetY + "," + targetZ + ", world " + targetWorld);
				}

				Location target = new Location(targetWorld, targetX, targetY, targetZ);

				for (int i = 0; i < mobCount; ++i) {
					Entity last = null;
					for (EntityTemplate template : templates) {
						Entity ent = template.createAt(target);
						if (ent == null)
							break;

						++spawned;
						if (last != null)
                            last.addPassenger(ent);
						last = ent;
					}
				}
				++playerCount;
			}

			if (playerCount > 1) {
				sender.sendMessage(ChatColor.GREEN + "Processed " + playerCount + " players and spawned " + spawned + " total entities");
			} else {
				if (spawned == 1)
					sender.sendMessage(ChatColor.GREEN + "Spawned 1 " + firstMob);
				else {
					if (firstMob.equalsIgnoreCase(lastMob))
						sender.sendMessage(ChatColor.GREEN + "Spawned " + spawned + " " + firstMob + "s");
					else
						sender.sendMessage(ChatColor.GREEN + "Spawned " + spawned + " entities (" + firstMob + " to " + lastMob + ")");
				}
			}

		} catch (IllegalArgumentException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
		}

		return true;
	}

	/**
     * Adds all players to a world
     * @param sender The CommandSender
     * @param worldName the World
     * @param playerList the list to add
     * @param relativeCoordinates the relative coords
     * @return boolean True if works
	 */
	private boolean AddWorldPlayers(CommandSender sender, String worldName, List<String> playerList, boolean relativeCoordinates) {

		boolean showDebug = false;

		if (showDebug)
			sender.sendMessage("Debug: world spec: " + worldName);

		if (worldName.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "Missing world name after the # sign");
			return false;
		}

		if (!(AddWorldPlayers(worldName, playerList, relativeCoordinates))) {
			ShowWorldNotFound(sender, worldName);
			return false;
		}

		if (playerList.isEmpty() && !relativeCoordinates) {
			// Store #world, meaning absolute coordinates are in use
			playerList.add("#" + worldName);
		}

		if (playerList.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "No online players found on world " + worldName);
			return false;
		}

		if (showDebug)
			sender.sendMessage("Debug: added " + playerList.size() + " player(s) for world " + worldName);

		return true;
	}

	private boolean AddWorldPlayers(String worldName, List<String> playerList, boolean relativeCoordinates) {

		// Get the world object
		World targetWorld = Bukkit.getWorld(worldName);
		if (targetWorld == null) {
			return false;
		}

		// Add all players on a given world
		// If relativeCoordinates == false, just add the first player found
		for (Player player : targetWorld.getPlayers()) {
			playerList.add(player.getName());
			if (!relativeCoordinates)
				break;
		}

		return true;
	}

	private Double ResolveCoordinate(String coordSpec, double playerCoord, CommandSender sender) {
		if (coordSpec.isEmpty() || coordSpec.equals("~")) {
			return playerCoord;
		}

		try {

			if (coordSpec.startsWith("~")) {
				Double relativeCoordinate = Double.parseDouble(coordSpec.substring(1));
				return playerCoord + relativeCoordinate;
			}

			return Double.parseDouble(coordSpec);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Error parsing coordinate " + coordSpec + "; defaulting to " + playerCoord);
			return playerCoord;
		}

	}

	private void ShowConsoleUsageWarning(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Must specify absolute coordinates with #world[x,y,z] since not in-game");
		sender.sendMessage(ChatColor.RED + "Or, use absolute and/or relative coordinates with PlayerName[x,y,z] or #worldName[x,y,z] or @a[x,y,z] or @a#worldName[x,y,z]");
	}

	private void ShowWorldNotFound(CommandSender sender, String worldName) {
		sender.sendMessage(ChatColor.RED + "World not found: " + worldName);

		if(!sender.hasPermission("multiverse.core.list.worlds")) {
			return;
		}

		// Display a list of available worlds

		List<String> worldNameList = new ArrayList<>();

		for (World item : Bukkit.getWorlds()) {
			worldNameList.add(item.getName());
		}

		String worldList = StringUtils.join(worldNameList, ", ");

		sender.sendMessage(ChatColor.YELLOW + "Options are: " + worldList);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
}
