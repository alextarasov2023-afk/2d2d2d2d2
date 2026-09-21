package org.alexdlc.feature.impl.misc;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.inventory.ContainerLootService;
import org.alexdlc.utils.inventory.InventoryUtil;

public final class ChestStealerFeature extends Feature {
    public final NumberSetting delay = register(new NumberSetting("Delay", 80.0, 0.0, 1000.0, 10.0, "ms"));

    private long lastMoveAt;

    public ChestStealerFeature() {
        super("ChestStealer", "Automatically takes items from containers", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)
                || !InventoryUtil.isContainerScreenOpen()) {
            return;
        }
        AbstractContainerMenu menu = InventoryUtil.getOpenMenu();
        if (menu == null || menu == event.getClient().player.inventoryMenu) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastMoveAt < delay.getValue().longValue()) {
            return;
        }
        if (ContainerLootService.quickMoveFirst(menu, stack -> !stack.isEmpty())) {
            lastMoveAt = now;
        }
    }
}
