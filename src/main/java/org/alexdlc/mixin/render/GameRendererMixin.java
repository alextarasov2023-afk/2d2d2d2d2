package org.alexdlc.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.alexdlc.context.RenderContext;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.impl.visual.GlowHandsFeature;
import org.alexdlc.feature.impl.visual.OutlineHandsFeature;
import org.alexdlc.feature.impl.visual.RemovalsFeature;
import org.alexdlc.feature.impl.visual.ShaderHandsFeature;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.Events;
import org.alexdlc.event.events.render.Render3DEvent;
import org.alexdlc.utils.render.Render3DUtil;
import org.alexdlc.utils.render.world.ShaderHandsRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static ShaderHandsRenderer shaderHandsRenderer;
    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            )
    )
    private Matrix4f captureLevelProjection(Matrix4f projectionMatrix) {
        Render3DUtil.captureLevelProjection(projectionMatrix);
        return projectionMatrix;
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (RemovalsFeature.shouldRemoveShaking()) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
            )
    )
    private void renderShaderHands(FeatureRenderDispatcher dispatcher,
                                   SubmitNodeStorage storage,
                                   Operation<Void> original) {
        ShaderHandsFeature fill = FeatureManager.INSTANCE.getEnabled(ShaderHandsFeature.class);
        GlowHandsFeature glow = FeatureManager.INSTANCE.getEnabled(GlowHandsFeature.class);
        OutlineHandsFeature outline = FeatureManager.INSTANCE.getEnabled(OutlineHandsFeature.class);

        if (fill == null && glow == null && outline == null) {
            if (shaderHandsRenderer != null) {
                shaderHandsRenderer.release();
                shaderHandsRenderer = null;
            }
            original.call(dispatcher, storage);
            return;
        }
        if (shaderHandsRenderer == null) {
            shaderHandsRenderer = new ShaderHandsRenderer();
        }
        shaderHandsRenderer.render(() -> original.call(dispatcher, storage));
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRender3D(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!EventManager.hasListeners(Render3DEvent.class)) {
            return;
        }
        RenderContext.enter3D((GameRenderer) (Object) this, deltaTracker);
        try {
            EventManager.call(Events.RENDER_3D.set(this.minecraft, (GameRenderer) (Object) this, deltaTracker));
        } finally {
            RenderContext.exit3D();
        }
    }
}
