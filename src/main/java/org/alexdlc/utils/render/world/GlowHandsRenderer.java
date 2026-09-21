package org.alexdlc.utils.render.world;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import org.alexdlc.feature.impl.visual.GlowHandsFeature;
import org.alexdlc.utils.render.post.PostFx;
import org.alexdlc.utils.render.post.FullscreenQuad;
import org.alexdlc.utils.render.post.PostPipelines;
import org.alexdlc.utils.render.post.KawaseBlur;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Optional;

public final class GlowHandsRenderer {
    private static final Vector4f CLEAR = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
    private static final int MAX_BLUR_LEVELS = 4;

    private static final int MASK_UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .get();
    private static final int HALO_UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec4()
            .putFloat()
            .get();
    private static final int TRAIL_UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec2()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .get();
    private static final int FLAME_UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec4()
            .putFloat()
            .get();
    private static final int[] DYE_RGB = {
            0xFF990000, 0xFFFF5555, 0xFFCC00CC, 0xFF333399,
            0xFF191970, 0xFF109C15, 0xFFE4E467, 0xFF616161,
            0xFF333333, 0xFF4E4E4E, 0xFF404EAA, 0xFF7C2D12,
            0xFF1565C0, 0xFF222222, 0xFFF8F8F8, 0xFFD800D8
    };

    private final GpuBuffer maskUniforms = uniformBuffer("Alex DLC GlowHands Mask UBO", MASK_UNIFORM_SIZE);
    private final GpuBuffer haloUniforms = uniformBuffer("Alex DLC GlowHands Halo UBO", HALO_UNIFORM_SIZE);
    private final GpuBuffer trailUniforms = uniformBuffer("Alex DLC GlowHands Trail UBO", TRAIL_UNIFORM_SIZE);
    private final GpuBuffer flameUniforms = uniformBuffer("Alex DLC GlowHands Flame UBO", FLAME_UNIFORM_SIZE);

    private final KawaseBlur blur = new KawaseBlur(
            "alexdlc-glowhands-blur",
            PostPipelines.HAND_BLUR_DOWN,
            PostPipelines.HAND_BLUR_UP,
            "HandBlurUniforms",
            "HandBlurUniforms",
            KawaseBlur.HANDS_UNIFORM_SIZE,
            (builder, inputWidth, inputHeight, offset, alpha) -> builder
                    .putVec2(1.0F / inputWidth, 1.0F / inputHeight)
                    .putFloat(offset)
                    .putFloat(alpha)
    );
    private TextureTarget beforeTarget;
    private TextureTarget maskRawTarget;
    private TextureTarget maskTarget;
    private TextureTarget trailA;
    private TextureTarget trailB;
    private boolean trailUsesA;
    private boolean flameHistoryActive;

    public void render(GlowHandsFeature feature, Runnable handDraw) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        if (!validMainTarget(mainTarget)) {
            handDraw.run();
            return;
        }

        ensureTargets(mainTarget.width, mainTarget.height);
        if (!resourcesReady()) {
            handDraw.run();
            return;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                mainTarget.getColorTexture(),
                this.beforeTarget.getColorTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );
        encoder.copyTextureToTexture(
                mainTarget.getDepthTexture(),
                this.beforeTarget.getDepthTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );

        handDraw.run();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        writeMaskUniforms(feature, mainTarget.width, mainTarget.height);
        renderMask(mainTarget, sampler);

        int color = resolveGlowColor(feature, minecraft);
        float strength = feature.intensity.getValue().floatValue();
        float radius = feature.radius.getValue().floatValue();

        GpuTextureView blurredMask = blurTexture(
                this.maskTarget.getColorTextureView(),
                mainTarget.width,
                mainTarget.height,
                radius,
                true,
                sampler
        );
        if (feature.trail.getValue()) {
            float time = PostFx.shaderTime();
            writeTrailUniforms(feature, mainTarget.width, mainTarget.height, time);
            GpuTextureView flame = renderTrail(blurredMask, sampler);
            writeFlameUniforms(color, strength, time);
            renderFlame(mainTarget, flame, sampler);
            this.flameHistoryActive = true;
        } else {
            clearFlameHistoryIfNeeded();
            writeHaloUniforms(color, strength, radius);
            renderHalo(mainTarget, blurredMask, sampler);
        }
    }

    public void clearHistory() {
        this.trailUsesA = false;
        this.flameHistoryActive = false;
        clear(this.trailA);
        clear(this.trailB);
    }

    public void release() {
        this.beforeTarget = destroy(this.beforeTarget);
        this.maskRawTarget = destroy(this.maskRawTarget);
        this.maskTarget = destroy(this.maskTarget);
        this.trailA = destroy(this.trailA);
        this.trailB = destroy(this.trailB);
        this.blur.release();
        this.maskUniforms.close();
        this.haloUniforms.close();
        this.trailUniforms.close();
        this.flameUniforms.close();
    }

    private static TextureTarget destroy(TextureTarget target) {
        if (target != null) {
            target.destroyBuffers();
        }
        return null;
    }

    private void clearFlameHistoryIfNeeded() {
        if (this.flameHistoryActive) {
            clearHistory();
        }
    }

    private void renderMask(RenderTarget mainTarget, GpuSampler sampler) {
        try (RenderPass pass = renderPass("Alex DLC GlowHands mask", this.maskRawTarget)) {
            pass.setPipeline(PostPipelines.HAND_MASK);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandMaskUniforms", this.maskUniforms);
            pass.bindTexture("BeforeTexture", this.beforeTarget.getColorTextureView(), sampler);
            pass.bindTexture("AfterTexture", mainTarget.getColorTextureView(), sampler);
            pass.bindTexture("BeforeDepth", this.beforeTarget.getDepthTextureView(), sampler);
            pass.bindTexture("AfterDepth", mainTarget.getDepthTextureView(), sampler);
            draw(pass);
        }
        try (RenderPass pass = renderPass("Alex DLC GlowHands mask smooth", this.maskTarget)) {
            pass.setPipeline(PostPipelines.HAND_MASK_SMOOTH);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandMaskUniforms", this.maskUniforms);
            pass.bindTexture("RawMask", this.maskRawTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private void renderHalo(RenderTarget mainTarget,
                            GpuTextureView blurredMask,
                            GpuSampler sampler) {
        try (RenderPass pass = renderPass("Alex DLC GlowHands halo", mainTarget)) {
            pass.setPipeline(PostPipelines.HAND_HALO);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandHaloUniforms", this.haloUniforms);
            pass.bindTexture("BlurredSampler", blurredMask, sampler);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private GpuTextureView renderTrail(GpuTextureView injectTexture, GpuSampler sampler) {
        TextureTarget source = this.trailUsesA ? this.trailA : this.trailB;
        TextureTarget destination = this.trailUsesA ? this.trailB : this.trailA;
        try (RenderPass pass = renderPass("Alex DLC GlowHands trail", destination)) {
            pass.setPipeline(PostPipelines.HAND_TRAIL);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandTrailUniforms", this.trailUniforms);
            pass.bindTexture("PrevSampler", source.getColorTextureView(), sampler);
            pass.bindTexture("InjectSampler", injectTexture, sampler);
            draw(pass);
        }
        this.trailUsesA = !this.trailUsesA;
        return destination.getColorTextureView();
    }

    private void renderFlame(RenderTarget mainTarget,
                             GpuTextureView flame,
                             GpuSampler sampler) {
        try (RenderPass pass = renderPass("Alex DLC GlowHands flame", mainTarget)) {
            pass.setPipeline(PostPipelines.SHADER_HANDS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandCompositeUniforms", this.flameUniforms);
            pass.bindTexture("BlurredSampler", flame, sampler);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private GpuTextureView blurTexture(GpuTextureView source,
                                       int width,
                                       int height,
                                       float radius,
                                       boolean boostAlpha,
                                       GpuSampler sampler) {
        int levels = Math.clamp((int) Math.ceil(radius / 16.0F), 1, MAX_BLUR_LEVELS);
        float offset = Math.max(0.5F, radius / 18.0F);
        return this.blur.run(
                source,
                width,
                height,
                levels,
                offset,
                offset,
                boostAlpha ? 1.12F : 1.0F,
                boostAlpha ? 1.04F : 1.0F,
                sampler
        );
    }

    private void writeMaskUniforms(GlowHandsFeature feature, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean left = feature.bothHands.getValue();
        boolean right = feature.bothHands.getValue();
        if (!feature.bothHands.getValue()) {
            HumanoidArm mainArm = minecraft.player == null ? HumanoidArm.RIGHT : minecraft.player.getMainArm();
            left = mainArm == HumanoidArm.LEFT;
            right = mainArm == HumanoidArm.RIGHT;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, MASK_UNIFORM_SIZE)
                    .putVec2(1.0F / width, 1.0F / height)
                    .putVec2(left ? 1.0F : 0.0F, right ? 1.0F : 0.0F)
                    .get();
            write(this.maskUniforms, data);
        }
    }

    private void writeHaloUniforms(int color, float strength, float radius) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, HALO_UNIFORM_SIZE)
                    .putVec4(
                            channel(color),
                            channel(color >> 8),
                            channel(color << 16),
                            Math.min(strength, 3.0F)
                    )
                    .putFloat(radius / 12.0F)
                    .get();
            write(this.haloUniforms, data);
        }
    }

    private void writeTrailUniforms(GlowHandsFeature feature, int width, int height, float time) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, TRAIL_UNIFORM_SIZE)
                    .putVec2(1.0F / width, 1.0F / height)
                    .putFloat(0.82F + feature.trailLength.getValue().floatValue() * 0.14F)
                    .putFloat(1.0F)
                    .putFloat(time)
                    .putFloat(0.0F)
                    .get();
            write(this.trailUniforms, data);
        }
    }

    private void writeFlameUniforms(int color, float strength, float time) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, FLAME_UNIFORM_SIZE)
                    .putVec4(
                            channel(color),
                            channel(color >> 8),
                            channel(color << 16),
                            Math.min(strength, 3.0F)
                    )
                    .putFloat(time)
                    .get();
            write(this.flameUniforms, data);
        }
    }

    static int resolveGlowColor(GlowHandsFeature feature, Minecraft minecraft) {
        if (!feature.usesItemColor() || minecraft == null || minecraft.player == null) {
            return feature.customColor();
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) {
            return feature.customColor();
        }

        Item item = stack.getItem();
        if (item instanceof DyeItem dye) {
            int id = dye.getColor().getId();
            if (id >= 0 && id < DYE_RGB.length) {
                return DYE_RGB[id];
            }
            return feature.customColor();
        }
        int tint = minecraft.itemColors.getColor(stack, 0);
        if ((tint & 0xFFFFFF) != 0xFFFFFF && (tint & 0xFFFFFF) != 0) {
            return tint | 0xFF000000;
        }

        return switch (item) {
            case SwordItem ignored -> 0xFFD2691E;
            case AxeItem ignored -> 0xFF6FA8C9;
            case PickaxeItem ignored -> 0xFF9B8CC9;
            case ShovelItem ignored -> 0xFF7FB069;
            case HoeItem ignored -> 0xFFB08968;
            case BowItem ignored, CrossbowItem ignored -> 0xFF86C232;
            case FishingRodItem ignored -> 0xFF9C6644;
            case PotionItem ignored -> 0xFFB197FC;
            case ArmorItem ignored -> 0xFF8D99AE;
            default -> feature.customColor();
        };
    }

    private void ensureTargets(int width, int height) {
        boolean resetTrail = this.trailA == null
                || this.trailB == null
                || this.trailA.width != width
                || this.trailA.height != height
                || this.trailB.width != width
                || this.trailB.height != height;
        this.beforeTarget = ensureTarget(this.beforeTarget, "alexdlc-glowhands-before", width, height, true, PostPipelines.EFFECT_FORMAT);
        this.maskRawTarget = ensureTarget(this.maskRawTarget, "alexdlc-glowhands-mask-raw", width, height, false, PostPipelines.MASK_RAW_FORMAT);
        this.maskTarget = ensureTarget(this.maskTarget, "alexdlc-glowhands-mask", width, height, false, PostPipelines.EFFECT_FORMAT);
        this.trailA = ensureTarget(this.trailA, "alexdlc-glowhands-trail-a", width, height, false, PostPipelines.EFFECT_FORMAT);
        this.trailB = ensureTarget(this.trailB, "alexdlc-glowhands-trail-b", width, height, false, PostPipelines.EFFECT_FORMAT);
        if (resetTrail) {
            clearHistory();
        }
    }

    private TextureTarget ensureTarget(TextureTarget target,
                                       String label,
                                       int width,
                                       int height,
                                       boolean useDepth,
                                       GpuFormat format) {
        if (target != null && target.width == width && target.height == height) {
            return target;
        }
        if (target != null) {
            target.destroyBuffers();
        }
        return new TextureTarget(label, width, height, useDepth, format);
    }

    private boolean resourcesReady() {
        return ready(this.beforeTarget)
                && ready(this.maskRawTarget)
                && ready(this.maskTarget)
                && ready(this.trailA)
                && ready(this.trailB)
                && this.beforeTarget.getDepthTexture() != null
                && this.beforeTarget.getDepthTextureView() != null;
    }

    private static boolean ready(TextureTarget target) {
        return target != null
                && target.getColorTexture() != null
                && target.getColorTextureView() != null;
    }

    private static boolean validMainTarget(RenderTarget target) {
        return target != null
                && target.width > 0
                && target.height > 0
                && target.getColorTexture() != null
                && target.getColorTextureView() != null
                && target.getDepthTexture() != null
                && target.getDepthTextureView() != null;
    }

    private static RenderPass renderPass(String label, RenderTarget target) {
        return RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> label,
                target.getColorTextureView(),
                Optional.empty()
        );
    }

    private static void draw(RenderPass pass) {
        pass.setVertexBuffer(0, FullscreenQuad.buffer().slice());
        pass.draw(FullscreenQuad.vertexCount(), 1, 0, 0);
    }

    private static void clear(TextureTarget target) {
        if (target != null && target.getColorTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(target.getColorTexture(), CLEAR);
        }
    }

    private static void write(GpuBuffer buffer, ByteBuffer data) {
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
    }

    private static float channel(int value) {
        return (value & 0xFF) / 255.0F;
    }

    private static GpuBuffer uniformBuffer(String label, int size) {
        return RenderSystem.getDevice().createBuffer(
                () -> label,
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                size
        );
    }
}
