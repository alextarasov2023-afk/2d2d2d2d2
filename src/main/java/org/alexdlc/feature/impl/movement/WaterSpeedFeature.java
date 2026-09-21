package org.alexdlc.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.PlayerContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class WaterSpeedFeature extends Feature implements PlayerContext {
    public final NumberSetting boost = register(new NumberSetting("Boost", 1.05, 1.01, 1.2, 0.01, "x"));

    public WaterSpeedFeature() {
        super("WaterSpeed", "Swim faster", FeatureCategory.MOVEMENT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player == null || !player.isSwimming() || !isMoving()) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * boost.getValue(), movement.y, movement.z * boost.getValue());
    }
}
