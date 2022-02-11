package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.Map;

public class ServerPlaceholderCondition extends Condition {

	ServerPlaceholderCondition() {
		super("server-placeholder");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Missing requried option: 'server-name'");
		}

		if (!options.containsKey("placeholder-name")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-name' (placeholder name, no %)");
		}

		if (!options.containsKey("placeholder-value")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-value' (expected placeholder value)");
		}


		String placeholderName = (String) options.get("name");
		String expectedPlaceholderValue = (String) options.get("value");

		if (placeholderName.contains("%")) {
			throw new InvalidConfigurationException("Placeholder name must not contain percentage symbols");
		}

		String serverName = (String) options.get("server-name");
		Server server = Server.getServer(serverName);
		if (server.isOnline()) {
			Placeholder placeholder = server.getPlaceholder(placeholderName);
			final String actualPlaceholderValue;
			if (placeholder instanceof GlobalPlaceholder) {
				actualPlaceholderValue = ((GlobalPlaceholder) placeholder).getValue();
			} else if (placeholder instanceof PlayerPlaceholder) {
				actualPlaceholderValue = ((PlayerPlaceholder) placeholder).getValue(player);
			} else {
				throw new IllegalStateException();
			}
			return actualPlaceholderValue.equals(expectedPlaceholderValue);
		} else {
			Main.getPlugin().getLogger().info(String.format(
					"Cannot obtain placeholder %s for server %s, the server is offline. Consider adding condition checking if the server is online.",
					placeholderName, serverName));
			return false;
		}
	}
}
