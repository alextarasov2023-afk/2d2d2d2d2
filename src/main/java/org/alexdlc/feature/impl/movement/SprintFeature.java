package org.alexdlc.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.combat.SprintManager;
import org.lwjgl.glfw.GLFW;

public final class SprintFeature extends Feature {
    public final BooleanSetting omniDirectional = register(new BooleanSetting("Omni Directional", false));
    public final BooleanSetting keepSprint = register(new BooleanSetting("Keep Sprint", true));

    public SprintFeature() {
        super("Sprint", "Automatically sprints while moving", FeatureCategory.MOVEMENT, GLFW.GLFW_KEY_V);
    }

    public static SprintFeature getEnabled() {
        return FeatureManager.INSTANCE.getEnabled(SprintFeature.class);
    }

    public static boolean shouldForceSprintKey() {
        return getEnabled() != null && !pveControlsMovement();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (!omniDirectional.getValue() || pveControlsMovement()) {
            return;
        }

        LocalPlayer player = event.getClient().player;
        if (player == null || player.isSpectator() || player.isUsingItem()) {
            return;
        }

        if (SprintManager.shouldFreezeMovementInput()) {
            return;
        }

        if (player.input.getMoveVector().lengthSquared() > 0.0F
                && (keepSprint.getValue() || !player.isSprinting())) {
            player.setSprinting(true);
        }
    }

    private static boolean pveControlsMovement() {
        return PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.MOVEMENT)
                || PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.NAVIGATION);
    }
}
