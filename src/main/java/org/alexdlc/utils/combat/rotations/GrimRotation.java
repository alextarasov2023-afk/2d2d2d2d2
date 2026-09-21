package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.util.concurrent.ThreadLocalRandom;

public final class GrimRotation implements AuraRotation {
    private static final float MAX_YAW_SPEED = 32.0F;
    private static final float MAX_PITCH_SPEED = 20.0F;

    private float smoothedYaw;
    private float smoothedPitch;
    private boolean initialized;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            reset();
            RotationContext.clear();
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 delta = targetEyePos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float idealYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float idealPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -90.0F, 90.0F);

        float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();

        if (!initialized) {
            this.smoothedYaw = currentYaw;
            this.smoothedPitch = currentPitch;
            this.initialized = true;
        }

        float deltaYaw = Mth.wrapDegrees(idealYaw - this.smoothedYaw);
        float deltaPitch = idealPitch - this.smoothedPitch;

        double dist = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        float speedFactor = (float) Math.min(1.0D, Math.max(0.15D, cubicEase(Math.min(1.0D, dist / 45.0D))));

        float stepYaw = Mth.clamp(deltaYaw * speedFactor, -MAX_YAW_SPEED, MAX_YAW_SPEED);
        float stepPitch = Mth.clamp(deltaPitch * speedFactor, -MAX_PITCH_SPEED, MAX_PITCH_SPEED);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        stepYaw += (float) random.nextDouble(-0.15D, 0.15D);
        stepPitch += (float) random.nextDouble(-0.1D, 0.1D);

        this.smoothedYaw = Mth.wrapDegrees(this.smoothedYaw + stepYaw);
        this.smoothedPitch = Mth.clamp(this.smoothedPitch + stepPitch, -90.0F, 90.0F);

        RotationContext.setRotation(this.smoothedYaw, this.smoothedPitch);
    }

    @Override
    public void reset() {
        this.initialized = false;
    }

    private static double cubicEase(double t) {
        return t < 0.5D ? 4.0D * t * t * t : 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
    }

    private static float getGcdStep() {
        double sensitivity = Minecraft.getInstance().options.sensitivity().get();
        double factor = sensitivity * 0.6D + 0.2D;
        return (float) (factor * factor * factor * 1.2D);
    }
}
