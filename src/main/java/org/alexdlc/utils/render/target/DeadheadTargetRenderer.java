package org.alexdlc.utils.render.target;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.math.Animation;
import org.alexdlc.utils.render.HurtUtil;
import org.alexdlc.utils.render.Render3DUtil;
import org.alexdlc.utils.render.Textures;
import org.alexdlc.utils.render.particles.WorldParticleRenderer;
import org.alexdlc.utils.render.particles.WorldParticleRenderer.Sprite;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

public final class DeadheadTargetRenderer {
    private static final int SKULL_COUNT = 3;
    private static final int SEAL_PARTICLES = 16;
    private static final Random RNG = new Random();

    private static long lastBiteTime = 0L;
    private static int biteSkullIndex = 0;

    public static void triggerBite() {
        lastBiteTime = System.currentTimeMillis();
        biteSkullIndex = RNG.nextInt(SKULL_COUNT);
    }

    private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();

    private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
            .withSampler("BloomSampler")
            .withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final RenderPipeline ALPHA_THROUGH_WALLS = RenderPipeline.builder()
            .withLocation(Identifier.parse("alexdlc:pipeline/world/deadhead_alpha"))
            .withVertexShader(Identifier.parse("alexdlc:core/world_particle"))
            .withFragmentShader(Identifier.parse("alexdlc:core/world_particle"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private final Animation appear = new Animation(650L, Animation.Easing.EASE_OUT_EXPO);
    private final Animation inner  = new Animation(850L, Animation.Easing.EASE_IN_OUT_QUAD);

    private final WorldParticleRenderer glowRenderer = new WorldParticleRenderer();
    private final TargetDeathDissolve deathDissolve = new TargetDeathDissolve();
    private final List<Sprite> glowSprites = new ArrayList<>(120);

    private LivingEntity lastTarget;
    private int dissolvedTargetId = Integer.MIN_VALUE;
    private boolean animationTarget;
    private long lastFrameNanos;
    private float clock;

    public void render(LivingEntity activeTarget, float tickDelta, int color) {
        long nowNanos  = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        float dt = lastFrameNanos == 0L
                ? 0.016F
                : Math.min(0.1F, (nowNanos - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = nowNanos;

        boolean present = valid(activeTarget);
        if (present) {
            lastTarget = activeTarget;
            if (activeTarget.getId() == dissolvedTargetId) dissolvedTargetId = Integer.MIN_VALUE;
        }
        if (dead(lastTarget) && lastTarget.getId() != dissolvedTargetId) {
            deathDissolve.burst(glowSprites, lastTarget, color, nowMillis);
            dissolvedTargetId = lastTarget.getId();
            present = false;
        }
        updateAnimations(present);

        float aV = appear.getValue();
        float iV = inner.getValue();
        if (lastTarget == null || aV <= 0.01F || iV <= 0.001F) {
            if (!present && aV <= 0.01F) { lastTarget = null; glowSprites.clear(); }
            deathDissolve.render(dt, nowMillis);
            return;
        }

        clock += dt * 1000.0F;
        float t = clock / 1000.0F;

        Vec3 base = Render3DUtil.interpolatedPosition(lastTarget, tickDelta);
        float alpha = Math.min(0.95F, iV * 1.6F) * aV;

        int baseColor = HurtUtil.blend(color, lastTarget, 1.0F);
        int mintColor = ColorUtil.lerp(baseColor, 0xFF00FF88, 0.40F);

        glowSprites.clear();

        float sealR = (0.85F + 0.12F * Mth.sin(t * 3.0F)) * iV;
        for (int i = 0; i < SEAL_PARTICLES; i++) {
            float angle = t * 1.8F + (float)(i * Math.PI * 2.0 / SEAL_PARTICLES);
            glowSprites.add(new Sprite(
                    new Vec3(base.x + sealR * Mth.sin(angle),
                             base.y + 0.05 + 0.03 * Mth.sin(t * 4F + i),
                             base.z + sealR * Mth.cos(angle)),
                    0.08F * iV,
                    ColorUtil.multiplyAlpha(mintColor, alpha * 0.45F)));
        }

        long biteAge = nowMillis - lastBiteTime;
        float biteT  = (biteAge >= 0 && biteAge <= 380) ? biteAge / 380.0F : -1.0F;
        Vec3 chestPos = base.add(0, lastTarget.getBbHeight() * 0.55, 0);

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);

        List<Quad3D> meshQuads = new ArrayList<>(180);

        float orbitR = 1.85F * (0.8F + 0.2F * iV);

        for (int si = 0; si < SKULL_COUNT; si++) {
            float phase = (float)(si * Math.PI * 2.0 / SKULL_COUNT);
            float orbitA = t * 2.2F + phase;
            double bob = 0.40 * Mth.sin(t * 3.6F + phase * 1.4F);

            Vec3 skullCenter = new Vec3(
                    base.x + orbitR * Mth.sin(orbitA),
                    base.y + lastTarget.getBbHeight() * 0.52 + bob,
                    base.z + orbitR * Mth.cos(orbitA)
            );

            float scale = 1.56F * iV;
            float jawOpen = 0.0F;
            float yawAngle = -orbitA + (float) Math.PI / 2.0F; 

            if (biteT >= 0.0F && si == biteSkullIndex) {
                float lunge = (float) Math.sin(biteT * Math.PI);
                skullCenter = skullCenter.lerp(chestPos, lunge * 0.88);
                scale *= 1.0F + lunge * 0.6F;
                jawOpen = 0.18F * lunge; 

                if (biteT >= 0.35F && biteT <= 0.65F) {
                    for (int b = 0; b < 4; b++) {
                        glowSprites.add(new Sprite(
                                chestPos.add((RNG.nextFloat() - 0.5) * 0.4,
                                             (RNG.nextFloat() - 0.5) * 0.4,
                                             (RNG.nextFloat() - 0.5) * 0.4),
                                0.12F * iV,
                                ColorUtil.multiplyAlpha(0xFF00FF88, alpha)));
                    }
                }
            }

            for (int ti = 1; ti <= 8; ti++) {
                float tA = orbitA - ti * 0.08F;
                double ttBob = 0.40 * Mth.sin(t * 3.6F + phase * 1.4F - ti * 0.1F);
                float tAlpha = alpha * (1.0F - ti / 8.0F) * 0.35F;
                if (tAlpha > 0.005F) {
                    glowSprites.add(new Sprite(
                            new Vec3(base.x + orbitR * Mth.sin(tA),
                                     base.y + lastTarget.getBbHeight() * 0.52 + ttBob,
                                     base.z + orbitR * Mth.cos(tA)),
                            scale * (1.0F - ti * 0.04F),
                            ColorUtil.multiplyAlpha(mintColor, tAlpha)));
                }
            }

            int boneColor  = packARGB((int)(alpha * 230), 215, 255, 225); 
            int darkSocket = packARGB((int)(alpha * 240), 10, 20, 15);   
            int toothCol   = packARGB((int)(alpha * 240), 245, 255, 250); 

            add3DBox(meshQuads, skullCenter, scale * 0.32f, scale * 0.28f, scale * 0.32f, 0, scale * 0.10f, 0, yawAngle, boneColor);

            add3DBox(meshQuads, skullCenter, scale * 0.24f, scale * 0.14f, scale * 0.18f, 0, -scale * 0.08f, scale * 0.12f, yawAngle, boneColor);

            add3DBox(meshQuads, skullCenter, scale * 0.08f, scale * 0.08f, scale * 0.05f, -scale * 0.08f, scale * 0.10f, scale * 0.14f, yawAngle, darkSocket);

            add3DBox(meshQuads, skullCenter, scale * 0.08f, scale * 0.08f, scale * 0.05f, scale * 0.08f, scale * 0.10f, scale * 0.14f, yawAngle, darkSocket);

            add3DBox(meshQuads, skullCenter, scale * 0.05f, scale * 0.06f, scale * 0.04f, 0, scale * 0.01f, scale * 0.15f, yawAngle, darkSocket);

            add3DBox(meshQuads, skullCenter, scale * 0.22f, scale * 0.10f, scale * 0.24f, 0, -scale * (0.20f + jawOpen), scale * 0.06f, yawAngle, boneColor);

            add3DBox(meshQuads, skullCenter, scale * 0.18f, scale * 0.04f, scale * 0.02f, 0, -scale * 0.14f, scale * 0.20f, yawAngle, toothCol);

            add3DBox(meshQuads, skullCenter, scale * 0.16f, scale * 0.04f, scale * 0.02f, 0, -scale * (0.16f + jawOpen), scale * 0.17f, yawAngle, toothCol);

            Vec3 leftEyeWorld  = rotateOffset(skullCenter, -scale * 0.08f, scale * 0.10f, scale * 0.16f, yawAngle);
            Vec3 rightEyeWorld = rotateOffset(skullCenter, scale * 0.08f, scale * 0.10f, scale * 0.16f, yawAngle);
            glowSprites.add(new Sprite(leftEyeWorld, scale * 0.18f, ColorUtil.multiplyAlpha(0xFF00FFCC, alpha * 0.9F)));
            glowSprites.add(new Sprite(rightEyeWorld, scale * 0.18f, ColorUtil.multiplyAlpha(0xFF00FFCC, alpha * 0.9F)));
        }

        render3DMesh(mc, meshQuads, cameraPos, viewPose);

        glowRenderer.render(glowSprites, 1.0F, true, true);
        deathDissolve.render(dt, nowMillis);
    }

    private record Quad3D(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {}

    private static Vec3 rotateOffset(Vec3 center, float ox, float oy, float oz, float yaw) {
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        double rx = ox * cos - oz * sin;
        double rz = ox * sin + oz * cos;
        return center.add(rx, oy, rz);
    }

    private static void add3DBox(List<Quad3D> quads, Vec3 center, float w, float h, float d, float ox, float oy, float oz, float yaw, int color) {
        float hW = w * 0.5f;
        float hH = h * 0.5f;
        float hD = d * 0.5f;

        Vec3[] v = new Vec3[] {
                rotateOffset(center, ox - hW, oy - hH, oz - hD, yaw), 
                rotateOffset(center, ox + hW, oy - hH, oz - hD, yaw), 
                rotateOffset(center, ox + hW, oy + hH, oz - hD, yaw), 
                rotateOffset(center, ox - hW, oy + hH, oz - hD, yaw), 
                rotateOffset(center, ox - hW, oy - hH, oz + hD, yaw), 
                rotateOffset(center, ox + hW, oy - hH, oz + hD, yaw), 
                rotateOffset(center, ox + hW, oy + hH, oz + hD, yaw), 
                rotateOffset(center, ox - hW, oy + hH, oz + hD, yaw)  
        };

        quads.add(new Quad3D(v[4], v[5], v[6], v[7], color));

        quads.add(new Quad3D(v[1], v[0], v[3], v[2], darken(color, 0.80f)));

        quads.add(new Quad3D(v[3], v[2], v[6], v[7], lighten(color, 1.15f)));

        quads.add(new Quad3D(v[4], v[5], v[1], v[0], darken(color, 0.65f)));

        quads.add(new Quad3D(v[5], v[1], v[2], v[6], darken(color, 0.88f)));

        quads.add(new Quad3D(v[0], v[4], v[7], v[3], darken(color, 0.88f)));
    }

    private void render3DMesh(Minecraft mc, List<Quad3D> quads, Vec3 cameraPos, Matrix4f viewPose) {
        if (quads.isEmpty() || mc.level == null) return;

        var target = mc.gameRenderer.mainRenderTarget();
        GpuTextureView colorView = target != null ? target.getColorTextureView() : null;
        if (colorView == null) return;

        AbstractTexture bloom = mc.getTextureManager().getTexture(Textures.Shader.BLOOM);
        GpuTextureView bloomView = bloom != null ? bloom.getTextureView() : null;
        if (bloomView == null) return;

        BufferBuilder builder = new BufferBuilder(
                ByteBufferBuilder.exactlySized(quads.size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        for (Quad3D q : quads) {
            Vector4f v0 = Render3DUtil.toViewSpace(q.p0, cameraPos, viewPose);
            Vector4f v1 = Render3DUtil.toViewSpace(q.p1, cameraPos, viewPose);
            Vector4f v2 = Render3DUtil.toViewSpace(q.p2, cameraPos, viewPose);
            Vector4f v3 = Render3DUtil.toViewSpace(q.p3, cameraPos, viewPose);

            builder.addVertex(v0.x, v0.y, v0.z).setUv(0, 0).setColor(q.color);
            builder.addVertex(v1.x, v1.y, v1.z).setUv(1, 0).setColor(q.color);
            builder.addVertex(v2.x, v2.y, v2.z).setUv(1, 1).setColor(q.color);

            builder.addVertex(v0.x, v0.y, v0.z).setUv(0, 0).setColor(q.color);
            builder.addVertex(v2.x, v2.y, v2.z).setUv(1, 1).setColor(q.color);
            builder.addVertex(v3.x, v3.y, v3.z).setUv(0, 1).setColor(q.color);
        }

        MeshData meshData = builder.build();
        if (meshData == null) return;

        var device = RenderSystem.getDevice();
        GpuBuffer vertexBuffer = null;
        GpuBuffer uniformBuffer = null;
        try {
            vertexBuffer = device.createBuffer(
                    () -> "Alex DLC 3D Deadhead Vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    meshData.vertexBuffer()
            );
            uniformBuffer = uploadUniform(1.0f);

            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            RenderPass pass = device.createCommandEncoder().createRenderPass(
                    () -> "Alex DLC 3D Deadhead Pass", colorView, Optional.empty());
            try {
                pass.setPipeline(ALPHA_THROUGH_WALLS);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setUniform("WorldParticleUniforms", uniformBuffer);
                pass.bindTexture("BloomSampler", bloomView, sampler);
                pass.setVertexBuffer(0, vertexBuffer.slice());
                pass.draw(quads.size() * 6, 1, 0, 0);
            } finally {
                pass.close();
            }
        } finally {
            if (uniformBuffer != null) uniformBuffer.close();
            if (vertexBuffer != null) vertexBuffer.close();
            meshData.close();
        }
    }

    private GpuBuffer uploadUniform(float brightness) {
        var device = RenderSystem.getDevice();
        GpuBuffer buffer = device.createBuffer(
                () -> "Alex DLC 3D Deadhead UBO",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_SIZE
        );
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = Std140Builder.onStack(stack, UNIFORM_SIZE)
                    .putVec4(brightness, 0.0F, 0.0F, 0.0F)
                    .get();
            device.createCommandEncoder().writeToBuffer(buffer.slice(), data);
        }
        return buffer;
    }

    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lighten(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int packARGB(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateAnimations(boolean present) {
        if (present == animationTarget) return;
        animationTarget = present;
        float to = present ? 1.0F : 0.0F;
        appear.animate(appear.getValue(), to, present ? 650L : 450L, present ? Animation.Easing.EASE_OUT_EXPO : Animation.Easing.EASE_OUT_QUAD);
        inner.animate(inner.getValue(), to, present ? 850L : 500L, present ? Animation.Easing.EASE_IN_OUT_QUAD : Animation.Easing.EASE_OUT_QUAD);
    }

    private static boolean valid(LivingEntity e) {
        return e != null && e.isAlive() && !e.isRemoved();
    }

    private static boolean dead(LivingEntity e) {
        return e != null && (e.isDeadOrDying() || e.deathTime > 0 || e.getHealth() <= 0.0F);
    }

    public void release() {
        lastTarget = null;
        glowSprites.clear();
    }
}
