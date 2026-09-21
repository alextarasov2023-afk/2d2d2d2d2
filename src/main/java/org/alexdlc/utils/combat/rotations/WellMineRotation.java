package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.util.concurrent.ThreadLocalRandom;

public final class WellMineRotation implements AuraRotation {
    private float currentYaw;
    private float currentPitch;
    private boolean initialized;
    private double oscillationTime;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            reset();
            RotationContext.clear();
            return;
        }

        if (!initialized) {
            this.currentYaw = player.getYRot();
            this.currentPitch = player.getXRot();
            this.initialized = true;
        }

        Vec3 targetVel = target.getDeltaMovement();
        Vec3 predictedPos = targetEyePos.add(targetVel.x * 1.5D, targetVel.y * 1.0D, targetVel.z * 1.5D);

        Vec3 eye = player.getEyePosition();
        Vec3 delta = predictedPos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float idealYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float idealPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -90.0F, 90.0F);

        this.oscillationTime += 0.35D;
        float oscYaw = (float) (Math.sin(this.oscillationTime) * 0.7D);
        float oscPitch = (float) (Math.cos(this.oscillationTime * 0.8D) * 0.4D);

        idealYaw += oscYaw;
        idealPitch = Mth.clamp(idealPitch + oscPitch, -90.0F, 90.0F);

        float deltaYaw = Mth.wrapDegrees(idealYaw - this.currentYaw);
        float deltaPitch = idealPitch - this.currentPitch;

        float turnSpeedYaw = attackLikely ? 42.0F : 28.0F;
        float turnSpeedPitch = attackLikely ? 26.0F : 18.0F;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float lerpFactor = (float) random.nextDouble(0.40D, 0.65D);

        float stepYaw = Mth.clamp(deltaYaw * lerpFactor, -turnSpeedYaw, turnSpeedYaw);
        float stepPitch = Mth.clamp(deltaPitch * lerpFactor, -turnSpeedPitch, turnSpeedPitch);

        this.currentYaw = Mth.wrapDegrees(this.currentYaw + stepYaw);
        this.currentPitch = Mth.clamp(this.currentPitch + stepPitch, -90.0F, 90.0F);

        RotationContext.setRotation(this.currentYaw, this.currentPitch);
    }

    @Override
    public void reset() {
        this.initialized = false;
        this.oscillationTime = 0.0D;
    }
}
