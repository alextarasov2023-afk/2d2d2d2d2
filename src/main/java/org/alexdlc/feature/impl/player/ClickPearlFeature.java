package org.alexdlc.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.event.events.input.KeyboardInputEvent;
import org.alexdlc.event.events.input.MouseInputEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.InputBindSetting;
import org.alexdlc.pve.AutomationOwner;
import org.alexdlc.pve.AutomationPriority;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.inventory.InventorySwap;
import org.alexdlc.utils.inventory.InventoryUtil;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;

public final class ClickPearlFeature extends Feature implements AutomationOwner {
    private static final int HOTBAR_SIZE = 9;

    public final InputBindSetting key = register(new InputBindSetting("Key", GLFW.GLFW_KEY_UNKNOWN));

    private boolean throwQueued;
    private boolean releasePending;

    public ClickPearlFeature() {
        super("ClickPearl", "Quickly throws an ender pearl", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        throwQueued = false;
        releasePending = false;
        PveAutomationCoordinator.INSTANCE.release(this);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && key.matches(event.getKey())) {
            queueThrow();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && key.matchesMouse(event.getButton()) && queueThrow()) {
            event.cancel();
        }
    }

    private boolean queueThrow() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null
                && client.player != null
                && PveAutomationCoordinator.INSTANCE.acquire(
                this,
                AutomationPriority.EMERGENCY,
                EnumSet.of(AutomationResource.INVENTORY, AutomationResource.ROTATION)
        )) {
            throwQueued = true;
            return true;
        }
        return false;
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (this.releasePending && !InventorySwap.isBusy()) {
            this.releasePending = false;
            PveAutomationCoordinator.INSTANCE.release(this);
        }
        if (!throwQueued) {
            return;
        }
        throwQueued = false;
        boolean asyncSwap = false;
        try {
            Minecraft client = event.getClient();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) {
                return;
            }

            if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
                throwFromMainHand(client, player);
                return;
            }

            int hotbarSlot = findPearlHotbarSlot(player);
            if (hotbarSlot != -1) {
                int previousSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(hotbarSlot);
                throwFromMainHand(client, player);
                player.getInventory().setSelectedSlot(previousSlot);
                return;
            }

            int containerSlot = findPearlContainerSlot(player);
            if (containerSlot != -1) {
                InventorySwap.useFromSlot(containerSlot);
                this.releasePending = true;
                asyncSwap = true;
            }
        } finally {
            if (!asyncSwap) {
                PveAutomationCoordinator.INSTANCE.release(this);
            }
        }
    }

    private void throwFromMainHand(Minecraft client, LocalPlayer player) {
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private int findPearlHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) {
                return slot;
            }
        }
        return -1;
    }

    private int findPearlContainerSlot(LocalPlayer player) {
        return InventoryUtil.findInventorySlot(player, stack -> stack.is(Items.ENDER_PEARL));
    }
}
