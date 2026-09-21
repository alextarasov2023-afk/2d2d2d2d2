package org.alexdlc.mixin.input;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.events.input.PlayerInputEvent;
import org.alexdlc.feature.impl.movement.InventoryMoveFeature;
import org.alexdlc.feature.impl.movement.SprintFeature;
import org.alexdlc.utils.combat.SprintManager;
import org.alexdlc.utils.inventory.InventorySwap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerInputTick(CallbackInfo ci) {

        Input screenInput = InventoryMoveFeature.screenInput();
        if (screenInput != null) {
            this.keyPresses = screenInput;
            this.moveVector = new Vec2(
                    impulse(screenInput.left(), screenInput.right()),
                    impulse(screenInput.forward(), screenInput.backward())
            );
        }

        if (!this.keyPresses.sprint() && SprintFeature.shouldForceSprintKey()) {
            this.keyPresses = new Input(
                    this.keyPresses.forward(),
                    this.keyPresses.backward(),
                    this.keyPresses.left(),
                    this.keyPresses.right(),
                    this.keyPresses.jump(),
                    this.keyPresses.shift(),
                    true
            );
        }

        if (SprintManager.shouldFreezeMovementInput()) {
            this.moveVector = Vec2.ZERO;
            this.keyPresses = new Input(
                    false, false, false, false,
                    this.keyPresses.jump(),
                    this.keyPresses.shift(),
                    false
            );
        }

        if (InventorySwap.shouldStopMovement() || InventoryMoveFeature.shouldStopMovement()) {
            this.moveVector = Vec2.ZERO;
            this.keyPresses = new Input(false, false, false, false, false, this.keyPresses.shift(), false);
        }

        if (!EventManager.hasListeners(PlayerInputEvent.class)) {
            return;
        }
        PlayerInputEvent event = EventManager.call(new PlayerInputEvent((KeyboardInput) (Object) this, this.keyPresses, this.getMoveVector()));
        this.keyPresses = event.getKeyPresses();
        this.moveVector = event.getMoveVector();
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
