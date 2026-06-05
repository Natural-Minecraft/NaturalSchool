package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import org.bukkit.entity.Player;
import java.util.UUID;

public interface BedrockHandler {
    boolean isBedrockPlayer(UUID uuid);
    void openStep1(Player player);
    void openStep2(Player player);
    void openStep3(Player player);
    void openForm(Player player, SchoolMenuType menuType);
}
