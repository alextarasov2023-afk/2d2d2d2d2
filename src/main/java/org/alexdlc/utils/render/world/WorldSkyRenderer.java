package org.alexdlc.utils.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.alexdlc.feature.impl.visual.ShaderSkyFeature;
import org.alexdlc.utils.render.post.PostFx;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Render3DUtil;
import org.alexdlc.utils.render.post.FullscreenQuad;
import org.alexdlc.utils.render.post.PostPipelines;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Optional;

public final class WorldSkyRenderer {
    private static final int SKY_UNIFORM_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private final GpuBuffer skyUniforms = uniformBuffer(
            "Alex DLC ShaderSky UBO",
            SKY_UNIFORM_SIZE
    );
    private TextureTarget skyClouds;

    public void render(ShaderSkyFeature feature, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        if (!valid(target) || cameraState == null || !cameraState.initialized) {
            return;
        }
        Matrix4f projection = Render3DUtil.levelProjectionCopy();
        if (projection == null) {
            return;
        }
        Matrix4f inverseViewProjection = projection
                .mul(cameraState.viewRotationMatrix)
                .invert();
        writeSkyUniforms(feature, inverseViewProjection, target.width, target.height);
        ensureSkyClouds(target.width, target.height);

        RenderPipeline cloudsPipeline;
        RenderPipeline compositePipeline;
        switch (feature.shader.getValue()) {
            case ShaderSkyFeature.SKY_NEBULA -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_NEBULA;
                compositePipeline = PostPipelines.WORLD_SKY_NEBULA;
            }
            case ShaderSkyFeature.SKY_PLASMA -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_PLASMA;
                compositePipeline = PostPipelines.WORLD_SKY_PLASMA;
            }
            case ShaderSkyFeature.SKY_AURORA -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_AURORA;
                compositePipeline = PostPipelines.WORLD_SKY_AURORA;
            }
            case ShaderSkyFeature.SKY_GALAXY -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_GALAXY;
                compositePipeline = PostPipelines.WORLD_SKY_GALAXY;
            }
            case ShaderSkyFeature.SKY_SUNSET -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_SUNSET;
                compositePipeline = PostPipelines.WORLD_SKY_SUNSET;
            }
            default -> {
                cloudsPipeline = PostPipelines.WORLD_SKY_CLOUDS_DEEP_SPACE;
                compositePipeline = PostPipelines.WORLD_SKY_DEEP_SPACE;
            }
        }

        GpuSampler depthSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        GpuSampler cloudSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Alex DLC ShaderSky clouds",
                this.skyClouds.getColorTextureView(),
                Optional.empty()
        )) {
            pass.setPipeline(cloudsPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("WorldSkyUniforms", this.skyUniforms);
            pass.bindTexture("DepthSampler", target.getDepthTextureView(), depthSampler);
            drawFullscreen(pass);
        }

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Alex DLC ShaderSky",
                target.getColorTextureView(),
                Optional.empty()
        )) {
            pass.setPipeline(compositePipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("WorldSkyUniforms", this.skyUniforms);
            pass.bindTexture("DepthSampler", target.getDepthTextureView(), depthSampler);
            pass.bindTexture("CloudSampler", this.skyClouds.getColorTextureView(), cloudSampler);
            drawFullscreen(pass);
        }
    }

    private void writeSkyUniforms(ShaderSkyFeature feature,
                                  Matrix4f inverseViewProjection,
                                  int width,
                                  int height) {
        int primary = feature.resolvedColor1();
        int secondary = feature.resolvedColor2();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, SKY_UNIFORM_SIZE)
                    .putMat4f(inverseViewProjection)
                    .putVec4(
                            ColorUtil.red(primary) / 255.0F,
                            ColorUtil.green(primary) / 255.0F,
                            ColorUtil.blue(primary) / 255.0F,
                            1.0F
                    )
                    .putVec4(
                            ColorUtil.red(secondary) / 255.0F,
                            ColorUtil.green(secondary) / 255.0F,
                            ColorUtil.blue(secondary) / 255.0F,
                            1.0F
                    )
                    .putVec4(
                            PostFx.shaderTime(),
                            feature.intensity.getValue().floatValue(),
                            feature.speed.getValue().floatValue(),
                            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F
                    )
                    .putVec4(1.0F / width, 1.0F / height, 0.0F, 0.0F)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
                    this.skyUniforms.slice(),
                    data
            );
        }
    }

    private void ensureSkyClouds(int width, int height) {
        int halfWidth = Math.max(1, width / 2);
        int halfHeight = Math.max(1, height / 2);
        if (this.skyClouds != null
                && this.skyClouds.width == halfWidth
                && this.skyClouds.height == halfHeight) {
            return;
        }
        if (this.skyClouds != null) {
            this.skyClouds.destroyBuffers();
        }
        this.skyClouds = new TextureTarget(
                "alexdlc-shadersky-clouds",
                halfWidth,
                halfHeight,
                false,
                PostPipelines.SKY_CLOUDS_FORMAT
        );
    }

    private static void drawFullscreen(RenderPass pass) {
        pass.setVertexBuffer(0, FullscreenQuad.buffer().slice());
        pass.draw(FullscreenQuad.vertexCount(), 1, 0, 0);
    }

    private static boolean valid(RenderTarget target) {
        return target != null
                && target.width > 0
                && target.height > 0
                && target.getColorTexture() != null
                && target.getColorTextureView() != null
                && target.getDepthTexture() != null
                && target.getDepthTextureView() != null;
    }

    private static GpuBuffer uniformBuffer(String label, int size) {
        return RenderSystem.getDevice().createBuffer(
                () -> label,
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                size
        );
    }

    public void release() {
        if (this.skyClouds != null) {
            this.skyClouds.destroyBuffers();
            this.skyClouds = null;
        }
        this.skyUniforms.close();
    }
}
