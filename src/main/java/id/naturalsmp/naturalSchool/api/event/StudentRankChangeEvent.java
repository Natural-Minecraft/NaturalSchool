package id.naturalsmp.naturalSchool.api.event;

import id.naturalsmp.naturalSchool.profile.SchoolRank;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class StudentRankChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SchoolRank oldRank;
    private final SchoolRank newRank;
    private boolean cancelled;

    public StudentRankChangeEvent(@NotNull Player player, @NotNull SchoolRank oldRank, @NotNull SchoolRank newRank) {
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public SchoolRank getOldRank() {
        return oldRank;
    }

    @NotNull
    public SchoolRank getNewRank() {
        return newRank;
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
