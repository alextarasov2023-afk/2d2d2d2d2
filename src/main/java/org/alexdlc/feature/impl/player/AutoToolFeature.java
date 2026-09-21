package org.alexdlc.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.utils.inventory.InventoryUtil;

public final class AutoToolFeature extends Feature {
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_SIZE = 36;

    public final BooleanSetting useInventory = register(new BooleanSetting("Use Inventory", true));
    public final ModeSetting mode = register(new ModeSetting(
            "Mode", "Silent", "Silent", "Normal"
    ));

    private int originalHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private int silentServerSlot = -1;

    public AutoToolFeature() {
        super("AutoTool", "Picks the best tool for the targeted block", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        restore();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft client = event.getClient();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || player.isCreative() || client.gui.screen() != null) {
            restore();
            return;
        }
        if (!isBreakingBlock(client)) {
            restore();
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        int bestSlot = findBestToolSlot(player, state);
        if (bestSlot == -1) {
            return;
        }

        if (originalHotbarSlot == -1) {
            originalHotbarSlot = player.getInventory().getSelectedSlot();
        }

        if (bestSlot < HOTBAR_SIZE) {
            selectHotbarTool(player, bestSlot);
            return;
        }
        if (swappedInventorySlot != bestSlot) {
            restoreSilentServerSlot(player);
            restoreSwappedItem();

            InventoryUtil.swapWithHotbar(bestSlot, originalHotbarSlot);
            swappedInventorySlot = bestSlot;
        }
        player.getInventory().setSelectedSlot(originalHotbarSlot);
    }

    public boolean swapForBlock(BlockState state) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!isEnabled() || player == null || state == null) {
            return false;
        }
        int bestSlot = findBestToolSlot(player, state);
        if (bestSlot < 0) {
            return false;
        }
        if (this.originalHotbarSlot == -1) {
            this.originalHotbarSlot = player.getInventory().getSelectedSlot();
        }
        if (bestSlot < HOTBAR_SIZE) {
            selectHotbarTool(player, bestSlot);
        } else if (this.useInventory.getValue()) {
            restoreSilentServerSlot(player);
            restoreSwappedItem();
            InventoryUtil.swapWithHotbar(bestSlot, this.originalHotbarSlot);
            this.swappedInventorySlot = bestSlot;
        }
        return true;
    }

    private boolean isBreakingBlock(Minecraft client) {
        return client.options.keyAttack.isDown()
                && client.hitResult != null
                && client.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private int findBestToolSlot(LocalPlayer player, BlockState state) {
        int limit = useInventory.getValue() ? INVENTORY_SIZE : HOTBAR_SIZE;
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private void selectHotbarTool(LocalPlayer player, int slot) {
        if (this.mode.is("Normal")) {
            restoreSilentServerSlot(player);
            player.getInventory().setSelectedSlot(slot);
            return;
        }
        if (slot == player.getInventory().getSelectedSlot()) {
            restoreSilentServerSlot(player);
            return;
        }
        if (this.silentServerSlot != slot) {
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            this.silentServerSlot = slot;
        }
    }

    private void restore() {
        if (originalHotbarSlot == -1) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        restoreSwappedItem();
        if (player != null) {
            restoreSilentServerSlot(player);
            player.getInventory().setSelectedSlot(originalHotbarSlot);
        }
        originalHotbarSlot = -1;
    }

    private void restoreSwappedItem() {
        if (swappedInventorySlot == -1) {
            return;
        }
        InventoryUtil.swapWithHotbar(swappedInventorySlot, originalHotbarSlot);
        swappedInventorySlot = -1;
    }

    private void restoreSilentServerSlot(LocalPlayer player) {
        if (this.silentServerSlot == -1) {
            return;
        }
        player.connection.send(new ServerboundSetCarriedItemPacket(
                player.getInventory().getSelectedSlot()
        ));
        this.silentServerSlot = -1;
    }
}
