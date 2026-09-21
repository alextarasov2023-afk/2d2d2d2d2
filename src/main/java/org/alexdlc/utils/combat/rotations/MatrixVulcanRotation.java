package org.alexdlc.utils.combat.rotations;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.RotationContext;

import java.util.concurrent.ThreadLocalRandom;

public class MatrixVulcanRotation implements AuraRotation {
    private static final float MAX_YAW_SPEED = 45.0F;
    private static final float MAX_PITCH_SPEED = 30.0F;

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || !target.isAlive()) {
            RotationContext.clear();
            return;
        }

        Vec3 delta = targetEyePos.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float idealYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float idealPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(idealYaw - currentYaw);
        float deltaPitch = idealPitch - currentPitch;

        float stepYaw = Mth.clamp(deltaYaw, -MAX_YAW_SPEED, MAX_YAW_SPEED);
        float stepPitch = Mth.clamp(deltaPitch, -MAX_PITCH_SPEED, MAX_PITCH_SPEED);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float jitterYaw = (float) random.nextDouble(-0.35D, 0.35D);
        float jitterPitch = (float) random.nextDouble(-0.25D, 0.25D);

        RotationContext.setRotation(currentYaw + stepYaw + jitterYaw, Mth.clamp(currentPitch + stepPitch + jitterPitch, -90.0F, 90.0F));
    }
}
