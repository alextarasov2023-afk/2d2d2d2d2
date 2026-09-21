package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.util.concurrent.ThreadLocalRandom;

public final class HolyWorldRotation implements AuraRotation {
    private int holdTicks;
    private float lastYaw;
    private float lastPitch;
    private boolean initialized;
    private float pitchPhase;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            reset();
            RotationContext.clear();
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (!initialized) {
            this.lastYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
            this.lastPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
            this.initialized = true;
        }

        if (this.holdTicks > 0 && !attackLikely) {
            this.holdTicks--;
            this.pitchPhase += 0.2F;
            float pitchSway = (float) Math.sin(this.pitchPhase) * 0.4F;
            RotationContext.setRotation(this.lastYaw, Mth.clamp(this.lastPitch + pitchSway, -90.0F, 90.0F));
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 delta = targetEyePos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float idealYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float idealPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -90.0F, 90.0F);

        float deltaYaw = Mth.wrapDegrees(idealYaw - this.lastYaw);
        float deltaPitch = idealPitch - this.lastPitch;

        float stepRatio = attackLikely ? 0.95F : (float) random.nextDouble(0.55D, 0.80D);
        float yawStep = deltaYaw * stepRatio;
        float pitchStep = deltaPitch * stepRatio;

        this.lastYaw = Mth.wrapDegrees(this.lastYaw + yawStep);
        this.lastPitch = Mth.clamp(this.lastPitch + pitchStep, -90.0F, 90.0F);

        if (Math.abs(deltaYaw) < 15.0F && random.nextDouble() < 0.35D) {
            this.holdTicks = random.nextInt(1, 3);
        }

        RotationContext.setRotation(this.lastYaw, this.lastPitch);
    }

    @Override
    public void reset() {
        this.holdTicks = 0;
        this.initialized = false;
        this.pitchPhase = 0.0F;
    }
}
