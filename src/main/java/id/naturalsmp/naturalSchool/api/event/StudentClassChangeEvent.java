package id.naturalsmp.naturalSchool.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class StudentClassChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int oldClass;
    private final int newClass;
    private boolean cancelled;

    public StudentClassChangeEvent(@NotNull Player player, int oldClass, int newClass) {
        this.player = player;
        this.oldClass = oldClass;
        this.newClass = newClass;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public int getOldClass() {
        return oldClass;
    }

    public int getNewClass() {
        return newClass;
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
