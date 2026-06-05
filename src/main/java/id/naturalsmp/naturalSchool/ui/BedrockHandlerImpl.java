package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.factory.BedrockFormFactory;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class BedrockHandlerImpl implements BedrockHandler {

    private final BedrockFormFactory bedrockFormFactory;

    public BedrockHandlerImpl(NaturalSchool plugin) {
        this.bedrockFormFactory = new BedrockFormFactory(plugin);
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void openStep1(Player player) {
        bedrockFormFactory.openStep1(player);
    }

    @Override
    public void openStep2(Player player) {
        bedrockFormFactory.openStep2(player);
    }

    @Override
    public void openStep3(Player player, boolean showWarning) {
        bedrockFormFactory.openStep3(player, showWarning);
    }

    @Override
    public void openForm(Player player, SchoolMenuType menuType) {
        bedrockFormFactory.openForm(player, menuType);
    }

    @Override
    public void openTestExam(Player player) {
        bedrockFormFactory.openTestExam(player);
    }

    @Override
    public void openExam1(Player player, boolean showWarning) {
        bedrockFormFactory.openExam1(player, showWarning);
    }

    @Override
    public void openExam2(Player player) {
        bedrockFormFactory.openExam2(player);
    }

    @Override
    public void openExam3(Player player, boolean showWarning) {
        bedrockFormFactory.openExam3(player, showWarning);
    }

    @Override
    public void openExam4(Player player) {
        bedrockFormFactory.openExam4(player);
    }

    @Override
    public void openExam5(Player player, boolean showWarning) {
        bedrockFormFactory.openExam5(player, showWarning);
    }
}
