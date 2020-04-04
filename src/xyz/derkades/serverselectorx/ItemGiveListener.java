package xyz.derkades.serverselectorx;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.tr7zw.changeme.nbtapi.NBTItem;

public class ItemGiveListener implements Listener {

	// Event priorities are set to high so the items are given after other plugins clear the player's inventory.

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration config = Main.getConfigurationManager().inventory;

		if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			debug("Clearning inventory for " + player.getName());
			final PlayerInventory inv = player.getInventory();
			inv.setContents(new ItemStack[inv.getContents().length]);
			inv.setStorageContents(new ItemStack[inv.getStorageContents().length]);
			inv.setArmorContents(new ItemStack[inv.getArmorContents().length]);
		}

		this.giveItems(player, "join");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldChange(final PlayerChangedWorldEvent event) {
		this.giveItems(event.getPlayer(), "world-switch");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(final PlayerRespawnEvent event) {
		this.giveItems(event.getPlayer(), "death");
	}

	@EventHandler
	public void onClear(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().equalsIgnoreCase("/clear")) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> this.giveItems(event.getPlayer(), "clear"), 1);
		}
	}

	public void giveItems(final Player player, final String type) {
		debug("Giving items to " + player.getName() + ". Reason: " + type);

		for (final Map.Entry<String, FileConfiguration> itemConfigEntry : Main.getConfigurationManager().items.entrySet()) {
			final String name = itemConfigEntry.getKey();
			final FileConfiguration config = itemConfigEntry.getValue();

			debug("Preparing to give item '" + name + "'");

			if (!config.getBoolean("give." + type)) {
				debug("Item skipped, give is disabled");
				continue;
			}

			if (config.getBoolean("give.permission")) {
				debug("Permissions are enabled, checking permission");
				final String permission = "ssx.item." + name;
				if (!player.hasPermission(permission)) {
					debug("Player does not have permission '" + permission + "', skipping item");
					continue;
				}
			}

			if (config.isList("worlds")) {
				debug("World whitelisting is enabled");
				// World whitelisting option is present
				if (!config.getStringList("worlds").contains(player.getWorld().getName())) {
					debug("Player is in a world that is not whitelisted (" + player.getWorld().getName() + ")");
					continue;
				}
			}

			debug("All checks done, giving item");
			
			if (!config.isConfigurationSection("item")) {
				player.sendMessage("Missing 'item' section in item config '" + name + "'");
				continue;
			}

			ItemStack item = Main.getItemBuilderFromItemSection(player, config.getConfigurationSection("item")).create();

			final NBTItem nbt = new NBTItem(item);
			nbt.setString("SSXItem", name);
			item = nbt.getItem();

			final int slot = config.getInt("give.inv-slot", 0);
			final int delay = config.getInt("give.delay", 0);
			debug("Give delay: " + delay);
			final PlayerInventory inv = player.getInventory();
			if (slot < 0) {
				if (!inv.containsAtLeast(item, item.getAmount())) {
					if (delay == 0) {
						inv.addItem(item);
					} else {
						final ItemStack itemF = item;
						Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> inv.addItem(itemF), delay);
					}
				}
			} else {
				if (delay == 0) {
					inv.setItem(slot, item);
				} else {
					final ItemStack itemF = item;
					Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> inv.setItem(slot, itemF), delay);
				}
			}
		}
	}

	private void debug(final String message) {
		if (Main.ITEM_DEBUG) {
			Main.getPlugin().getLogger().info("[item debug] " + message);
		}
	}

}