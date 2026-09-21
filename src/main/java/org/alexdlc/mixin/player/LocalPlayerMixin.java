package org.alexdlc.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.Events;
import org.alexdlc.event.events.game.PlayerTickEvent;
import org.alexdlc.feature.impl.movement.NoPushFeature;
import org.alexdlc.utils.combat.LocalPlayerHistory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickPre(CallbackInfo ci) {
        if (!EventManager.hasListeners(PlayerTickEvent.class)) {
            return;
        }
        EventManager.call(Events.PLAYER_TICK.set((LocalPlayer) (Object) this, PlayerTickEvent.Phase.PRE));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickPost(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        LocalPlayerHistory.record(player);
        if (!EventManager.hasListeners(PlayerTickEvent.class)) {
            return;
        }
        EventManager.call(Events.PLAYER_TICK.set(player, PlayerTickEvent.Phase.POST));
    }

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void cancelClosestSpacePush(double x, double z, CallbackInfo ci) {
        if (NoPushFeature.shouldCancelClosestSpacePush((LocalPlayer) (Object) this)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "applyInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
    )
    private float useCameraPitchForHandBob(LocalPlayer player) {
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            return this.minecraft.gameRenderer.mainCamera().xRot();
        }

        return player.getXRot();
    }

    @Redirect(
            method = "applyInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float useCameraYawForHandBob(LocalPlayer player) {
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            return this.minecraft.gameRenderer.mainCamera().yRot();
        }

        return player.getYRot();
    }

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void onGetViewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (org.alexdlc.context.RotationContext.isActive()) {
            org.alexdlc.feature.impl.combat.AuraFeature aura = org.alexdlc.feature.FeatureManager.INSTANCE.getEnabled(org.alexdlc.feature.impl.combat.AuraFeature.class);
            if (aura != null && aura.clientLook.getValue() && aura.getTarget() != null) {
                net.minecraft.world.entity.Entity target = aura.getTarget();
                LocalPlayer player = (LocalPlayer) (Object) this;

                // Visual target (144Hz interpolated)
                net.minecraft.world.phys.Vec3 visualEye = player.getEyePosition(partialTick);
                net.minecraft.world.phys.Vec3 visualTarget = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.5D, 0);
                float visualYaw = (float) Math.toDegrees(Math.atan2(-(visualTarget.x - visualEye.x), visualTarget.z - visualEye.z));

                if (aura.componentMode.is("Плавный")) {
                    cir.setReturnValue(visualYaw);
                    return;
                }

                // Tick target (current tick)
                net.minecraft.world.phys.Vec3 tickEye = player.getEyePosition(1.0F);
                net.minecraft.world.phys.Vec3 tickTarget = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                float tickYaw = (float) Math.toDegrees(Math.atan2(-(tickTarget.x - tickEye.x), tickTarget.z - tickEye.z));

                // Prev tick target
                net.minecraft.world.phys.Vec3 prevTickEye = new net.minecraft.world.phys.Vec3(player.xo, player.yo + player.getEyeHeight(), player.zo);
                net.minecraft.world.phys.Vec3 prevTickTarget = new net.minecraft.world.phys.Vec3(target.xo, target.yo + target.getBbHeight() * 0.5D, target.zo);
                float prevTickYaw = (float) Math.toDegrees(Math.atan2(-(prevTickTarget.x - prevTickEye.x), prevTickTarget.z - prevTickEye.z));

                // Humanized error
                float errorOld = net.minecraft.util.Mth.wrapDegrees(player.yRotO - prevTickYaw);
                float errorNew = net.minecraft.util.Mth.wrapDegrees(player.getYRot() - tickYaw);
                float interpolatedError = net.minecraft.util.Mth.lerp(partialTick, errorOld, errorNew);

                cir.setReturnValue(visualYaw + interpolatedError);
                return;
            }
            cir.setReturnValue(org.alexdlc.context.RotationContext.getFreeYaw());
        }
    }

    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void onGetViewXRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (org.alexdlc.context.RotationContext.isActive()) {
            org.alexdlc.feature.impl.combat.AuraFeature aura = org.alexdlc.feature.FeatureManager.INSTANCE.getEnabled(org.alexdlc.feature.impl.combat.AuraFeature.class);
            if (aura != null && aura.clientLook.getValue() && aura.getTarget() != null) {
                net.minecraft.world.entity.Entity target = aura.getTarget();
                LocalPlayer player = (LocalPlayer) (Object) this;

                // Visual target (144Hz interpolated)
                net.minecraft.world.phys.Vec3 visualEye = player.getEyePosition(partialTick);
                net.minecraft.world.phys.Vec3 visualTarget = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.5D, 0);
                double visualDist = Math.hypot(visualTarget.x - visualEye.x, visualTarget.z - visualEye.z);
                float visualPitch = (float) net.minecraft.util.Mth.clamp(-Math.toDegrees(Math.atan2(visualTarget.y - visualEye.y, visualDist)), -90.0D, 90.0D);

                if (aura.componentMode.is("Плавный")) {
                    cir.setReturnValue(visualPitch);
                    return;
                }

                // Tick target (current tick)
                net.minecraft.world.phys.Vec3 tickEye = player.getEyePosition(1.0F);
                net.minecraft.world.phys.Vec3 tickTarget = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                double tickDist = Math.hypot(tickTarget.x - tickEye.x, tickTarget.z - tickEye.z);
                float tickPitch = (float) net.minecraft.util.Mth.clamp(-Math.toDegrees(Math.atan2(tickTarget.y - tickEye.y, tickDist)), -90.0D, 90.0D);

                // Prev tick target
                net.minecraft.world.phys.Vec3 prevTickEye = new net.minecraft.world.phys.Vec3(player.xo, player.yo + player.getEyeHeight(), player.zo);
                net.minecraft.world.phys.Vec3 prevTickTarget = new net.minecraft.world.phys.Vec3(target.xo, target.yo + target.getBbHeight() * 0.5D, target.zo);
                double prevTickDist = Math.hypot(prevTickTarget.x - prevTickEye.x, prevTickTarget.z - prevTickEye.z);
                float prevTickPitch = (float) net.minecraft.util.Mth.clamp(-Math.toDegrees(Math.atan2(prevTickTarget.y - prevTickEye.y, prevTickDist)), -90.0D, 90.0D);

                // Humanized error
                float errorOld = player.xRotO - prevTickPitch;
                float errorNew = player.getXRot() - tickPitch;
                float interpolatedError = net.minecraft.util.Mth.lerp(partialTick, errorOld, errorNew);

                cir.setReturnValue(net.minecraft.util.Mth.clamp(visualPitch + interpolatedError, -90.0F, 90.0F));
                return;
            }
            cir.setReturnValue(org.alexdlc.context.RotationContext.getFreePitch());
        }
    }
}
