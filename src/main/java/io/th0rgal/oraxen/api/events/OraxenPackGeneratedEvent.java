package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OraxenPackGeneratedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final List<VirtualFile> output;

    public OraxenPackGeneratedEvent(List<VirtualFile> output) {
        this.output = output;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public List<VirtualFile> getOutput() {
        return output;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
