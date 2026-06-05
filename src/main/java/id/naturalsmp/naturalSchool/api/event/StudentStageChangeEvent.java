package id.naturalsmp.naturalSchool.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class StudentStageChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String oldStage;
    private final String newStage;
    private boolean cancelled;

    public StudentStageChangeEvent(@NotNull Player player, @NotNull String oldStage, @NotNull String newStage) {
        this.player = player;
        this.oldStage = oldStage;
        this.newStage = newStage;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getOldStage() {
        return oldStage;
    }

    @NotNull
    public String getNewStage() {
        return newStage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
