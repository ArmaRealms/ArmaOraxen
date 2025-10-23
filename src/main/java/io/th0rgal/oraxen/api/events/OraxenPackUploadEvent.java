package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Lets other plugins detect when a pack is uploaded
 */
public class OraxenPackUploadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final HostingProvider hostingProvider;

    public OraxenPackUploadEvent(HostingProvider hostingProvider) {
        this.hostingProvider = hostingProvider;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * @return The hosting provider used to upload the pack
     */
    public HostingProvider getHostingProvider() {
        return hostingProvider;
    }

    /**
     * @return The Pack-URL that will be sent to players
     */
    public String getPackUrl() {
        return hostingProvider.getPackURL();
    }

    /**
     * @return The hash of the resourcepack
     */
    public String getHash() {
        return hostingProvider.getOriginalSHA1();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
