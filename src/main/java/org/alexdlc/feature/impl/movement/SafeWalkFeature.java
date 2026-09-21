package org.alexdlc.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.alexdlc.context.MinecraftContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.input.PlayerInputEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class SafeWalkFeature extends Feature implements MinecraftContext {
    public SafeWalkFeature() {
        super("SafeWalk", "Prevents walking off block edges", FeatureCategory.MOVEMENT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        LocalPlayer player = player();
        if (player == null || level() == null || !player.onGround()) {
            return;
        }

        BlockPos below = player.blockPosition().below();
        if (level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
            event.setShift(true);
        }
    }
}
