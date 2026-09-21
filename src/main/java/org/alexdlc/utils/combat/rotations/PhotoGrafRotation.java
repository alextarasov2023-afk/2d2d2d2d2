package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public final class PhotoGrafRotation implements AuraRotation {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private float smoothedYaw;
    private float smoothedPitch;
    private boolean initialized;
    private int counter;
    private long startTime;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            reset();
            RotationContext.clear();
            return;
        }

        if (!initialized) {
            this.smoothedYaw = RotationContext.isActive()
                    ? RotationContext.getServerYaw()
                    : player.getYRot();
            this.smoothedPitch = RotationContext.isActive()
                    ? RotationContext.getServerPitch()
                    : player.getXRot();
            this.startTime = System.currentTimeMillis();
            this.initialized = true;
        }

        if (player.distanceTo(target) < 0.75F) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 delta = targetEyePos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float idealYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float idealPitch = Mth.clamp(
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)),
                -90.0F,
                90.0F
        );

        long elapsed = System.currentTimeMillis() - startTime;
        float phaseAngle = elapsed / 40.0F + (counter % 6);
        int pattern = counter % 3;

        float patternYaw;
        float patternPitch;
        switch (pattern) {
            case 0 -> {
                patternYaw = (float) Math.cos(phaseAngle);
                patternPitch = (float) Math.sin(phaseAngle);
            }
            case 1 -> {
                patternYaw = (float) Math.sin(phaseAngle);
                patternPitch = (float) Math.cos(phaseAngle);
            }
            default -> {
                patternYaw = (float) Math.sin(phaseAngle);
                patternPitch = (float) -Math.cos(phaseAngle);
            }
        }

        float yawJitter = randomLerp(3.0F, 5.0F) * patternYaw;
        float pitchCosModulator = randomLerp(0.0F, 2.0F)
                * (float) Math.cos(System.currentTimeMillis() / 5000.0D);
        float pitchJitter = randomLerp(2.0F, 4.0F) * patternPitch + pitchCosModulator;

        float cooldownFraction = 1.0F - Mth.clamp(
                player.getAttackStrengthScale(0.5F),
                0.0F,
                1.0F
        );
        float cooldownAddition = cooldownFraction * randomLerp(20.0F, 40.0F);
        float cooldownSign = (counter % 2 == 0) ? -1.0F : 1.0F;

        boolean critWindow = attackLikely && !player.onGround() && player.fallDistance > 0.0F;
        if (critWindow) {
            yawJitter = 0.0F;
            pitchJitter = 0.0F;
            cooldownAddition = 0.0F;
        }

        float noiseYaw = (critWindow ? 0.0F : 19.23253F) * cooldownSign
                + cooldownAddition * cooldownSign
                + yawJitter;
        float noisePitch = -cooldownAddition + pitchJitter;

        float targetYaw = idealYaw + noiseYaw;
        float targetPitch = Mth.clamp(idealPitch + noisePitch, -90.0F, 90.0F);

        float deltaYaw = Mth.wrapDegrees(targetYaw - this.smoothedYaw);
        float deltaPitch = targetPitch - this.smoothedPitch;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float yawSpeed = (float) (9.0D - rng.nextDouble(0.0D, 2.5D));
        float pitchSpeed = (float) (4.0D - rng.nextDouble(0.0D, 2.5D));

        float stepYaw = Mth.clamp(deltaYaw, -yawSpeed, yawSpeed);
        float stepPitch = Mth.clamp(deltaPitch, -pitchSpeed, pitchSpeed);

        this.smoothedYaw = Mth.wrapDegrees(this.smoothedYaw + stepYaw);
        this.smoothedPitch = Mth.clamp(this.smoothedPitch + stepPitch, -90.0F, 90.0F);

        RotationContext.setRotation(this.smoothedYaw, this.smoothedPitch);
    }

    @Override
    public void onAttack() {
        this.counter++;
    }

    @Override
    public void reset() {
        this.initialized = false;
        this.counter = 0;
        this.startTime = 0L;
    }

    private static float randomLerp(float min, float max) {
        return Mth.lerp(SECURE_RANDOM.nextFloat(), min, max);
    }
}
