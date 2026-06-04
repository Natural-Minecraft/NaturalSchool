package id.naturalsmp.naturalSchool.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class StudentPracticalToggleEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean passed;
    private boolean cancelled;

    public StudentPracticalToggleEvent(@NotNull Player player, boolean passed) {
        this.player = player;
        this.passed = passed;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public boolean isPassed() {
        return passed;
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
