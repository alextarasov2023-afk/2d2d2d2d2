package org.alexdlc.event.events.game;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.alexdlc.event.CancellableEvent;

@Getter
public final class AttackEvent extends CancellableEvent {
    private Minecraft client;

    public AttackEvent set(Minecraft client) {
        this.client = client;
        return this;
    }
}
