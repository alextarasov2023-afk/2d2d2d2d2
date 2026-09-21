package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.security.SecureRandom;

public final class SpookyTimeRotation implements AuraRotation {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final MouseProfile profile = MouseProfile.DEFAULT;

    private LivingEntity lastTarget = null;
    private float startYaw = 0.0F;
    private float startPitch = 0.0F;
    private float targetYaw = 0.0F;
    private float targetPitch = 0.0F;
    private long startTimeMs = 0L;
    private long durationMs = 1L;

    private float currentJitterYaw = 0.0F;
    private float currentJitterPitch = 0.0F;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        float fromYaw = player.yHeadRot;
        float fromPitch = player.getXRot();

        AABB box = target.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double bodyY = box.minY + (box.maxY - box.minY) * 0.4D;

        Rotation rawGoal = toPoint(player, new Vec3(centerX, bodyY, centerZ));

        long now = System.currentTimeMillis();
        double deltaYaw = Mth.wrapDegrees(rawGoal.yaw() - fromYaw);
        double deltaPitch = rawGoal.pitch() - fromPitch;
        double totalDistance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        // Reset trajectory if target changed or previous movement finished
        if (this.lastTarget != target || (now - this.startTimeMs) >= this.durationMs || totalDistance > 45.0D) {
            this.lastTarget = target;
            this.startYaw = fromYaw;
            this.startPitch = fromPitch;
            this.startPitch = fromPitch;

            // Estimate angular width of target box
            double distanceToTarget = player.distanceTo(target);
            double angularWidth = Math.toDegrees(Math.atan2(box.getXsize(), Math.max(distanceToTarget, 0.1D)));

            // Calculate duration using Fitts's Law
            long fittsTime = KinematicsUtil.calculateFittsLawTime(totalDistance, angularWidth, profile.getFittsA(), profile.getFittsB());
            this.durationMs = Mth.clamp(fittsTime, attackLikely ? 40L : 70L, 400L);
            this.startTimeMs = now;

            // Optional human overshoot for large distance shifts
            float overshootYaw = 0.0F;
            float overshootPitch = 0.0F;
            if (totalDistance > 35.0D && SECURE_RANDOM.nextDouble() < profile.getOvershootProbability()) {
                overshootYaw = (float) ((SECURE_RANDOM.nextDouble() - 0.5D) * 4.0D);
                overshootPitch = (float) ((SECURE_RANDOM.nextDouble() - 0.5D) * 2.0D);
            }

            this.targetYaw = rawGoal.yaw() + overshootYaw;
            this.targetPitch = Mth.clamp(rawGoal.pitch() + overshootPitch, -90.0F, 90.0F);
        }

        double progress = (double) (now - this.startTimeMs) / (double) this.durationMs;
        progress = Mth.clamp(progress, 0.0D, 1.0D);

        // Apply Minimum Jerk interpolation (bell-shaped velocity profile)
        double currentYawUnwrapped = KinematicsUtil.getMinimumJerk(this.startYaw, this.startYaw + Mth.wrapDegrees(this.targetYaw - this.startYaw), progress);
        double currentPitchUnwrapped = KinematicsUtil.getMinimumJerk(this.startPitch, this.targetPitch, progress);

        // Dynamic human tremor/jitter dependent on movement phase
        float targetJitterYaw = legitRandom(-1.2F, 1.2F) * (float) (1.0D - Math.abs(progress - 0.5D));
        float targetJitterPitch = legitRandom(-0.8F, 0.8F) * (float) (1.0D - Math.abs(progress - 0.5D));
        this.currentJitterYaw = Mth.lerp(0.2F, this.currentJitterYaw, targetJitterYaw);
        this.currentJitterPitch = Mth.lerp(0.2F, this.currentJitterPitch, targetJitterPitch);

        float yaw = Mth.wrapDegrees((float) currentYawUnwrapped + this.currentJitterYaw);
        float pitch = Mth.clamp((float) currentPitchUnwrapped + this.currentJitterPitch, -90.0F, 90.0F);

        Rotation corrected = correctRotation(fromYaw, fromPitch, yaw, pitch);
        RotationContext.setRotation(corrected.yaw(), corrected.pitch());
    }

    private static Rotation toPoint(LocalPlayer player, Vec3 point) {
        Vec3 eye = player.getEyePosition();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)), -90.0F, 90.0F);
        return new Rotation(yaw, pitch);
    }

    private static float legitRandom(float min, float max) {
        return min + (max - min) * SECURE_RANDOM.nextFloat();
    }

    private static Rotation correctRotation(float fromYaw, float fromPitch, float toYaw, float toPitch) {
        float step = gcdStep();
        float deltaYaw = Mth.wrapDegrees(toYaw - fromYaw);
        float deltaPitch = toPitch - fromPitch;
        deltaYaw = Math.round(deltaYaw / step) * step;
        deltaPitch = Math.round(deltaPitch / step) * step;
        return new Rotation(
                fromYaw + deltaYaw,
                Mth.clamp(fromPitch + deltaPitch, -90.0F, 90.0F)
        );
    }

    private static float gcdStep() {
        double sensitivity = Minecraft.getInstance().options.sensitivity().get();
        double factor = sensitivity * 0.6D + 0.2D;
        return (float) (factor * factor * factor * 1.2D);
    }

    private record Rotation(float yaw, float pitch) {
    }
}
