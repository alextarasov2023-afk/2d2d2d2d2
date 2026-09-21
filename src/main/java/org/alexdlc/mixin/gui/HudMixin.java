package org.alexdlc.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.world.scores.Objective;
import org.alexdlc.context.RenderContext;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.impl.visual.CrosshairFeature;
import org.alexdlc.feature.impl.visual.HudFeature;
import org.alexdlc.feature.impl.visual.RemovalsFeature;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.Events;
import org.alexdlc.event.events.render.Render2DEvent;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onDisplayScoreboardSidebar(GuiGraphicsExtractor guiGraphicsExtractor, Objective objective, CallbackInfo ci) {
        if (RemovalsFeature.shouldRemoveScoreboard()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean alexdlc$hotbarDecorationsShifted;

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void alexdlc$shiftHotbarDecorations(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        float offset = HudFeature.hotbarDecorationOffset(this.minecraft);
        this.alexdlc$hotbarDecorationsShifted = offset != 0.0F;
        if (this.alexdlc$hotbarDecorationsShifted) {
            guiGraphicsExtractor.pose().pushMatrix();
            guiGraphicsExtractor.pose().translate(0.0F, offset);
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void alexdlc$unshiftHotbarDecorations(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.alexdlc$hotbarDecorationsShifted) {
            guiGraphicsExtractor.pose().popMatrix();
            this.alexdlc$hotbarDecorationsShifted = false;
        }
    }

    @WrapOperation(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
            )
    )
    private void alexdlc$scalePlayerStatus(
            Hud instance,
            GuiGraphicsExtractor guiGraphicsExtractor,
            Operation<Void> original
    ) {
        alexdlc$withScaledStatus(guiGraphicsExtractor, () -> original.call(instance, guiGraphicsExtractor));
    }

    @WrapOperation(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractVehicleHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
            )
    )
    private void alexdlc$scaleVehicleStatus(
            Hud instance,
            GuiGraphicsExtractor guiGraphicsExtractor,
            Operation<Void> original
    ) {
        alexdlc$withScaledStatus(guiGraphicsExtractor, () -> original.call(instance, guiGraphicsExtractor));
    }

    @Unique
    private void alexdlc$withScaledStatus(GuiGraphicsExtractor guiGraphicsExtractor, Runnable draw) {
        float scale = HudFeature.hotbarDecorationScale(this.minecraft);
        if (Math.abs(scale - 1.0F) < 0.001F) {
            draw.run();
            return;
        }

        float pivotX = this.minecraft.getWindow().getGuiScaledWidth() / 2.0F;
        float pivotY = this.minecraft.getWindow().getGuiScaledHeight() - 22.0F;
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor.pose().translate(pivotX, pivotY);
        guiGraphicsExtractor.pose().scale(scale);
        guiGraphicsExtractor.pose().translate(-pivotX, -pivotY);
        try {
            draw.run();
        } finally {
            guiGraphicsExtractor.pose().popMatrix();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void alexdlc$hideVanillaHotbar(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudFeature.customHotbarActive()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "extractPlayerHealth", constant = @Constant(intValue = 91))
    private int alexdlc$alignPlayerHealthRows(int original) {
        return HudFeature.hotbarStatsHalfWidth(this.minecraft, original);
    }

    @ModifyConstant(method = "extractVehicleHealth", constant = @Constant(intValue = 91))
    private int alexdlc$alignVehicleHealthRow(int original) {
        return HudFeature.hotbarStatsHalfWidth(this.minecraft, original);
    }

    @Redirect(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            )
    )
    private void alexdlc$skipXpBarBackground(ContextualBar bar, GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
        if (!(bar instanceof ExperienceBar) || !HudFeature.customHotbarActive()) {
            bar.extractBackground(guiGraphicsExtractor, deltaTracker);
        }
    }

    @Redirect(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"
            )
    )
    private void alexdlc$skipXpLevel(GuiGraphicsExtractor guiGraphicsExtractor, Font font, int experienceLevel) {
        if (!HudFeature.customHotbarActive()) {
            ContextualBar.extractExperienceLevel(guiGraphicsExtractor, font, experienceLevel);
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void alexdlc$hideVanillaSelectedItemName(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo ci) {
        if (HudFeature.customHotbarActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideVanillaCrosshair(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (FeatureManager.INSTANCE.getEnabled(CrosshairFeature.class) != null) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderContext.enter2D(null, guiGraphicsExtractor, deltaTracker);
        try {
            Render2DUtil.beginFrame();

            if (EventManager.hasListeners(Render2DEvent.class)) {
                EventManager.call(Events.RENDER_2D.set(this.minecraft, null, guiGraphicsExtractor, deltaTracker));
            }

            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }
}
