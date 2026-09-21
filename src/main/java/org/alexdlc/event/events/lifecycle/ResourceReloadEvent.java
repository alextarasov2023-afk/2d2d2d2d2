package org.alexdlc.event.events.lifecycle;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.alexdlc.event.Event;

@Getter
public final class ResourceReloadEvent extends Event {
    private Minecraft client;

    public ResourceReloadEvent set(Minecraft client) {
        this.client = client;
        return this;
    }
}
