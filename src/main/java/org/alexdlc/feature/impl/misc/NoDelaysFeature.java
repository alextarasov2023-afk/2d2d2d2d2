package org.alexdlc.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.mixin.accessor.LivingEntityAccessor;
import org.alexdlc.mixin.accessor.MinecraftAccessor;
import org.alexdlc.mixin.accessor.MultiPlayerGameModeAccessor;

public final class NoDelaysFeature extends Feature {
    private static final int DEFAULT_JUMP_DELAY = 10;
    private static final int DEFAULT_RIGHT_CLICK_DELAY = 4;
    private static final int DEFAULT_BLOCK_BREAK_DELAY = 5;

    public final BooleanSetting jump = register(new BooleanSetting("Jump", true));
    public final BooleanSetting rightClick = register(new BooleanSetting("Right Click", false));
    public final BooleanSetting experienceBottlesOnly = register(new BooleanSetting(
            "Experience Bottles Only", false
    ).visibleWhen(this.rightClick::getValue));
    public final BooleanSetting blockBreak = register(new BooleanSetting("Block Break", false));

    public NoDelaysFeature() {
        super("NoDelays", "Removes selected player action delays", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft client = event.getClient();
        if (client.player == null) {
            return;
        }

        if (jump.getValue()) {
            ((LivingEntityAccessor) client.player).setNoJumpDelay(0);
        }
        if (rightClick.getValue()
                && (!this.experienceBottlesOnly.getValue()
                || client.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
                || client.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE))) {
            ((MinecraftAccessor) client).setRightClickDelay(0);
        }
        if (blockBreak.getValue() && client.gameMode != null) {
            ((MultiPlayerGameModeAccessor) client.gameMode).setDestroyDelay(0);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ((LivingEntityAccessor) client.player).setNoJumpDelay(DEFAULT_JUMP_DELAY);
        }
        ((MinecraftAccessor) client).setRightClickDelay(DEFAULT_RIGHT_CLICK_DELAY);
        if (client.gameMode != null) {
            ((MultiPlayerGameModeAccessor) client.gameMode).setDestroyDelay(DEFAULT_BLOCK_BREAK_DELAY);
        }
    }
}
