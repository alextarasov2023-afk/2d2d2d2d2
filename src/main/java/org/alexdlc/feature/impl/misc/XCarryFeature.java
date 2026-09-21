package org.alexdlc.feature.impl.misc;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.packet.PacketSendEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class XCarryFeature extends Feature {
    private static final int PLAYER_INVENTORY_CONTAINER_ID = 0;

    public XCarryFeature() {
        super("XCarry", "Store items in the crafting grid", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPhase() != PacketSendEvent.Phase.PRE) {
            return;
        }
        if (event.getPacket() instanceof ServerboundContainerClosePacket packet
                && packet.getContainerId() == PLAYER_INVENTORY_CONTAINER_ID) {
            event.cancel();
        }
    }
}
