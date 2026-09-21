package org.alexdlc.utils.render.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.feature.impl.visual.ParticleRainFeature;
import org.alexdlc.utils.render.jump.JumpCircleRenderer;
import org.alexdlc.utils.render.particles.ProceduralParticleRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class ParticleRainRenderer {
    private static final class Drop {
        double x;
        double y;
        double z;
        float size;
        float seed;
    }

    private static final class Ripple {
        Vec3 position;
        long startNanos;
        float radius;
    }

    private final ProceduralParticleRenderer particles = new ProceduralParticleRenderer();
    private final JumpCircleRenderer circles = new JumpCircleRenderer();
    private final Random random = new Random();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private long lastFrameNanos;
    private double spawnCarry;

    public void render(ParticleRainFeature feature) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            reset();
            return;
        }

        long now = System.nanoTime();
        float dt = this.lastFrameNanos == 0L
                ? 0.0F
                : Math.min(0.05F, (now - this.lastFrameNanos) / 1.0E9F);
        this.lastFrameNanos = now;

        double radius = feature.radius.getValue();
        double height = feature.height.getValue();
        double fallSpeed = feature.fallSpeed.getValue();
        int maxDrops = feature.maxDrops.getValue().intValue();
        double dropSize = feature.size.getValue();
        Vec3 playerPos = minecraft.player.position();
        double cullDistance = radius * 2.0 + 8.0;

        // Spawn new drops on a disc around the player, up in the sky.
        this.spawnCarry += dt * feature.intensity.getValue().doubleValue();
        while (this.spawnCarry >= 1.0D && this.drops.size() < maxDrops) {
            this.spawnCarry -= 1.0D;
            double angle = this.random.nextDouble() * 6.283185307179586D;
            double distance = Math.sqrt(this.random.nextDouble()) * radius;
            double x = playerPos.x() + Math.cos(angle) * distance;
            double z = playerPos.z() + Math.sin(angle) * distance;
            Drop drop = new Drop();
            drop.x = x;
            drop.z = z;
            drop.y = playerPos.y() + height + this.random.nextDouble() * 4.0D;
            drop.size = (float) (dropSize * (0.75D + this.random.nextDouble() * 0.5D));
            drop.seed = this.random.nextFloat();
            this.drops.add(drop);
        }
        if (this.spawnCarry > 1.0D) {
            this.spawnCarry = 1.0D;
        }

        // Integrate drops, ground them with a short collider ray and drop ripples.
        List<ProceduralParticleRenderer.Sprite> sprites = new ArrayList<>(this.drops.size());
        int color = feature.resolvedColor();
        Iterator<Drop> iterator = this.drops.iterator();
        while (iterator.hasNext()) {
            Drop drop = iterator.next();
            drop.y -= fallSpeed * dt;
            double distanceToPlayer = Math.sqrt(
                    (drop.x - playerPos.x()) * (drop.x - playerPos.x())
                            + (drop.z - playerPos.z()) * (drop.z - playerPos.z())
            );
            if (distanceToPlayer > cullDistance || drop.y < playerPos.y() - 80.0D) {
                iterator.remove();
                continue;
            }
            Vec3 from = new Vec3(drop.x, drop.y + 0.2D, drop.z);
            Vec3 to = new Vec3(drop.x, drop.y - 6.0D, drop.z);
            BlockHitResult hit = level.clip(new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    minecraft.player
            ));
            if (hit.getType() == BlockHitResult.Type.BLOCK) {
                if (feature.ripples.getValue()) {
                    Ripple ripple = new Ripple();
                    ripple.position = hit.getPosition();
                    ripple.startNanos = now;
                    ripple.radius = feature.rippleRadius.getValue().floatValue();
                    this.ripples.add(ripple);
                }
                iterator.remove();
                continue;
            }
            if (drop.y < -64.0D) {
                iterator.remove();
                continue;
            }
            sprites.add(new ProceduralParticleRenderer.Sprite(
                    new Vec3(drop.x, drop.y, drop.z),
                    drop.size,
                    color,
                    ProceduralParticleRenderer.Shape.GLOW,
                    0.0F,
                    0.6F,
                    drop.seed,
                    drop.seed * 12.9898F
            ));
        }

        // Age the ripples and draw them like jump circles.
        float time = System.currentTimeMillis() / 1000.0F;
        float rippleTime = feature.rippleTime.getValue().floatValue();
        Iterator<Ripple> rippleIterator = this.ripples.iterator();
        while (rippleIterator.hasNext()) {
            Ripple ripple = rippleIterator.next();
            float progress = (now - ripple.startNanos) / 1.0E9F / Math.max(0.05F, rippleTime);
            if (progress >= 1.0F) {
                rippleIterator.remove();
                continue;
            }
            float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
            this.circles.render(
                    ripple.position,
                    ripple.radius * (0.25F + 0.75F * eased),
                    (1.0F - progress) * 0.9F,
                    time + (float) (ripple.position.x() * 0.13D),
                    color,
                    true
            );
        }

        if (!sprites.isEmpty()) {
            this.particles.render(sprites, feature.glow.getValue() ? 1.0F : 0.0F, false);
        }
    }

    private void reset() {
        this.lastFrameNanos = 0L;
        this.spawnCarry = 0.0D;
        this.drops.clear();
        this.ripples.clear();
    }

    public void release() {
        reset();
        this.particles.release();
    }
}
