package org.alexdlc.feature.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class NoJumpBoostFeature extends Feature {
    public NoJumpBoostFeature() {
        super("NoJumpBoost", "Removes jump boost effect", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player != null && player.hasEffect(MobEffects.JUMP_BOOST)) {
            player.removeEffect(MobEffects.JUMP_BOOST);
        }
    }
}
