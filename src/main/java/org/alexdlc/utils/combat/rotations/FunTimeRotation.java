package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.util.concurrent.ThreadLocalRandom;

public final class FunTimeRotation implements AuraRotation {
    private static final int PHASE_DIRECT = 0;
    private static final int PHASE_OVERSHOOT = 1;
    private static final int PHASE_RETURN = 2;
    private static final int PRE_ATTACK_TICKS = 2;
    private static final int POST_ATTACK_TICKS = 2;

    private static final double OVERSHOOT_MIN_DISTANCE = 25.0D;
    private static final double FINISH_DISTANCE = 0.65D;
    private static final double STOP_DISTANCE = 0.08D;

    private int targetId = Integer.MIN_VALUE;
    private int phase = PHASE_DIRECT;
    private int preAttackTicks;
    private int postAttackTicks;

    private double velocityYaw;
    private double velocityPitch;
    private double windYaw;
    private double windPitch;

    private double speed;
    private double gravity;
    private double wind;
    private double threshold;

    private double overshootYaw;
    private double overshootPitch;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            reset();
            RotationContext.clear();
            return;
        }

        if (this.targetId != target.getId()) {
            resetMotion(target.getId());
        }

        double currentYaw = Mth.wrapDegrees(RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot());
        double currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
        double realYaw = Mth.wrapDegrees(targetYaw(player, targetEyePos));
        double realPitch = targetPitch(player, targetEyePos);
        double deltaRealYaw = Mth.wrapDegrees(realYaw - currentYaw);
        double realDistance = distance(0.0D, 0.0D, deltaRealYaw, realPitch - currentPitch);

        if (attackLikely) {
            this.preAttackTicks = PRE_ATTACK_TICKS;
        }

        if (this.preAttackTicks > 0 || this.postAttackTicks > 0) {
            this.phase = PHASE_DIRECT;
            this.velocityYaw = 0.0D;
            this.velocityPitch = 0.0D;
            this.windYaw = 0.0D;
            this.windPitch = 0.0D;
            tuneCombat();
            RotationContext.setRotation((float) realYaw, (float) realPitch);

            if (this.preAttackTicks > 0) {
                this.preAttackTicks--;
            }
            if (this.postAttackTicks > 0) {
                this.postAttackTicks--;
            }
            return;
        }

        if (this.phase == PHASE_RETURN) {
            tuneCorrection();
        } else {
            tuneIdle();
            maybeStartOvershoot(currentYaw, currentPitch, realYaw, realPitch, realDistance);
        }

        double deltaYaw = this.phase == PHASE_OVERSHOOT
                ? Mth.wrapDegrees(this.overshootYaw - currentYaw)
                : Mth.wrapDegrees(realYaw - currentYaw);
        double deltaPitch = (this.phase == PHASE_OVERSHOOT ? this.overshootPitch : realPitch) - currentPitch;
        double targetDistance = distance(0.0D, 0.0D, deltaYaw, deltaPitch);

        if (targetDistance < FINISH_DISTANCE && this.phase == PHASE_OVERSHOOT) {
            this.phase = PHASE_RETURN;
            this.velocityYaw *= 0.45D;
            this.velocityPitch *= 0.45D;
            tuneCorrection();
            deltaYaw = Mth.wrapDegrees(realYaw - currentYaw);
            deltaPitch = realPitch - currentPitch;
            targetDistance = distance(0.0D, 0.0D, deltaYaw, deltaPitch);
        } else if (targetDistance < FINISH_DISTANCE && this.phase == PHASE_RETURN) {
            this.phase = PHASE_DIRECT;
            this.velocityYaw *= 0.25D;
            this.velocityPitch *= 0.25D;
        }

        if (targetDistance < STOP_DISTANCE) {
            this.velocityYaw *= 0.35D;
            this.velocityPitch *= 0.35D;
            RotationContext.setRotation((float) realYaw, (float) realPitch);
            return;
        }

        StepParams params = scaledParams(targetDistance);
        applyWind(params.wind());

        double gravityYaw = params.gravity() * deltaYaw / targetDistance;
        double gravityPitch = params.gravity() * deltaPitch / targetDistance;

        this.velocityYaw += this.windYaw + gravityYaw;
        this.velocityPitch += this.windPitch + gravityPitch;
        applyMicroCorrections(attackLikely, targetDistance);
        brakeWhenPassingTarget(deltaYaw, deltaPitch, targetDistance);
        clampVelocity(params.speed());

        double nextYaw = Mth.wrapDegrees(currentYaw + this.velocityYaw);
        double nextPitch = clamp(currentPitch + this.velocityPitch, -90.0D, 90.0D);
        RotationContext.setRotation((float) nextYaw, (float) nextPitch);
    }

    @Override
    public void onAttack() {
        this.preAttackTicks = 0;
        this.postAttackTicks = POST_ATTACK_TICKS;
        this.phase = PHASE_DIRECT;
        this.velocityYaw *= 0.35D;
        this.velocityPitch *= 0.35D;
        this.windYaw *= 0.2D;
        this.windPitch *= 0.2D;
    }

    @Override
    public void reset() {
        this.targetId = Integer.MIN_VALUE;
        this.phase = PHASE_DIRECT;
        this.preAttackTicks = 0;
        this.postAttackTicks = 0;
        this.velocityYaw = 0.0D;
        this.velocityPitch = 0.0D;
        this.windYaw = 0.0D;
        this.windPitch = 0.0D;
        this.speed = 0.0D;
        this.gravity = 0.0D;
        this.wind = 0.0D;
        this.threshold = 0.0D;
        this.overshootYaw = 0.0D;
        this.overshootPitch = 0.0D;
    }

    private void resetMotion(int nextTargetId) {
        this.targetId = nextTargetId;
        this.phase = PHASE_DIRECT;
        this.preAttackTicks = 0;
        this.postAttackTicks = 0;
        this.velocityYaw = 0.0D;
        this.velocityPitch = 0.0D;
        this.windYaw = 0.0D;
        this.windPitch = 0.0D;
        this.speed = 0.0D;
        this.gravity = 0.0D;
        this.wind = 0.0D;
        this.threshold = 0.0D;
    }

    private void maybeStartOvershoot(double currentYaw, double currentPitch, double realYaw, double realPitch, double realDistance) {
        if (this.phase != PHASE_DIRECT || realDistance < OVERSHOOT_MIN_DISTANCE || random().nextDouble() >= 0.75D) {
            return;
        }

        double deltaYaw = Mth.wrapDegrees(realYaw - currentYaw);
        double angle = Math.atan2(realPitch - currentPitch, deltaYaw);
        double amount = randomRange(4.0D, Math.min(16.0D, realDistance * 0.35D));
        this.overshootYaw = Mth.wrapDegrees(realYaw + Math.cos(angle) * amount);
        this.overshootPitch = clamp(realPitch + Math.sin(angle) * amount, -89.0D, 89.0D);
        this.phase = PHASE_OVERSHOOT;
    }

    private void tuneCombat() {
        tune(randomRange(18.0D, 26.0D), randomRange(9.5D, 14.0D), randomRange(0.35D, 1.25D), 3.0D, 0.32D);
    }

    private void tuneIdle() {
        tune(randomRange(8.0D, 14.0D), randomRange(4.5D, 8.0D), randomRange(2.5D, 5.5D), 8.0D, 0.2D);
    }

    private void tuneCorrection() {
        tune(randomRange(4.0D, 8.0D), randomRange(7.0D, 10.0D), randomRange(0.35D, 1.2D), 2.5D, 0.28D);
    }

    private void tune(double nextSpeed, double nextGravity, double nextWind, double nextThreshold, double factor) {
        if (this.speed <= 0.0D) {
            this.speed = nextSpeed;
            this.gravity = nextGravity;
            this.wind = nextWind;
            this.threshold = nextThreshold;
            return;
        }

        this.speed = lerp(this.speed, nextSpeed, factor);
        this.gravity = lerp(this.gravity, nextGravity, factor);
        this.wind = lerp(this.wind, nextWind, factor);
        this.threshold = lerp(this.threshold, nextThreshold, factor);
    }

    private StepParams scaledParams(double targetDistance) {
        if (targetDistance >= this.threshold) {
            return new StepParams(this.speed, this.gravity, this.wind);
        }

        double progress = clamp(targetDistance / Math.max(this.threshold, 0.1D), 0.0D, 1.0D);
        double localSpeed = Math.max(0.65D, this.speed * (0.35D + progress * 0.65D));
        double localGravity = Math.max(1.2D, this.gravity * (0.55D + progress * 0.45D));
        double localWind = Math.max(0.04D, this.wind * (0.2D + progress * 0.8D));
        return new StepParams(localSpeed, localGravity, localWind);
    }

    private void applyWind(double localWind) {
        this.windYaw = this.windYaw * 0.86D + randomRange(-localWind, localWind) * 0.14D;
        this.windPitch = this.windPitch * 0.86D + randomRange(-localWind, localWind) * 0.14D;
    }

    private void applyMicroCorrections(boolean attackLikely, double targetDistance) {
        if (targetDistance > this.threshold) {
            return;
        }

        double scale = attackLikely ? 0.055D : 0.12D;
        this.velocityYaw += randomRange(-scale, scale);
        this.velocityPitch += randomRange(-scale, scale);
    }

    private void brakeWhenPassingTarget(double deltaYaw, double deltaPitch, double targetDistance) {
        if (targetDistance > this.threshold) {
            return;
        }

        double dot = this.velocityYaw * deltaYaw + this.velocityPitch * deltaPitch;
        if (dot < 0.0D) {
            this.velocityYaw *= 0.58D;
            this.velocityPitch *= 0.58D;
        }
    }

    private void clampVelocity(double maxSpeed) {
        double magnitude = distance(0.0D, 0.0D, this.velocityYaw, this.velocityPitch);
        if (magnitude <= maxSpeed || magnitude <= 0.0D) {
            return;
        }

        this.velocityYaw = this.velocityYaw / magnitude * maxSpeed;
        this.velocityPitch = this.velocityPitch / magnitude * maxSpeed;
    }

    private static float targetYaw(LocalPlayer player, Vec3 targetEyePos) {
        Vec3 delta = targetEyePos.subtract(player.getEyePosition());
        return (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
    }

    private static float targetPitch(LocalPlayer player, Vec3 targetEyePos) {
        Vec3 delta = targetEyePos.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        return (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
    }

    private static double distance(double fromYaw, double fromPitch, double toYaw, double toPitch) {
        double yaw = toYaw - fromYaw;
        double pitch = toPitch - fromPitch;
        return Math.sqrt(yaw * yaw + pitch * pitch);
    }

    private static double lerp(double current, double target, double factor) {
        return current + (target - current) * factor;
    }

    private static double randomRange(double min, double max) {
        return random().nextDouble(min, max);
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record StepParams(double speed, double gravity, double wind) {
    }
}
