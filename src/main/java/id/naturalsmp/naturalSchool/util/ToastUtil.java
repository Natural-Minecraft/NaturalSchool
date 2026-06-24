package id.naturalsmp.naturalSchool.util;

import id.naturalsmp.naturalSchool.NaturalSchool;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import com.google.gson.JsonObject;

import java.util.UUID;

public class ToastUtil {

    public static void sendToast(NaturalSchool plugin, Player player, String title, String description, String icon, String frame) {
        NamespacedKey key = new NamespacedKey(plugin, "toast_" + UUID.randomUUID().toString().replace("-", ""));

        JsonObject json = new JsonObject();
        
        JsonObject display = new JsonObject();
        JsonObject iconObj = new JsonObject();
        iconObj.addProperty("item", icon != null ? icon : "minecraft:paper");
        display.add("icon", iconObj);
        
        JsonObject titleComponent = new JsonObject();
        titleComponent.addProperty("text", title);
        display.add("title", titleComponent);
        
        JsonObject descComponent = new JsonObject();
        descComponent.addProperty("text", description);
        display.add("description", descComponent);
        
        display.addProperty("frame", frame != null ? frame : "task"); // task, goal, challenge
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", false);
        display.addProperty("hidden", true);
        
        json.add("display", display);
        
        JsonObject criteria = new JsonObject();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");
        criteria.add("trigger", trigger);
        json.add("criteria", criteria);

        String jsonString = json.toString();

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Advancement advancement = Bukkit.getUnsafe().loadAdvancement(key, jsonString);
                    if (advancement == null) return;

                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    for (String criterion : progress.getRemainingCriteria()) {
                        progress.awardCriteria(criterion);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            AdvancementProgress prog = player.getAdvancementProgress(advancement);
                            for (String criterion : prog.getAwardedCriteria()) {
                                prog.revokeCriteria(criterion);
                            }
                            Bukkit.getUnsafe().removeAdvancement(key);
                        } catch (Exception ignored) {}
                    }, 5L);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to display Toast Notification: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Toast Notification: " + e.getMessage());
        }
    }
}
