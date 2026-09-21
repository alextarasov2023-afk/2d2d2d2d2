package org.alexdlc.feature.impl.combat;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.alexdlc.context.MinecraftContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.packet.PacketReceiveEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class VelocityFeature extends Feature implements MinecraftContext {
    public VelocityFeature() {
        super("Velocity", "Removes knockback", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPhase() != PacketReceiveEvent.Phase.PRE || player() == null) {
            return;
        }
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.id() == player().getId()) {
            event.cancel();
        }
    }
}
