package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.factory.BedrockFormFactory;
import id.naturalsmp.naturalSchool.ui.factory.JavaDialogFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class UIManager {

    private final NaturalSchool plugin;
    private final JavaDialogFactory javaDialogFactory;
    private final BedrockFormFactory bedrockFormFactory;
    private final boolean floodgateEnabled;

    public UIManager(NaturalSchool plugin) {
        this.plugin = plugin;
        this.javaDialogFactory = new JavaDialogFactory(plugin);
        this.bedrockFormFactory = new BedrockFormFactory(plugin);
        this.floodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
    }

    /**
     * Opens a specific school menu for the given player, routing based on connection type.
     *
     * @param player   the player opening the menu
     * @param menuType the type of menu to open
     */
    public void openMenu(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) {
            return;
        }

        if (isBedrockPlayer(player)) {
            bedrockFormFactory.openForm(player, menuType);
        } else {
            javaDialogFactory.openDialog(player, menuType);
        }
    }

    /**
     * Check if the player is a Bedrock Edition player via Floodgate API.
     *
     * @param player the player to check
     * @return true if the player is connected via Floodgate, false otherwise
     */
    private boolean isBedrockPlayer(Player player) {
        if (!floodgateEnabled) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }
}
