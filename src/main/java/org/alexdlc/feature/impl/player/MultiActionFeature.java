package org.alexdlc.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class MultiActionFeature extends Feature {
    public MultiActionFeature() {
        super("MultiAction", "Attack and mine while using items", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft client = event.getClient();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (!player.isUsingItem() || !client.options.keyAttack.isDown()) {
            return;
        }

        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            if (player.getAttackStrengthScale(0.0F) >= 1.0F) {
                client.gameMode.attack(player, entityHit.getEntity());
                player.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            client.gameMode.continueDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
        }
        player.swing(InteractionHand.MAIN_HAND);
    }
}
